package com.avbooknest.reporting;

import static com.avbooknest.reporting.ReportDtos.*;
import static org.junit.jupiter.api.Assertions.*;

import com.avbooknest.admin.service.*;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.auth.service.AccountSecurityService;
import com.avbooknest.common.exception.*;
import com.avbooknest.notification.service.NotificationService;
import jakarta.persistence.EntityManager;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate", showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  ReportingService.class,
  ReportStore.class,
  EvidenceValidator.class,
  AdminService.class,
  AdminAuditService.class,
  NotificationService.class,
  SuspensionExpiryService.class
})
@EnabledIfEnvironmentVariable(named = "BOOKNEST_TEST_DATABASE_URL", matches = ".+")
class ReportingPostgresTest {
  static final String SCHEMA = "reporting_" + UUID.randomUUID().toString().replace("-", "");
  static final String BUYER = "buyer@report.test",
      SELLER = "seller@report.test",
      ADMIN = "admin@report.test";
  @Autowired ReportingService service;
  @Autowired ReportStore store;
  @Autowired JdbcTemplate jdbc;
  @Autowired EntityManager em;
  @Autowired UserRepository users;
  @Autowired AdminService admin;
  @Autowired SuspensionExpiryService expiry;
  @MockitoBean AccountSecurityService accountSecurity;

  @DynamicPropertySource
  static void db(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", () -> System.getenv("BOOKNEST_TEST_DATABASE_URL"));
    r.add(
        "spring.datasource.username",
        () -> System.getenv().getOrDefault("BOOKNEST_TEST_DATABASE_USER", "booknest_test"));
    r.add(
        "spring.datasource.password",
        () -> System.getenv().getOrDefault("BOOKNEST_TEST_DATABASE_PASSWORD", ""));
    r.add("spring.flyway.schemas", () -> SCHEMA);
    r.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
    r.add("spring.datasource.hikari.connection-init-sql", () -> "SET search_path TO " + SCHEMA);
  }

  @BeforeEach
  void fixtures() {
    jdbc.update(
        """
        INSERT INTO users(id,email,first_name,last_name,password_hash,role_id,email_verified)
        VALUES (101,'buyer@report.test','Ana','Test','test',(SELECT id FROM roles WHERE name='USER'),true),
        (102,'seller@report.test','Ion','Test','test',(SELECT id FROM roles WHERE name='USER'),true),
        (103,'admin@report.test','Admin','Test','test',(SELECT id FROM roles WHERE name='ADMIN'),true),
        (104,'stranger@report.test','Other','Test','test',(SELECT id FROM roles WHERE name='USER'),true)
        """);
    jdbc.update("INSERT INTO categories(id,name,slug) VALUES(1001,'Books','report-books')");
    jdbc.update(
        """
        INSERT INTO books(id,title,author,price,book_condition,language,seller_id,category_id)
        VALUES(1001,'Reported book','Author',20,'GOOD','Română',102,1001),
        (1002,'Other book','Author',20,'GOOD','Română',102,1001)
        """);
  }

  Create book() {
    return new Create(Target.BOOK, 1001L, Reason.FRAUD, "Detalii despre o posibilă fraudă.");
  }

  @Test
  void deletingTheListingKeepsReportsAndDoesNotCollideWithOtherDeletedTargets() {
    long first = report();
    long second =
        service.create(BUYER, new Create(Target.BOOK, 1002L, Reason.SPAM, null), List.of()).id();
    jdbc.update("DELETE FROM books WHERE id IN (1001,1002)");
    assertNull(store.get(first, false).bookId());
    assertNull(store.get(second, false).bookId());
    assertEquals("Reported book", store.get(first, false).targetLabel());
    assertEquals(102L, store.get(first, false).targetUserId());
    assertEquals(2, store.list(101L, null, null, 0, 20).totalElements());
    assertEquals(
        Status.RESOLVED,
        service
            .resolve(ADMIN, first, new Resolve(Decision.WARN, "Verified even after deletion", null))
            .report()
            .status());
  }

  @Test
  void cannotSanctionAnAdministratorThroughAListingReport() {
    jdbc.update("UPDATE books SET seller_id=103 WHERE id=1001");
    long id = report();
    assertThrows(
        ForbiddenException.class,
        () -> service.resolve(ADMIN, id, new Resolve(Decision.SUSPEND, "Attempt", 7)));
    assertEquals(Status.NEW, store.get(id, false).status());
  }

  @Test
  void suspensionDurationMustBePresentAndBoundedAndOnlyAppliesToSuspension() {
    long id = report();
    assertThrows(
        BadRequestException.class,
        () -> service.resolve(ADMIN, id, new Resolve(Decision.SUSPEND, "No duration", null)));
    assertThrows(
        BadRequestException.class,
        () -> service.resolve(ADMIN, id, new Resolve(Decision.SUSPEND, "Too long", 91)));
    assertThrows(
        BadRequestException.class,
        () -> service.resolve(ADMIN, id, new Resolve(Decision.WARN, "Inapplicable duration", 7)));
  }

  long report() {
    return service.create(BUYER, book(), List.of()).id();
  }

  void flush() {
    em.flush();
    em.clear();
  }

  @Test
  void persistsEvidencePrivatelyAndKeepsSnapshot() throws Exception {
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
    long id =
        service
            .create(
                BUYER,
                book(),
                List.of(
                    new MockMultipartFile(
                        "evidence", "secret.png", "image/png", bytes.toByteArray())))
            .id();
    var mine = service.details(BUYER, id, false);
    assertEquals("Reported book", mine.report().targetLabel());
    assertTrue(mine.events().isEmpty());
    long evidenceId = mine.evidence().getFirst().id();
    assertEquals("image/png", service.evidence(BUYER, id, evidenceId).contentType());
    assertTrue(service.evidence(ADMIN, id, evidenceId).content().length > 0);
    assertThrows(NotFoundException.class, () -> service.evidence(SELLER, id, evidenceId));
    assertThrows(NotFoundException.class, () -> service.details(SELLER, id, false));
    assertThrows(NotFoundException.class, () -> service.evidence(BUYER, id, evidenceId + 1));
    assertEquals(0, service.list(SELLER, false, null, null, 0, 20).totalElements());
    assertEquals(1, service.list(ADMIN, true, Status.NEW, 102L, 0, 20).totalElements());
  }

  @Test
  void rejectsDuplicateOpenReports() {
    report();
    assertThrows(ConflictException.class, () -> service.create(BUYER, book(), List.of()));
    assertEquals(1, store.list(101L, null, null, 0, 20).totalElements());
  }

  @Test
  void rejectsOwnListing() {
    assertThrows(BadRequestException.class, () -> service.create(SELLER, book(), List.of()));
  }

  @Test
  void rejectsPrivateAccountWithoutTradingRelationship() {
    assertThrows(
        NotFoundException.class,
        () ->
            service.create(BUYER, new Create(Target.USER, 104L, Reason.HARASSMENT, ""), List.of()));
  }

  @Test
  void requiresExplanationForOtherReason() {
    assertThrows(
        BadRequestException.class,
        () -> service.create(BUYER, new Create(Target.BOOK, 1001L, Reason.OTHER, " "), List.of()));
  }

  @Test
  void cannotReportHiddenOrDraftListing() {
    jdbc.update("UPDATE books SET status='DRAFT' WHERE id=1001");
    assertThrows(NotFoundException.class, () -> service.create(BUYER, book(), List.of()));
  }

  @Test
  void rateLimitIsPersisted() {
    for (int i = 0; i < 10; i++) {
      long id = store.create(101L, Target.BOOK, 102L, 1001L, "Book", Reason.SPAM, "");
      store.resolve(id, 103L, new Resolve(Decision.DISMISS, "No violation", null), null);
    }
    assertThrows(RateLimitExceededException.class, () -> report());
  }

  @Test
  void sellerCanReportBuyerFromTheirOrder() {
    jdbc.update(
        """
        INSERT INTO orders(id,order_number,buyer_id,status,subtotal,total_amount,currency,recipient_name,recipient_email,recipient_phone)
        VALUES(2001,'REPORT-ORDER',101,'SHIPPED',20,20,'RON','Ana Test','buyer@report.test','0700000000')
        """);
    jdbc.update(
        "INSERT INTO seller_orders(id,order_id,seller_id,status,item_subtotal,commission_amount,seller_proceeds) VALUES(2001,2001,102,'ACCEPTED',20,1,19)");
    var result =
        service.create(SELLER, new Create(Target.USER, 101L, Reason.HARASSMENT, ""), List.of());
    assertEquals(101L, result.targetUserId());
  }

  @Test
  void claimAndDismissDoNotCreateAnInfraction() {
    long id = report();
    assertEquals(103L, service.start(ADMIN, id).report().assignedToId());
    service.start(ADMIN, id);
    assertEquals(2, store.events(id).size());
    assertEquals(
        Status.DISMISSED,
        service
            .resolve(ADMIN, id, new Resolve(Decision.DISMISS, "Nu se confirmă", null))
            .report()
            .status());
    assertTrue(service.history(ADMIN, 102L).entries().isEmpty());
    assertThrows(
        ConflictException.class,
        () -> service.resolve(ADMIN, id, new Resolve(Decision.WARN, "Duplicate decision", null)));
  }

  @Test
  void warningIsInHistoryAndNotifications() {
    long id = report();
    service.resolve(ADMIN, id, new Resolve(Decision.WARN, "Respectă regulile comunității", null));
    flush();
    assertEquals("REPORT_WARNING", service.history(ADMIN, 102L).entries().getFirst().action());
    assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM notifications", Integer.class));
    assertTrue(users.findById(102L).orElseThrow().isEnabled());
  }

  @Test
  void hideOnlyTheReportedBook() {
    service.resolve(ADMIN, report(), new Resolve(Decision.HIDE_BOOK, "Anunț înșelător", null));
    flush();
    assertEquals(
        "HIDDEN",
        jdbc.queryForObject("SELECT moderation_status FROM books WHERE id=1001", String.class));
    assertEquals(
        "VISIBLE",
        jdbc.queryForObject("SELECT moderation_status FROM books WHERE id=1002", String.class));
    assertTrue(users.findById(102L).orElseThrow().isEnabled());
  }

  @Test
  void temporarySuspensionExpiresWithoutRestoringManuallyHiddenBooks() {
    jdbc.update(
        "INSERT INTO refresh_tokens(user_id,token,expires_at) VALUES(102,'synthetic-token',CURRENT_TIMESTAMP + INTERVAL '30 days')");
    jdbc.update(
        "UPDATE books SET moderation_status='HIDDEN',moderation_reason='POLICY_VIOLATION',moderation_note='Existing violation' WHERE id=1002");
    long id = report();
    service.resolve(ADMIN, id, new Resolve(Decision.SUSPEND, "Fraudă confirmată", 7));
    flush();
    var target = users.findById(102L).orElseThrow();
    assertFalse(target.isEnabled());
    assertNotNull(target.getSuspendedUntil());
    assertTrue(
        jdbc.queryForObject("SELECT revoked FROM refresh_tokens WHERE user_id=102", Boolean.class));
    assertEquals(
        "ACCOUNT_SUSPENDED",
        jdbc.queryForObject("SELECT moderation_reason FROM books WHERE id=1001", String.class));
    expiry.expire(102L);
    assertFalse(target.isEnabled());
    target.setSuspendedUntil(Instant.now().minusSeconds(1));
    flush();
    expiry.expire(102L);
    flush();
    assertTrue(users.findById(102L).orElseThrow().isEnabled());
    assertEquals(
        "VISIBLE",
        jdbc.queryForObject("SELECT moderation_status FROM books WHERE id=1001", String.class));
    assertEquals(
        "HIDDEN",
        jdbc.queryForObject("SELECT moderation_status FROM books WHERE id=1002", String.class));
    assertNotNull(store.get(id, false).suspendedUntil());
    assertTrue(
        service.history(ADMIN, 102L).entries().stream()
            .anyMatch(e -> e.action().equals("USER_SUSPENSION_EXPIRED")));
  }

  @Test
  void permanentSuspensionAndManualReactivationClearExpiry() {
    service.resolve(ADMIN, report(), new Resolve(Decision.SUSPEND, "Temporary", 2));
    flush();
    admin.suspendUser(ADMIN, 102L, "Permanent decision");
    flush();
    assertNull(users.findById(102L).orElseThrow().getSuspendedUntil());
    expiry.expire(102L);
    assertFalse(users.findById(102L).orElseThrow().isEnabled());
    admin.reactivateUser(ADMIN, 102L, "Appeal accepted");
    flush();
    assertTrue(users.findById(102L).orElseThrow().isEnabled());
  }

  @Test
  void cannotOverwriteExistingSuspension() {
    long id = report();
    admin.suspendUser(ADMIN, 102L, "Existing permanent suspension");
    assertThrows(
        ConflictException.class,
        () -> service.resolve(ADMIN, id, new Resolve(Decision.SUSPEND, "Shorten", 1)));
    assertNull(users.findById(102L).orElseThrow().getSuspendedUntil());
  }

  @Test
  void ordinaryUserCannotModerateOrReadHistory() {
    long id = report();
    assertThrows(ForbiddenException.class, () -> service.start(BUYER, id));
    assertThrows(ForbiddenException.class, () -> service.history(BUYER, 102L));
  }
}
