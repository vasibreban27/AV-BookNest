package com.avbooknest.support.service;

import com.avbooknest.common.exception.ConflictException;
import com.avbooknest.common.exception.NotFoundException;
import com.avbooknest.support.model.*;
import com.avbooknest.support.repository.SupportEmailRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SupportEmailService {
  private static final Logger LOG = LoggerFactory.getLogger(SupportEmailService.class);
  private final SupportEmailRepository emails;
  private final ObjectProvider<JavaMailSender> senders;
  private final boolean enabled;
  private final String from;
  private final String teamEmail;
  private final String frontendUrl;

  public SupportEmailService(
      SupportEmailRepository emails,
      ObjectProvider<JavaMailSender> senders,
      @Value("${app.mail.enabled:false}") boolean enabled,
      @Value("${app.mail.from:no-reply@booknest.local}") String from,
      @Value("${app.contact.recipient:av.booknest@gmail.com}") String teamEmail,
      @Value("${app.auth.frontend-url:http://localhost:5173}") String frontendUrl) {
    this.emails = emails;
    this.senders = senders;
    this.enabled = enabled;
    this.from = from;
    this.teamEmail = teamEmail;
    this.frontendUrl = frontendUrl.replaceAll("/+$", "");
  }

  public void enqueue(SupportMessage message) {
    String recipient =
        message.getKind() == SupportMessageKind.REQUESTER
            ? teamEmail
            : message.getTicket().getContactEmail();
    emails.save(SupportEmailDelivery.create(message, recipient, enabled, Instant.now()));
  }

  public void retry(Long messageId) {
    if (!enabled)
      throw new ConflictException(
          "Email delivery is disabled. Configure MAIL_ENABLED and SMTP first");
    SupportEmailDelivery email =
        emails
            .findByMessageIdForUpdate(messageId)
            .orElseThrow(() -> new NotFoundException("Email delivery not found"));
    if (email.getStatus() != SupportEmailStatus.FAILED
        && email.getStatus() != SupportEmailStatus.DISABLED) {
      throw new ConflictException("Only failed or disabled emails can be retried");
    }
    email.retry(Instant.now());
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deliver(Long id) {
    if (!enabled) return;
    SupportEmailDelivery delivery = emails.findByIdForUpdate(id).orElse(null);
    if (delivery == null || !delivery.isDue(Instant.now())) return;
    JavaMailSender sender = senders.getIfAvailable();
    if (sender == null) {
      delivery.failed(Instant.now());
      return;
    }
    SupportMessage message = delivery.getMessage();
    SupportTicket ticket = message.getTicket();
    boolean toTeam = message.getKind() == SupportMessageKind.REQUESTER;
    SimpleMailMessage mail = new SimpleMailMessage();
    mail.setFrom(from);
    mail.setTo(delivery.getRecipient());
    mail.setReplyTo(toTeam ? ticket.getContactEmail() : teamEmail);
    mail.setSubject(
        "[BookNest "
            + ticket.getReference()
            + "] "
            + ticket.getSubject().replaceAll("[\\r\\n]+", " "));
    String link =
        toTeam
            ? frontendUrl + "/admin/support/" + ticket.getId()
            : ticket.getRequester() == null ? null : frontendUrl + "/support/" + ticket.getId();
    mail.setText(
        "Solicitare "
            + ticket.getReference()
            + "\n\n"
            + message.getBody()
            + (link == null
                ? "\n\nPoți răspunde pe email. Echipa preia manual răspunsurile din căsuța de suport."
                : "\n\nConversația și istoricul: " + link)
            + (toTeam
                ? "\n\nContact: " + ticket.getContactName() + " <" + ticket.getContactEmail() + ">"
                : ""));
    try {
      sender.send(mail);
      delivery.sent(Instant.now());
    } catch (MailException exception) {
      delivery.failed(Instant.now());
      LOG.warn("Support email {} failed on attempt {}", id, delivery.getAttempts());
    }
  }
}
