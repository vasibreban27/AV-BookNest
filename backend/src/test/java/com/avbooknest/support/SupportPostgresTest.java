package com.avbooknest.support;

import static org.junit.jupiter.api.Assertions.*;

import com.avbooknest.admin.service.AdminAuditService;
import com.avbooknest.common.exception.*;
import com.avbooknest.contact.dto.ContactRequest;
import com.avbooknest.notification.service.NotificationService;
import com.avbooknest.support.dto.SupportRequests.*;
import com.avbooknest.support.model.*;
import com.avbooknest.support.repository.*;
import com.avbooknest.support.service.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest(
    properties = {"spring.jpa.hibernate.ddl-auto=validate", "app.mail.enabled=false"},
    showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  SupportService.class,
  SupportEmailService.class,
  SupportReplyRateLimitService.class,
  NotificationService.class,
  AdminAuditService.class
})
@EnabledIfEnvironmentVariable(named = "BOOKNEST_TEST_DATABASE_URL", matches = ".+")
class SupportPostgresTest {
  static final String SCHEMA = "support_test_" + UUID.randomUUID().toString().replace("-", "");
  @Autowired JdbcTemplate jdbc;
  @Autowired SupportService service;
  @Autowired SupportEmailRepository deliveries;

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", () -> System.getenv("BOOKNEST_TEST_DATABASE_URL"));
    registry.add(
        "spring.datasource.username",
        () -> System.getenv().getOrDefault("BOOKNEST_TEST_DATABASE_USER", "booknest_test"));
    registry.add(
        "spring.datasource.password",
        () -> System.getenv().getOrDefault("BOOKNEST_TEST_DATABASE_PASSWORD", ""));
    registry.add("spring.flyway.schemas", () -> SCHEMA);
    registry.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
    registry.add(
        "spring.datasource.hikari.connection-init-sql", () -> "SET search_path TO " + SCHEMA);
  }

  @BeforeEach
  void fixtures() {
    jdbc.update(
        """
      INSERT INTO users(id,email,first_name,last_name,password_hash,role_id,email_verified)
      VALUES (101,'buyer@support.test','Ana','Popescu','test',(SELECT id FROM roles WHERE name='USER'),true),
      (102,'seller@support.test','Ion','Ionescu','test',(SELECT id FROM roles WHERE name='USER'),true),
      (103,'admin@support.test','Admin','Test','test',(SELECT id FROM roles WHERE name='ADMIN'),true)
      """);
    jdbc.update(
        "INSERT INTO categories(id,name,slug) VALUES (1001,'Support books','support-books')");
    jdbc.update(
        """
      INSERT INTO books(id,title,author,price,book_condition,language,seller_id,category_id)
      VALUES (1001,'Carte','Autor',20,'GOOD','Română',102,1001)
      """);
    jdbc.update(
        """
      INSERT INTO orders(id,order_number,buyer_id,status,subtotal,total_amount,currency,recipient_name,recipient_email,recipient_phone)
      VALUES (2001,'SUPPORT-TEST',101,'SHIPPED',20,20,'RON','Ana Popescu','buyer@support.test','0700000000')
      """);
    jdbc.update(
        """
      INSERT INTO seller_orders(id,order_id,seller_id,status,item_subtotal,commission_amount,seller_proceeds)
      VALUES (2001,2001,102,'ACCEPTED',20,1,19)
      """);
    jdbc.update(
        """
      INSERT INTO order_items(id,order_id,book_id,seller_id,seller_order_id,title,author,unit_price)
      VALUES (3001,2001,1001,102,2001,'Carte','Autor',20)
      """);
  }

  @Test
  void guestPersistsWithOutboxButIsNeverClaimedByEmail() {
    var result = service.create(request(null, null), null);
    assertNull(result.ticketId());
    assertEquals(0, service.mine("buyer@support.test", null, 0, 20).totalElements());
    var admin =
        service.adminList("admin@support.test", null, null, true, result.reference(), 0, 20);
    assertEquals(1, admin.totalElements());
    assertNull(admin.content().getFirst().requesterId());
    var history =
        service.adminMessages(
            admin.content().getFirst().ticket().id(), "admin@support.test", 0, 30);
    assertEquals(SupportEmailStatus.DISABLED, history.content().getFirst().emailStatus());
    assertEquals(1, deliveries.count());
  }

  @Test
  void fullConversationStatusAssignmentAuditAndNotificationsPersist() {
    Long id = service.create(request(2001L, 1001L), "buyer@support.test").ticketId();
    service.assign(id, "admin@support.test", new Assignment(103L, "Preluare"));
    service.adminReply(id, "admin@support.test", new Reply("Am verificat situația."));
    service.changeStatus(
        id,
        "admin@support.test",
        new StatusChange(SupportStatus.RESOLVED, "Internal resolution detail"));
    assertEquals(SupportStatus.RESOLVED, service.getMine(id, "buyer@support.test").status());
    service.reply(id, "buyer@support.test", new Reply("Problema încă apare."));
    assertEquals(SupportStatus.IN_PROGRESS, service.getMine(id, "buyer@support.test").status());
    assertEquals(4, service.myMessages(id, "buyer@support.test", 0, 2).totalElements());
    assertTrue(service.myMessages(id, "buyer@support.test", 0, 2).hasNext());
    assertEquals(4, deliveries.count());
    assertEquals(
        3,
        jdbc.queryForObject(
            "SELECT count(*) FROM admin_audit_logs WHERE target_type='SUPPORT_TICKET'",
            Integer.class));
    assertEquals(4, jdbc.queryForObject("SELECT count(*) FROM notifications", Integer.class));
    assertFalse(
        service.myMessages(id, "buyer@support.test", 0, 30).content().stream()
            .anyMatch(m -> m.body().contains("Internal")));
  }

  @Test
  void userIsolationAppliesToEveryReadAndWrite() {
    Long id = service.create(request(null, null), "buyer@support.test").ticketId();
    assertEquals(0, service.mine("seller@support.test", null, 0, 20).totalElements());
    assertThrows(NotFoundException.class, () -> service.getMine(id, "seller@support.test"));
    assertThrows(
        NotFoundException.class, () -> service.myMessages(id, "seller@support.test", 0, 20));
    assertThrows(
        NotFoundException.class,
        () -> service.reply(id, "seller@support.test", new Reply("No access")));
  }

  @Test
  void orderSellerCanLinkOwnParcelAndHiddenListing() {
    jdbc.update(
        "UPDATE books SET moderation_status='HIDDEN', moderation_reason='OTHER' WHERE id=1001");
    Long id = service.create(request(2001L, 1001L), "seller@support.test").ticketId();
    assertEquals(2001L, service.getMine(id, "seller@support.test").orderId());
  }

  @Test
  void hiddenBookCannotBeLinkedWithoutPurchaseContext() {
    jdbc.update(
        "UPDATE books SET moderation_status='HIDDEN', moderation_reason='OTHER' WHERE id=1001");
    assertThrows(
        NotFoundException.class, () -> service.create(request(null, 1001L), "buyer@support.test"));
  }

  @Test
  void adminFiltersKeepUnassignedAndGuestTickets() {
    Long assigned = service.create(request(null, null), "buyer@support.test").ticketId();
    service.create(request(null, null), null);
    service.assign(assigned, "admin@support.test", new Assignment(103L, "Preluare"));
    assertEquals(
        2, service.adminList("admin@support.test", null, null, false, "", 0, 20).totalElements());
    assertEquals(
        1, service.adminList("admin@support.test", null, null, true, "", 0, 20).totalElements());
    assertEquals(
        1,
        service
            .adminList("admin@support.test", SupportStatus.NEW, 103L, false, "Ajutor", 0, 20)
            .totalElements());
    assertEquals(1, service.administrators("admin@support.test").size());
  }

  @Test
  void closureBlocksReplyAndReopeningRestoresIt() {
    Long id = service.create(request(null, null), "buyer@support.test").ticketId();
    service.changeStatus(
        id, "admin@support.test", new StatusChange(SupportStatus.CLOSED, "Închidere"));
    assertFalse(service.getMine(id, "buyer@support.test").canReply());
    assertThrows(
        ConflictException.class, () -> service.reply(id, "buyer@support.test", new Reply("Reply")));
  }

  @Test
  void longestValidAccountNameFitsTheContactSnapshot() {
    jdbc.update(
        "UPDATE users SET first_name=?,last_name=? WHERE id=101", "A".repeat(100), "B".repeat(100));
    Long id = service.create(request(null, null), "buyer@support.test").ticketId();
    assertEquals(201, service.adminGet(id, "admin@support.test").contactName().length());
  }

  private ContactRequest request(Long orderId, Long bookId) {
    return new ContactRequest(
        "Visitor",
        "buyer@support.test",
        "GENERAL",
        "Ajutor comandă",
        "Am nevoie de ajutor pentru această situație.",
        true,
        "",
        orderId,
        bookId);
  }
}
