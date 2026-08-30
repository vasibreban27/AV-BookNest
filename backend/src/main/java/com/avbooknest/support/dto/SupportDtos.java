package com.avbooknest.support.dto;

import com.avbooknest.auth.model.User;
import com.avbooknest.support.model.*;
import java.time.Instant;

public final class SupportDtos {
  private SupportDtos() {}

  public record Ticket(
      Long id,
      String reference,
      SupportTopic topic,
      String subject,
      SupportStatus status,
      Long orderId,
      Long bookId,
      Instant createdAt,
      Instant updatedAt,
      Instant resolvedAt,
      Instant closedAt,
      boolean canReply) {
    public static Ticket from(SupportTicket ticket) {
      return new Ticket(
          ticket.getId(),
          ticket.getReference(),
          ticket.getTopic(),
          ticket.getSubject(),
          ticket.getStatus(),
          ticket.getRelatedOrder() == null ? null : ticket.getRelatedOrder().getId(),
          ticket.getRelatedBook() == null ? null : ticket.getRelatedBook().getId(),
          ticket.getCreatedAt(),
          ticket.getUpdatedAt(),
          ticket.getResolvedAt(),
          ticket.getClosedAt(),
          ticket.getStatus() != SupportStatus.CLOSED);
    }
  }

  public record AdminTicket(
      Ticket ticket,
      Long requesterId,
      String contactName,
      String contactEmail,
      Long assignedToId,
      String assignedToName) {
    public static AdminTicket from(SupportTicket ticket) {
      return new AdminTicket(
          Ticket.from(ticket),
          ticket.getRequester() == null ? null : ticket.getRequester().getId(),
          ticket.getContactName(),
          ticket.getContactEmail(),
          ticket.getAssignedTo() == null ? null : ticket.getAssignedTo().getId(),
          ticket.getAssignedTo() == null ? null : name(ticket.getAssignedTo()));
    }
  }

  public record Message(
      Long id, SupportMessageKind kind, String authorName, String body, Instant createdAt) {
    public static Message from(SupportMessage message) {
      String author =
          switch (message.getKind()) {
            case REQUESTER -> message.getTicket().getContactName();
            case ADMIN -> "Echipa BookNest";
            case SYSTEM -> "BookNest";
          };
      return new Message(
          message.getId(), message.getKind(), author, message.getBody(), message.getCreatedAt());
    }
  }

  public record AdminMessage(
      Message message, SupportEmailStatus emailStatus, int emailAttempts, String emailError) {
    public static AdminMessage from(SupportMessage message, SupportEmailDelivery email) {
      return new AdminMessage(
          Message.from(message),
          email == null ? null : email.getStatus(),
          email == null ? 0 : email.getAttempts(),
          email == null ? null : email.getLastError());
    }
  }

  public record Administrator(Long id, String name) {
    public static Administrator from(User user) {
      return new Administrator(user.getId(), SupportDtos.name(user));
    }
  }

  private static String name(User user) {
    return user.getFirstName() + " " + user.getLastName();
  }
}
