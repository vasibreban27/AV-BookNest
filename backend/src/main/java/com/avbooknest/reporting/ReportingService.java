package com.avbooknest.reporting;

import static com.avbooknest.reporting.ReportDtos.*;

import com.avbooknest.admin.dto.AdminRequests.BookModerationRequest;
import com.avbooknest.admin.service.AdminAuditService;
import com.avbooknest.admin.service.AdminService;
import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.book.model.*;
import com.avbooknest.book.repository.BookRepository;
import com.avbooknest.common.dto.PageResponse;
import com.avbooknest.common.exception.*;
import com.avbooknest.notification.model.NotificationType;
import com.avbooknest.notification.service.NotificationService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class ReportingService {
  private final ReportStore store;
  private final UserRepository users;
  private final BookRepository books;
  private final EvidenceValidator evidence;
  private final AdminService adminService;
  private final AdminAuditService audit;
  private final NotificationService notifications;

  public ReportingService(
      ReportStore store,
      UserRepository users,
      BookRepository books,
      EvidenceValidator evidence,
      AdminService adminService,
      AdminAuditService audit,
      NotificationService notifications) {
    this.store = store;
    this.users = users;
    this.books = books;
    this.evidence = evidence;
    this.adminService = adminService;
    this.audit = audit;
    this.notifications = notifications;
  }

  public Summary create(String email, Create request, List<MultipartFile> files) {
    User reporter = actor(email, false);
    String description = request.description() == null ? "" : request.description().trim();
    if (request.reason() == Reason.OTHER && description.length() < 10)
      throw new BadRequestException("Describe the issue in at least 10 characters for OTHER");
    var images = evidence.validate(files);
    // Serialize submissions for this account: rate/duplicate checks also work across instances.
    users
        .findByIdForUpdate(reporter.getId())
        .orElseThrow(() -> new NotFoundException("User not found"));
    if (store.recentCount(reporter.getId()) >= 10) throw new RateLimitExceededException(86400);
    User target;
    Long bookId = null;
    String label;
    if (request.targetType() == Target.BOOK) {
      Book book =
          books
              .findById(request.targetId())
              .orElseThrow(() -> new NotFoundException("Listing not found"));
      if (book.getStatus() != BookStatus.AVAILABLE
          || book.getModerationStatus() != BookModerationStatus.VISIBLE
          || !book.getCategory().isActive()
          || !book.getSeller().isEnabled()) throw new NotFoundException("Listing not found");
      target = book.getSeller();
      bookId = book.getId();
      label = book.getTitle();
    } else {
      target =
          users
              .findById(request.targetId())
              .orElseThrow(() -> new NotFoundException("User not found"));
      if (users.findSellerById(target.getId()).isEmpty()
          && !store.tradingPartners(reporter.getId(), target.getId()))
        throw new NotFoundException("User not found");
      label = target.getFirstName() + " " + target.getLastName();
    }
    if (reporter.getId().equals(target.getId()))
      throw new BadRequestException("You cannot report yourself or your own listing");
    if (store.duplicate(reporter.getId(), request.targetType(), target.getId(), bookId))
      throw new ConflictException("You already have an open report for this target");
    long id =
        store.create(
            reporter.getId(),
            request.targetType(),
            target.getId(),
            bookId,
            label,
            request.reason(),
            description);
    images.forEach(image -> store.addEvidence(id, image));
    store.event(id, reporter.getId(), "CREATED", "Raportare trimisă");
    return store.get(id, false);
  }

  @Transactional(readOnly = true)
  public PageResponse<Summary> list(
      String email, boolean admin, Status status, Long targetUser, int page, int size) {
    User actor = actor(email, admin);
    validatePage(page, size);
    return store.list(admin ? null : actor.getId(), status, admin ? targetUser : null, page, size);
  }

  @Transactional(readOnly = true)
  public Details details(String email, long id, boolean admin) {
    User actor = actor(email, admin);
    Summary report = store.get(id, false);
    checkAccess(actor, report, admin);
    return new Details(report, store.evidenceInfo(id), admin ? store.events(id) : List.of());
  }

  @Transactional(readOnly = true)
  public Evidence evidence(String email, long reportId, long evidenceId) {
    User actor = actor(email, false);
    Summary report = store.get(reportId, false);
    checkAccess(actor, report, "ADMIN".equals(actor.getRole().getName()));
    return store.evidence(reportId, evidenceId);
  }

  public Details start(String email, long id) {
    User admin = actor(email, true);
    Summary report = store.get(id, true);
    open(report);
    if (report.status() == Status.IN_PROGRESS
        && Long.valueOf(admin.getId()).equals(report.assignedToId()))
      return details(email, id, true);
    store.start(id, admin.getId());
    store.event(id, admin.getId(), "IN_PROGRESS", "Preluată de administrator");
    audit.record(admin, "REPORT_CLAIMED", "REPORT", id, null, null);
    return details(email, id, true);
  }

  public Details resolve(String email, long id, Resolve request) {
    User admin = actor(email, true);
    Summary report = store.get(id, true);
    open(report);
    if (request.decision() == Decision.SUSPEND
        && (request.suspensionDays() == null
            || request.suspensionDays() < 1
            || request.suspensionDays() > 90))
      throw new BadRequestException("Suspension must be between 1 and 90 days");
    if (request.decision() != Decision.SUSPEND && request.suspensionDays() != null)
      throw new BadRequestException("Duration only applies to suspension");
    User target =
        users
            .findByIdForUpdate(report.targetUserId())
            .orElseThrow(() -> new NotFoundException("User not found"));
    if (request.decision() != Decision.DISMISS
        && (target.getId().equals(admin.getId()) || "ADMIN".equals(target.getRole().getName())))
      throw new ForbiddenException("Administrator accounts cannot be sanctioned here");
    Instant until = null;
    switch (request.decision()) {
      case HIDE_BOOK -> {
        if (report.bookId() == null) throw new BadRequestException("This report has no listing");
        adminService.hideBook(
            email,
            report.bookId(),
            new BookModerationRequest(BookModerationReason.POLICY_VIOLATION, request.note()));
        audit.record(admin, "REPORT_BOOK_HIDDEN", "USER", target.getId(), request.note(), null);
      }
      case SUSPEND -> {
        if (!target.isEnabled())
          throw new ConflictException(
              "Account already suspended; review its history before changing the sanction");
        until = Instant.now().plus(request.suspensionDays(), ChronoUnit.DAYS);
        adminService.suspendUser(email, target.getId(), request.note());
        target.setSuspendedUntil(until);
        audit.record(
            admin,
            "REPORT_TEMPORARY_SUSPENSION",
            "USER",
            target.getId(),
            request.note(),
            "{\"until\":\"" + until + "\"}");
      }
      case WARN ->
          audit.record(admin, "REPORT_WARNING", "USER", target.getId(), request.note(), null);
      case DISMISS -> {}
    }
    store.resolve(id, admin.getId(), request, until);
    store.event(id, admin.getId(), request.decision().name(), request.note().trim());
    audit.record(
        admin,
        "REPORT_RESOLVED",
        "REPORT",
        id,
        request.note(),
        "{\"decision\":\"" + request.decision() + "\"}");
    users
        .findById(report.reporterId())
        .ifPresent(
            reporter ->
                notifications.create(
                    reporter,
                    NotificationType.REPORT_RESOLVED,
                    "Raportarea a fost verificată",
                    "Raportarea #" + id + " are o decizie. Consultă detaliile.",
                    "/reports/" + id));
    if (request.decision() != Decision.DISMISS)
      notifications.create(
          target,
          NotificationType.MODERATION_NOTICE,
          "Decizie de moderare",
          request.note().trim() + (until == null ? "" : " Suspendare până la " + until),
          "/contact");
    return details(email, id, true);
  }

  @Transactional(readOnly = true)
  public AccountHistory history(String email, long id) {
    actor(email, true);
    User user = users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    return new AccountHistory(
        user.isEnabled(),
        user.getSuspendedUntil(),
        user.getSuspensionReason(),
        store.accountHistory(id));
  }

  private User actor(String email, boolean admin) {
    User user = users.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
    if (!user.isEnabled() || (admin && !"ADMIN".equals(user.getRole().getName())))
      throw new ForbiddenException("Access denied");
    return user;
  }

  private void checkAccess(User actor, Summary report, boolean admin) {
    if (!admin && report.reporterId() != actor.getId())
      throw new NotFoundException("Report not found");
  }

  private void open(Summary report) {
    if (report.status() == Status.RESOLVED || report.status() == Status.DISMISSED)
      throw new ConflictException("Report already resolved");
  }

  private void validatePage(int page, int size) {
    if (page < 0 || size < 1 || size > 100) throw new BadRequestException("Invalid pagination");
  }
}
