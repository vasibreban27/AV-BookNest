package com.avbooknest.support;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.avbooknest.common.exception.ConflictException;
import com.avbooknest.support.model.*;
import com.avbooknest.support.repository.SupportEmailRepository;
import com.avbooknest.support.service.SupportEmailService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;

class SupportEmailServiceTest {
  SupportEmailRepository repository = mock(SupportEmailRepository.class);

  @SuppressWarnings("unchecked")
  ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);

  JavaMailSender sender = mock(JavaMailSender.class);
  SupportTicket ticket =
      SupportTicket.create(
          null,
          "Visitor",
          "visitor@test.ro",
          SupportTopic.GENERAL,
          "Help\r\nSubject",
          null,
          null,
          Instant.now());
  SupportMessage message =
      SupportMessage.create(ticket, null, SupportMessageKind.ADMIN, "Reply", Instant.now());
  SupportEmailDelivery delivery =
      SupportEmailDelivery.create(message, "visitor@test.ro", true, Instant.now().minusSeconds(1));
  SupportEmailService service =
      new SupportEmailService(
          repository, provider, true, "from@test.ro", "team@test.ro", "https://booknest.test/");

  @Test
  void smtpFailureIsRecordedWithoutThrowingOrLeakingSecrets() {
    when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(delivery));
    when(provider.getIfAvailable()).thenReturn(sender);
    doThrow(new MailSendException("password=secret body=private"))
        .when(sender)
        .send(any(SimpleMailMessage.class));
    assertDoesNotThrow(() -> service.deliver(1L));
    assertEquals(SupportEmailStatus.FAILED, delivery.getStatus());
    assertEquals(1, delivery.getAttempts());
    assertFalse(delivery.getLastError().contains("secret"));
    assertFalse(delivery.isDue(Instant.now()));
  }

  @Test
  void successfulGuestEmailHasSafeSubjectAndNoPrivatePortalLink() {
    when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(delivery));
    when(provider.getIfAvailable()).thenReturn(sender);
    service.deliver(1L);
    assertEquals(SupportEmailStatus.SENT, delivery.getStatus());
    assertNotNull(delivery.getSentAt());
    verify(sender)
        .send(
            argThat(
                (SimpleMailMessage mail) ->
                    !mail.getSubject().contains("\n")
                        && !mail.getText().contains("/support/")
                        && mail.getReplyTo().equals("team@test.ro")));
    service.deliver(1L);
    verify(sender, times(1)).send(any(SimpleMailMessage.class));
  }

  @Test
  void retriesStopAfterFiveAttemptsAndCanBeManuallyRequeued() {
    Instant now = Instant.now();
    for (int i = 0; i < 5; i++) delivery.failed(now);
    assertFalse(delivery.isDue(now.plusSeconds(100000)));
    when(repository.findByMessageIdForUpdate(2L)).thenReturn(Optional.of(delivery));
    service.retry(2L);
    assertEquals(0, delivery.getAttempts());
    assertEquals(SupportEmailStatus.PENDING, delivery.getStatus());
    assertTrue(delivery.isDue(Instant.now().plusSeconds(1)));
  }

  @Test
  void disabledMailNeverPretendsToSendAndCannotRetry() {
    var disabled =
        new SupportEmailService(
            repository, provider, false, "from@test.ro", "team@test.ro", "http://localhost:5173");
    disabled.enqueue(message);
    verify(repository).save(argThat(e -> e.getStatus() == SupportEmailStatus.DISABLED));
    disabled.deliver(1L);
    verifyNoInteractions(provider);
    assertThrows(ConflictException.class, () -> disabled.retry(2L));
  }

  @Test
  void sentAndPendingEmailsCannotBeManuallyDuplicated() {
    when(repository.findByMessageIdForUpdate(2L)).thenReturn(Optional.of(delivery));
    assertThrows(ConflictException.class, () -> service.retry(2L));
    delivery.sent(Instant.now());
    assertThrows(ConflictException.class, () -> service.retry(2L));
  }
}
