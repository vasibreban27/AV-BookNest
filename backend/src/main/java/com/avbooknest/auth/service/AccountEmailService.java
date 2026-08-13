package com.avbooknest.auth.service;

import com.avbooknest.auth.model.User;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class AccountEmailService {
  private static final Logger LOGGER = LoggerFactory.getLogger(AccountEmailService.class);
  private final ObjectProvider<JavaMailSender> mailSenderProvider;
  private final boolean enabled;
  private final String from;
  private final String frontendUrl;

  public AccountEmailService(
      ObjectProvider<JavaMailSender> mailSenderProvider,
      @Value("${app.mail.enabled:false}") boolean enabled,
      @Value("${app.mail.from:no-reply@booknest.local}") String from,
      @Value("${app.auth.frontend-url:http://localhost:5173}") String frontendUrl) {
    this.mailSenderProvider = mailSenderProvider;
    this.enabled = enabled;
    this.from = from;
    this.frontendUrl = frontendUrl.replaceAll("/+$", "");
  }

  public void sendVerification(User user, String rawToken) {
    String link = link("/verify-email?token=", rawToken);
    send(
        user,
        "Confirmă adresa de email BookNest",
        "Bună, "
            + user.getFirstName()
            + "!\n\nConfirmă adresa de email accesând linkul:\n"
            + link
            + "\n\nLinkul expiră în 24 de ore.",
        link,
        "verification");
  }

  public void sendPasswordReset(User user, String rawToken) {
    String link = link("/reset-password?token=", rawToken);
    send(
        user,
        "Resetează parola BookNest",
        "Bună, "
            + user.getFirstName()
            + "!\n\nPoți seta o parolă nouă accesând linkul:\n"
            + link
            + "\n\nLinkul expiră în 30 de minute. Dacă nu ai cerut resetarea, ignoră mesajul.",
        link,
        "password reset");
  }

  private void send(User user, String subject, String text, String link, String purpose) {
    if (!enabled) {
      LOGGER.warn("DEV {} link for {}: {}", purpose, user.getEmail(), link);
      return;
    }
    JavaMailSender sender = mailSenderProvider.getIfAvailable();
    if (sender == null) {
      throw new IllegalStateException("Email delivery is enabled but no mail sender is configured");
    }
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(user.getEmail());
    message.setSubject(subject);
    message.setText(text);
    sender.send(message);
  }

  private String link(String path, String token) {
    return frontendUrl + path + URLEncoder.encode(token, StandardCharsets.UTF_8);
  }
}
