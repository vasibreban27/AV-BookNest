package com.avbooknest.support.service;

import com.avbooknest.admin.service.AdminAuditService;
import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.book.model.*;
import com.avbooknest.book.repository.BookRepository;
import com.avbooknest.common.dto.PageResponse;
import com.avbooknest.common.exception.*;
import com.avbooknest.contact.dto.*;
import com.avbooknest.notification.model.NotificationType;
import com.avbooknest.notification.service.NotificationService;
import com.avbooknest.order.model.Order;
import com.avbooknest.order.repository.OrderRepository;
import com.avbooknest.support.dto.SupportDtos.*;
import com.avbooknest.support.dto.SupportRequests.*;
import com.avbooknest.support.model.*;
import com.avbooknest.support.repository.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SupportService {
  private final SupportTicketRepository tickets;
  private final SupportMessageRepository messages;
  private final SupportEmailRepository deliveries;
  private final UserRepository users;
  private final OrderRepository orders;
  private final BookRepository books;
  private final SupportEmailService emails;
  private final NotificationService notifications;
  private final AdminAuditService audit;
  private final SupportReplyRateLimitService rateLimit;

  public SupportService(
      SupportTicketRepository tickets,
      SupportMessageRepository messages,
      SupportEmailRepository deliveries,
      UserRepository users,
      OrderRepository orders,
      BookRepository books,
      SupportEmailService emails,
      NotificationService notifications,
      AdminAuditService audit,
      SupportReplyRateLimitService rateLimit) {
    this.tickets = tickets;
    this.messages = messages;
    this.deliveries = deliveries;
    this.users = users;
    this.orders = orders;
    this.books = books;
    this.emails = emails;
    this.notifications = notifications;
    this.audit = audit;
    this.rateLimit = rateLimit;
  }

  public ContactResponse create(ContactRequest request, String email) {
    User requester = email == null ? null : activeUser(email);
    if (!Boolean.TRUE.equals(request.privacyAccepted()))
      throw new BadRequestException("Privacy consent is required");
    validateBody(request.message(), 20);
    if (request.subject() == null
        || request.subject().isBlank()
        || request.subject().length() > 150)
      throw new BadRequestException("A subject of up to 150 characters is required");
    SupportTopic topic;
    try {
      topic = SupportTopic.valueOf(request.topic());
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new BadRequestException("Invalid support topic");
    }
    if (requester == null && (request.orderId() != null || request.bookId() != null))
      throw new BadRequestException("Sign in to link an order or listing");
    Order order = linkedOrder(request.orderId(), requester);
    Book book = linkedBook(request.bookId(), requester, order);
    String name =
        requester == null
            ? request.name()
            : requester.getFirstName() + " " + requester.getLastName();
    String contactEmail = requester == null ? request.email() : requester.getEmail();
    SupportTicket ticket =
        tickets.save(
            SupportTicket.create(
                requester,
                name,
                contactEmail,
                topic,
                request.subject(),
                order,
                book,
                Instant.now()));
    append(ticket, requester, SupportMessageKind.REQUESTER, request.message());
    return new ContactResponse(
        "Solicitarea a fost înregistrată. Echipa de suport îți va răspunde în cont sau pe email.",
        requester == null ? null : ticket.getId(),
        ticket.getReference());
  }

  @Transactional(readOnly = true)
  public PageResponse<Ticket> mine(String email, SupportStatus status, int page, int size) {
    User user = activeUser(email);
    return PageResponse.from(
        tickets.forRequester(user.getId(), status, pageable(page, size, "updatedAt")),
        Ticket::from);
  }

  @Transactional(readOnly = true)
  public Ticket getMine(Long id, String email) {
    return Ticket.from(owned(id, activeUser(email)));
  }

  @Transactional(readOnly = true)
  public PageResponse<Message> myMessages(Long id, String email, int page, int size) {
    owned(id, activeUser(email));
    return PageResponse.from(
        messages.findAllByTicketId(id, pageable(page, size, "id")), Message::from);
  }

  public Message reply(Long id, String email, Reply request) {
    User user = activeUser(email);
    SupportTicket ticket = locked(id);
    if (ticket.getRequester() == null || !ticket.getRequester().getId().equals(user.getId()))
      throw missing();
    validateBody(request.body(), 1);
    ensureOpen(ticket);
    rateLimit.check(user.getId());
    if (ticket.getStatus() == SupportStatus.RESOLVED)
      ticket.changeStatus(SupportStatus.IN_PROGRESS, Instant.now());
    SupportMessage message = append(ticket, user, SupportMessageKind.REQUESTER, request.body());
    if (ticket.getAssignedTo() != null)
      notifications.create(
          ticket.getAssignedTo(),
          NotificationType.SUPPORT_REPLY,
          "Răspuns nou la suport",
          ticket.getReference() + ": " + ticket.getSubject(),
          "/admin/support/" + id);
    return Message.from(message);
  }

  @Transactional(readOnly = true)
  public PageResponse<AdminTicket> adminList(
      String email,
      SupportStatus status,
      Long assignedToId,
      boolean unassigned,
      String query,
      int page,
      int size) {
    administrator(email);
    String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    if (normalized.length() > 200)
      throw new BadRequestException("Search must not exceed 200 characters");
    if (assignedToId != null && (assignedToId < 1 || unassigned))
      throw new BadRequestException("Invalid assignment filter");
    return PageResponse.from(
        tickets.forAdmin(
            status, assignedToId, unassigned, normalized, pageable(page, size, "updatedAt")),
        AdminTicket::from);
  }

  @Transactional(readOnly = true)
  public AdminTicket adminGet(Long id, String email) {
    administrator(email);
    return AdminTicket.from(tickets.findById(id).orElseThrow(this::missing));
  }

  @Transactional(readOnly = true)
  public List<Administrator> administrators(String email) {
    administrator(email);
    return users.findSupportAdministrators().stream().map(Administrator::from).toList();
  }

  @Transactional(readOnly = true)
  public PageResponse<AdminMessage> adminMessages(Long id, String email, int page, int size) {
    administrator(email);
    if (!tickets.existsById(id)) throw missing();
    var result = messages.findAllByTicketId(id, pageable(page, size, "id"));
    var ids = result.stream().map(SupportMessage::getId).toList();
    Map<Long, SupportEmailDelivery> deliveryMap =
        ids.isEmpty()
            ? Map.of()
            : deliveries.findAllByMessageIdIn(ids).stream()
                .collect(Collectors.toMap(e -> e.getMessage().getId(), Function.identity()));
    return PageResponse.from(
        result, message -> AdminMessage.from(message, deliveryMap.get(message.getId())));
  }

  public Message adminReply(Long id, String email, Reply request) {
    User admin = administrator(email);
    SupportTicket ticket = locked(id);
    validateBody(request.body(), 1);
    ensureOpen(ticket);
    ticket.changeStatus(SupportStatus.IN_PROGRESS, Instant.now());
    SupportMessage message = append(ticket, admin, SupportMessageKind.ADMIN, request.body());
    audit.record(
        admin,
        "SUPPORT_REPLY",
        "SUPPORT_TICKET",
        id,
        "Administrator reply",
        "messageId=" + message.getId());
    notifyRequester(ticket, NotificationType.SUPPORT_REPLY, "Ai un răspuns de la suport");
    return Message.from(message);
  }

  public AdminTicket changeStatus(Long id, String email, StatusChange request) {
    User admin = administrator(email);
    validateReason(request.reason());
    if (request.status() == null) throw new BadRequestException("Status is required");
    SupportTicket ticket = locked(id);
    if (ticket.getStatus() == request.status()) return AdminTicket.from(ticket);
    if (request.status() == SupportStatus.NEW
        || (ticket.getStatus() == SupportStatus.CLOSED
            && request.status() != SupportStatus.IN_PROGRESS))
      throw new ConflictException(
          "Closed tickets must be reopened to IN_PROGRESS; NEW is only the initial status");
    SupportStatus previous = ticket.getStatus();
    ticket.changeStatus(request.status(), Instant.now());
    String label =
        switch (request.status()) {
          case NEW -> "nouă";
          case IN_PROGRESS -> "în lucru";
          case RESOLVED -> "rezolvată";
          case CLOSED -> "închisă";
        };
    append(ticket, admin, SupportMessageKind.SYSTEM, "Solicitarea este acum " + label + ".");
    audit.record(
        admin,
        "SUPPORT_STATUS_CHANGED",
        "SUPPORT_TICKET",
        id,
        request.reason(),
        previous + " -> " + request.status());
    notifyRequester(
        ticket, NotificationType.SUPPORT_STATUS_CHANGED, "Starea solicitării a fost actualizată");
    return AdminTicket.from(ticket);
  }

  public AdminTicket assign(Long id, String email, Assignment request) {
    User admin = administrator(email);
    validateReason(request.reason());
    SupportTicket ticket = locked(id);
    User assignee =
        request.administratorId() == null
            ? null
            : users
                .findById(request.administratorId())
                .filter(this::isAdministrator)
                .orElseThrow(() -> new BadRequestException("Choose an active administrator"));
    Long current = ticket.getAssignedTo() == null ? null : ticket.getAssignedTo().getId();
    if (Objects.equals(current, request.administratorId())) return AdminTicket.from(ticket);
    ticket.assign(assignee, Instant.now());
    audit.record(
        admin,
        "SUPPORT_ASSIGNED",
        "SUPPORT_TICKET",
        id,
        request.reason(),
        "assignedTo=" + request.administratorId());
    if (assignee != null)
      notifications.create(
          assignee,
          NotificationType.SUPPORT_ASSIGNED,
          "Solicitare de suport repartizată",
          ticket.getReference() + ": " + ticket.getSubject(),
          "/admin/support/" + id);
    return AdminTicket.from(ticket);
  }

  public void retryEmail(Long id, Long messageId, String email) {
    User admin = administrator(email);
    SupportMessage message =
        messages
            .findById(messageId)
            .filter(m -> m.getTicket().getId().equals(id))
            .orElseThrow(this::missing);
    emails.retry(message.getId());
    audit.record(
        admin,
        "SUPPORT_EMAIL_RETRY",
        "SUPPORT_TICKET",
        id,
        "Manual email retry",
        "messageId=" + messageId);
  }

  private SupportMessage append(
      SupportTicket ticket, User author, SupportMessageKind kind, String body) {
    Instant now = Instant.now();
    ticket.touch(now);
    SupportMessage message = messages.save(SupportMessage.create(ticket, author, kind, body, now));
    emails.enqueue(message);
    return message;
  }

  private Order linkedOrder(Long id, User requester) {
    if (id == null) return null;
    if (id < 1) throw new BadRequestException("Invalid order identifier");
    Order order = orders.findById(id).orElseThrow(() -> new NotFoundException("Order not found"));
    if (!order.getBuyer().getId().equals(requester.getId())
        && order.getSellerOrders().stream()
            .noneMatch(s -> s.getSeller().getId().equals(requester.getId())))
      throw new NotFoundException("Order not found");
    return order;
  }

  private Book linkedBook(Long id, User requester, Order order) {
    if (id == null) return null;
    if (id < 1) throw new BadRequestException("Invalid listing identifier");
    Book book = books.findById(id).orElseThrow(() -> new NotFoundException("Book not found"));
    if (order != null) {
      boolean related =
          order.getItems().stream()
              .anyMatch(
                  item ->
                      item.getBook() != null
                          && item.getBook().getId().equals(id)
                          && (order.getBuyer().getId().equals(requester.getId())
                              || item.getSeller().getId().equals(requester.getId())));
      if (!related) throw new NotFoundException("Book not found in this order");
      return book;
    }
    if (!book.getSeller().getId().equals(requester.getId())
        && !(book.getStatus() == BookStatus.AVAILABLE
            && book.getModerationStatus() == BookModerationStatus.VISIBLE
            && book.getCategory().isActive()
            && book.getSeller().isEnabled())) throw new NotFoundException("Book not found");
    return book;
  }

  private void notifyRequester(SupportTicket ticket, NotificationType type, String title) {
    if (ticket.getRequester() != null)
      notifications.create(
          ticket.getRequester(),
          type,
          title,
          ticket.getReference() + ": " + ticket.getSubject(),
          "/support/" + ticket.getId());
  }

  private void validateBody(String body, int min) {
    if (body == null || body.trim().length() < min || body.length() > 4000)
      throw new BadRequestException("Message must contain " + min + " to 4000 characters");
  }

  private void validateReason(String reason) {
    if (reason == null || reason.isBlank() || reason.length() > 500)
      throw new BadRequestException("A reason of up to 500 characters is required");
  }

  private void ensureOpen(SupportTicket ticket) {
    if (ticket.getStatus() == SupportStatus.CLOSED)
      throw new ConflictException(
          "This ticket is closed. An administrator must reopen it before replying");
  }

  private SupportTicket owned(Long id, User user) {
    return tickets.findByIdAndRequesterId(id, user.getId()).orElseThrow(this::missing);
  }

  private SupportTicket locked(Long id) {
    return tickets.findByIdForUpdate(id).orElseThrow(this::missing);
  }

  private NotFoundException missing() {
    return new NotFoundException("Support ticket not found");
  }

  private User activeUser(String email) {
    User user =
        users
            .findByEmail(email)
            .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    if (!user.isEnabled()) throw new ForbiddenException("Account is suspended");
    return user;
  }

  private boolean isAdministrator(User user) {
    return user.isEnabled() && user.getRole() != null && "ADMIN".equals(user.getRole().getName());
  }

  private User administrator(String email) {
    User user = activeUser(email);
    if (!isAdministrator(user)) throw new ForbiddenException("Administrator access required");
    return user;
  }

  private PageRequest pageable(int page, int size, String sort) {
    if (page < 0 || size < 1 || size > 100)
      throw new BadRequestException("Page must be non-negative and size between 1 and 100");
    return PageRequest.of(
        page,
        size,
        sort.equals("id")
            ? Sort.by(Sort.Direction.DESC, "id")
            : Sort.by(Sort.Direction.DESC, sort, "id"));
  }
}
