package com.avbooknest.contact.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avbooknest.contact.dto.ContactRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class ContactEmailServiceTest {
  @Mock private ObjectProvider<JavaMailSender> mailSenderProvider;
  @Mock private JavaMailSender mailSender;

  @Test
  void sendsMessageToConfiguredRecipientAndSetsVisitorAsReplyTo() {
    when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
    ContactEmailService service =
        new ContactEmailService(
            mailSenderProvider, true, "av.booknest@gmail.com", "av.booknest@gmail.com");
    ContactRequest request =
        new ContactRequest(
            "Ana Pop",
            "ana@example.com",
            "ORDER",
            "Întrebare despre comandă",
            "Aș dori mai multe informații despre comanda mea.",
            true,
            "");

    service.send(request);

    ArgumentCaptor<SimpleMailMessage> messageCaptor =
        ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(messageCaptor.capture());
    SimpleMailMessage message = messageCaptor.getValue();
    assertEquals("av.booknest@gmail.com", message.getTo()[0]);
    assertEquals("ana@example.com", message.getReplyTo());
    assertTrue(message.getSubject().contains("Comandă"));
    assertTrue(message.getText().contains("Ana Pop"));
  }
}
