package com.avbooknest.reporting;

import com.avbooknest.admin.service.AdminAuditService;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.book.model.*;
import com.avbooknest.book.repository.BookRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SuspensionExpiryService {
  private final UserRepository users;
  private final BookRepository books;
  private final AdminAuditService audit;

  public SuspensionExpiryService(
      UserRepository users, BookRepository books, AdminAuditService audit) {
    this.users = users;
    this.books = books;
    this.audit = audit;
  }

  @Transactional
  public void expire(long id) {
    var user = users.findByIdForUpdate(id).orElse(null);
    Instant now = Instant.now();
    if (user == null
        || user.isEnabled()
        || user.getSuspendedUntil() == null
        || user.getSuspendedUntil().isAfter(now)) return;
    var issuer = user.getSuspendedBy();
    user.reactivate(now);
    books
        .findAllBySellerIdAndModerationStatusAndModerationReason(
            id, BookModerationStatus.HIDDEN, BookModerationReason.ACCOUNT_SUSPENDED)
        .forEach(book -> book.restore(issuer, now));
    // Retain the original issuer as audit actor; action explicitly identifies automatic expiry.
    if (issuer != null)
      audit.record(
          issuer,
          "USER_SUSPENSION_EXPIRED",
          "USER",
          id,
          "Suspendarea temporară a expirat automat.",
          null);
  }
}
