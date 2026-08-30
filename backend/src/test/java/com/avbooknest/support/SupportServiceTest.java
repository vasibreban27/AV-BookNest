package com.avbooknest.support;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.avbooknest.admin.service.AdminAuditService;
import com.avbooknest.auth.model.*;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.book.repository.BookRepository;
import com.avbooknest.common.exception.*;
import com.avbooknest.contact.dto.ContactRequest;
import com.avbooknest.notification.service.NotificationService;
import com.avbooknest.order.model.Order;
import com.avbooknest.order.repository.OrderRepository;
import com.avbooknest.support.dto.SupportRequests.*;
import com.avbooknest.support.model.*;
import com.avbooknest.support.repository.*;
import com.avbooknest.support.service.*;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SupportServiceTest {
  @Mock SupportTicketRepository tickets;
  @Mock SupportMessageRepository messages;
  @Mock SupportEmailRepository deliveries;
  @Mock UserRepository users;
  @Mock OrderRepository orders;
  @Mock BookRepository books;
  @Mock SupportEmailService emails;
  @Mock NotificationService notifications;
  @Mock AdminAuditService audit;
  @Mock SupportReplyRateLimitService limits;
  SupportService service;
  User buyer = user(1L, "USER", true);
  User admin = user(2L, "ADMIN", true);
  SupportTicket ticket;

  @BeforeEach
  void setup() {
    service =
        new SupportService(
            tickets,
            messages,
            deliveries,
            users,
            orders,
            books,
            emails,
            notifications,
            audit,
            limits);
    ticket =
        SupportTicket.create(
            buyer,
            "Ana Test",
            buyer.getEmail(),
            SupportTopic.GENERAL,
            "Ajutor",
            null,
            null,
            Instant.now());
    ReflectionTestUtils.setField(ticket, "id", 10L);
  }

  @Test
  void guestIsNeverAttachedByUnverifiedEmail() {
    saving();
    var result = service.create(request(null, null), null);
    assertNull(result.ticketId());
    assertTrue(result.reference().startsWith("BN-SUP-"));
    ArgumentCaptor<SupportTicket> capture = ArgumentCaptor.forClass(SupportTicket.class);
    verify(tickets).save(capture.capture());
    assertNull(capture.getValue().getRequester());
    verifyNoInteractions(users);
    verify(emails).enqueue(any());
  }

  @Test
  void signedInIdentityCannotBeSpoofedByForm() {
    active(buyer);
    saving();
    var result = service.create(request(null, null), buyer.getEmail());
    assertEquals(10L, result.ticketId());
    ArgumentCaptor<SupportTicket> capture = ArgumentCaptor.forClass(SupportTicket.class);
    verify(tickets).save(capture.capture());
    assertEquals(buyer.getEmail(), capture.getValue().getContactEmail());
    assertEquals(buyer, capture.getValue().getRequester());
  }

  @Test
  void guestsCannotLinkPrivateResources() {
    assertThrows(BadRequestException.class, () -> service.create(request(1L, null), null));
    assertThrows(BadRequestException.class, () -> service.create(request(null, 1L), null));
    verifyNoInteractions(orders, books, tickets);
  }

  @Test
  void unrelatedOrderIsNotExposed() {
    active(buyer);
    when(orders.findById(40L))
        .thenReturn(Optional.of(Order.builder().id(40L).buyer(admin).build()));
    assertThrows(
        NotFoundException.class, () -> service.create(request(40L, null), buyer.getEmail()));
    verifyNoInteractions(tickets, messages);
  }

  @Test
  void ownOrderCanBeLinked() {
    active(buyer);
    saving();
    when(orders.findById(40L))
        .thenReturn(Optional.of(Order.builder().id(40L).buyer(buyer).build()));
    service.create(request(40L, null), buyer.getEmail());
    verify(tickets).save(argThat(t -> t.getRelatedOrder().getId().equals(40L)));
  }

  @Test
  void otherUsersCannotReadTicketOrHistory() {
    active(admin);
    assertThrows(NotFoundException.class, () -> service.getMine(10L, admin.getEmail()));
    assertThrows(NotFoundException.class, () -> service.myMessages(10L, admin.getEmail(), 0, 30));
    verifyNoInteractions(messages);
  }

  @Test
  void otherUsersCannotReplyEvenIfAdministratorOnUserEndpoint() {
    active(admin);
    locked();
    assertThrows(
        NotFoundException.class, () -> service.reply(10L, admin.getEmail(), new Reply("test")));
    verifyNoInteractions(messages, emails);
  }

  @Test
  void resolvedTicketReopensOnRequesterReply() {
    active(buyer);
    locked();
    messageSaving();
    ticket.changeStatus(SupportStatus.RESOLVED, Instant.now());
    service.reply(10L, buyer.getEmail(), new Reply("Încă am nevoie de ajutor"));
    assertEquals(SupportStatus.IN_PROGRESS, ticket.getStatus());
    assertNull(ticket.getResolvedAt());
    verify(limits).check(1L);
    verify(emails).enqueue(any());
  }

  @Test
  void closedTicketsRejectBothKindsOfReplies() {
    active(buyer);
    active(admin);
    locked();
    ticket.changeStatus(SupportStatus.CLOSED, Instant.now());
    assertThrows(
        ConflictException.class, () -> service.reply(10L, buyer.getEmail(), new Reply("test")));
    assertThrows(
        ConflictException.class,
        () -> service.adminReply(10L, admin.getEmail(), new Reply("test")));
    verifyNoInteractions(messages, emails);
  }

  @Test
  void blankAndOversizedRepliesAreRejected() {
    active(buyer);
    locked();
    for (String body : new String[] {"  ", "x".repeat(4001)})
      assertThrows(
          BadRequestException.class, () -> service.reply(10L, buyer.getEmail(), new Reply(body)));
    verifyNoInteractions(messages);
  }

  @Test
  void ordinaryAndSuspendedUsersCannotAdminister() {
    active(buyer);
    assertThrows(ForbiddenException.class, () -> service.adminGet(10L, buyer.getEmail()));
    User disabled = user(3L, "ADMIN", false);
    active(disabled);
    assertThrows(ForbiddenException.class, () -> service.adminGet(10L, disabled.getEmail()));
    verifyNoInteractions(tickets);
  }

  @Test
  void administratorReplyIsAuditedAndNotifiesOwner() {
    active(admin);
    locked();
    messageSaving();
    service.adminReply(10L, admin.getEmail(), new Reply("  Răspunsul echipei  "));
    assertEquals(SupportStatus.IN_PROGRESS, ticket.getStatus());
    verify(audit)
        .record(
            eq(admin),
            eq("SUPPORT_REPLY"),
            eq("SUPPORT_TICKET"),
            eq(10L),
            anyString(),
            eq("messageId=20"));
    verify(notifications).create(eq(buyer), any(), anyString(), anyString(), eq("/support/10"));
    verify(messages).save(argThat(m -> m.getBody().equals("Răspunsul echipei")));
  }

  @Test
  void statusReasonIsAuditOnlyNotPublicMessage() {
    active(admin);
    locked();
    messageSaving();
    service.changeStatus(
        10L,
        admin.getEmail(),
        new StatusChange(SupportStatus.RESOLVED, "Internal investigation detail"));
    assertNotNull(ticket.getResolvedAt());
    verify(messages)
        .save(
            argThat(
                m ->
                    !m.getBody().contains("Internal") && m.getKind() == SupportMessageKind.SYSTEM));
    verify(audit)
        .record(
            eq(admin),
            eq("SUPPORT_STATUS_CHANGED"),
            anyString(),
            eq(10L),
            eq("Internal investigation detail"),
            anyString());
  }

  @Test
  void closedTicketMustBeExplicitlyReopened() {
    active(admin);
    locked();
    ticket.changeStatus(SupportStatus.CLOSED, Instant.now());
    assertThrows(
        ConflictException.class,
        () ->
            service.changeStatus(
                10L, admin.getEmail(), new StatusChange(SupportStatus.RESOLVED, "reason")));
    messageSaving();
    service.changeStatus(
        10L, admin.getEmail(), new StatusChange(SupportStatus.IN_PROGRESS, "Reopen"));
    assertNull(ticket.getClosedAt());
  }

  @Test
  void assignmentRejectsNonAdmin() {
    active(admin);
    locked();
    when(users.findById(1L)).thenReturn(Optional.of(buyer));
    assertThrows(
        BadRequestException.class,
        () -> service.assign(10L, admin.getEmail(), new Assignment(1L, "Assign")));
    verifyNoInteractions(audit, notifications);
  }

  @Test
  void assignmentAndUnassignmentAreAudited() {
    active(admin);
    locked();
    when(users.findById(2L)).thenReturn(Optional.of(admin));
    service.assign(10L, admin.getEmail(), new Assignment(2L, "Preluare"));
    assertEquals(admin, ticket.getAssignedTo());
    service.assign(10L, admin.getEmail(), new Assignment(null, "Redistribuire"));
    assertNull(ticket.getAssignedTo());
    verify(audit, times(2))
        .record(eq(admin), eq("SUPPORT_ASSIGNED"), anyString(), eq(10L), anyString(), anyString());
  }

  @Test
  void retryCannotTargetMessageOfAnotherTicket() {
    active(admin);
    when(messages.findById(20L))
        .thenReturn(
            Optional.of(
                SupportMessage.create(
                    ticket, admin, SupportMessageKind.ADMIN, "Body", Instant.now())));
    assertThrows(NotFoundException.class, () -> service.retryEmail(999L, 20L, admin.getEmail()));
    verifyNoInteractions(emails);
  }

  @Test
  void paginationAndSearchAreBounded() {
    active(buyer);
    active(admin);
    assertThrows(BadRequestException.class, () -> service.mine(buyer.getEmail(), null, -1, 20));
    assertThrows(BadRequestException.class, () -> service.mine(buyer.getEmail(), null, 0, 101));
    assertThrows(
        BadRequestException.class,
        () -> service.adminList(admin.getEmail(), null, null, false, "x".repeat(201), 0, 20));
  }

  private void active(User user) {
    when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
  }

  private void locked() {
    when(tickets.findByIdForUpdate(10L)).thenReturn(Optional.of(ticket));
  }

  private void messageSaving() {
    when(messages.save(any()))
        .thenAnswer(
            call -> {
              SupportMessage m = call.getArgument(0);
              ReflectionTestUtils.setField(m, "id", 20L);
              return m;
            });
  }

  private void saving() {
    when(tickets.save(any()))
        .thenAnswer(
            call -> {
              SupportTicket t = call.getArgument(0);
              ReflectionTestUtils.setField(t, "id", 10L);
              return t;
            });
    messageSaving();
  }

  private ContactRequest request(Long orderId, Long bookId) {
    return new ContactRequest(
        "Visitor",
        "visitor@test.ro",
        "GENERAL",
        "Solicitare ajutor",
        "Am nevoie de ajutor cu această situație.",
        true,
        "",
        orderId,
        bookId);
  }

  private static User user(Long id, String role, boolean enabled) {
    return User.builder()
        .id(id)
        .email("user" + id + "@test.ro")
        .firstName("Ana")
        .lastName("Test")
        .role(Role.builder().name(role).build())
        .enabled(enabled)
        .build();
  }
}
