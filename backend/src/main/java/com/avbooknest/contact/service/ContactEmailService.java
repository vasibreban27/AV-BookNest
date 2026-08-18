package com.avbooknest.contact.service;

import com.avbooknest.common.exception.ExternalServiceException;
import com.avbooknest.contact.dto.ContactRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ContactEmailService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ContactEmailService.class);
  private static final Map<String, String> TOPICS =
      Map.of(
          "GENERAL", "Întrebare generală",
          "ORDER", "Comandă",
          "PAYMENT", "Plată",
          "DELIVERY", "Livrare",
          "ACCOUNT", "Cont",
          "LISTING", "Anunț",
          "PRIVACY", "Confidențialitate",
          "OTHER", "Alt subiect");

  private final ObjectProvider<JavaMailSender> mailSenderProvider;
  private final boolean enabled;
  private final String from;
  private final String recipient;

  public ContactEmailService(
      ObjectProvider<JavaMailSender> mailSenderProvider,
      @Value("${app.mail.enabled:false}") boolean enabled,
      @Value("${app.mail.from:no-reply@booknest.local}") String from,
      @Value("${app.contact.recipient:av.booknest@gmail.com}") String recipient) {
    this.mailSenderProvider = mailSenderProvider;
    this.enabled = enabled;
    this.from = from;
    this.recipient = recipient;
  }

  public void send(ContactRequest request) {
    if (!enabled) {
      LOGGER.warn(
          "DEV contact message from {} with subject: {}",
          request.email(),
          sanitizeHeader(request.subject()));
      return;
    }

    JavaMailSender sender = mailSenderProvider.getIfAvailable();
    if (sender == null) {
      throw new ExternalServiceException("Serviciul de contact nu este configurat momentan");
    }

    SimpleMailMessage email = new SimpleMailMessage();
    email.setFrom(from);
    email.setTo(recipient);
    email.setReplyTo(request.email().trim());
    email.setSubject(
        "[BookNest · "
            + TOPICS.getOrDefault(request.topic(), "Contact")
            + "] "
            + sanitizeHeader(request.subject()));
    email.setText(
        "Mesaj primit prin formularul BookNest\n\n"
            + "Nume: "
            + sanitizeHeader(request.name())
            + "\nEmail: "
            + request.email().trim()
            + "\nCategorie: "
            + TOPICS.getOrDefault(request.topic(), request.topic())
            + "\n\nMesaj:\n"
            + request.message().trim());

    try {
      sender.send(email);
    } catch (MailException exception) {
      throw new ExternalServiceException(
          "Mesajul nu a putut fi trimis momentan. Încearcă din nou mai târziu", exception);
    }
  }

  private String sanitizeHeader(String value) {
    return value.trim().replaceAll("[\\r\\n]+", " ");
  }
}
