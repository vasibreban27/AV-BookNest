package com.avbooknest.review;

import static org.junit.jupiter.api.Assertions.*;

import com.avbooknest.admin.service.AdminAuditService;
import com.avbooknest.review.dto.CreateReviewRequest;
import com.avbooknest.review.dto.ModerateReviewRequest;
import com.avbooknest.review.model.ReviewModerationStatus;
import com.avbooknest.review.repository.ReviewRepository;
import com.avbooknest.review.service.ReviewService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Uses an isolated schema and rollback-only fixtures; see scripts/test-reviews-postgres.ps1. */
@DataJpaTest(
    properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.show-sql=false"},
    showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ReviewService.class, AdminAuditService.class})
@EnabledIfEnvironmentVariable(named = "BOOKNEST_TEST_DATABASE_URL", matches = ".+")
class ReviewPostgresTest {
  private static final String SCHEMA =
      "review_test_" + UUID.randomUUID().toString().replace("-", "");
  @Autowired JdbcTemplate jdbc;
  @Autowired ReviewService service;
  @Autowired ReviewRepository reviews;

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
        VALUES (101,'buyer@reviews.test','Ana','Popescu','test',(SELECT id FROM roles WHERE name='USER'),true),
        (102,'seller@reviews.test','Ion','Ionescu','test',(SELECT id FROM roles WHERE name='USER'),true),
        (103,'admin@reviews.test','Admin','Test','test',(SELECT id FROM roles WHERE name='ADMIN'),true)
        """);
    jdbc.update("INSERT INTO categories(id,name,slug) VALUES (1001,'Review books','review-books')");
    jdbc.update(
        """
        INSERT INTO books(id,title,author,price,book_condition,language,seller_id,category_id)
        VALUES (1001,'Cartea întâi','Autor',20,'GOOD','Română',102,1001),
        (1002,'Cartea a doua','Autor',20,'GOOD','Română',102,1001)
        """);
    jdbc.update(
        """
        INSERT INTO orders(id,order_number,buyer_id,status,subtotal,total_amount,currency,recipient_name,recipient_email,recipient_phone)
        VALUES (2001,'REVIEW-TEST',101,'SHIPPED',40,40,'RON','Ana Popescu','buyer@reviews.test','0700000000')
        """);
    jdbc.update(
        """
        INSERT INTO seller_orders(id,order_id,seller_id,status,item_subtotal,commission_amount,seller_proceeds,fulfilled_at)
        VALUES (2001,2001,102,'FULFILLED',40,2,38,CURRENT_TIMESTAMP - INTERVAL '1 hour')
        """);
    jdbc.update(
        """
        INSERT INTO order_items(id,order_id,book_id,seller_id,seller_order_id,title,author,unit_price)
        VALUES (3001,2001,1001,102,2001,'Cartea întâi','Autor',20),
        (3002,2001,1002,102,2001,'Cartea a doua','Autor',20)
        """);
  }

  @Test
  void newSellerHasNoInventedRating() {
    var stats = service.reputation(102L).ratings();
    assertEquals(0L, stats.reviewCount());
    assertNull(stats.sellerRating());
    assertTrue(service.publicReviews(102L, 0, 10).content().isEmpty());
  }

  @Test
  void buyerOnlyAccountsAreNotExposedAsPublicSellerProfiles() {
    assertThrows(
        com.avbooknest.common.exception.NotFoundException.class, () -> service.reputation(101L));
    assertThrows(
        com.avbooknest.common.exception.NotFoundException.class,
        () -> service.publicReviews(101L, 0, 10));
  }

  @Test
  void persistenceModerationPaginationAndReputationUseSameVisibleSet() {
    var first =
        service.create(
            2001L, 3001L, "buyer@reviews.test", new CreateReviewRequest(5, 4, 3, "Foarte bine"));
    service.create(2001L, 3002L, "buyer@reviews.test", new CreateReviewRequest(1, 2, 1, null));
    assertEquals(2L, service.reputation(102L).ratings().reviewCount());
    assertEquals(3.0, service.reputation(102L).ratings().sellerRating());
    assertEquals(3.0, service.reputation(102L).ratings().descriptionRating());
    assertEquals(2.0, service.reputation(102L).ratings().conditionRating());
    var page = service.publicReviews(102L, 0, 1);
    assertTrue(page.hasNext());
    assertEquals(2, page.totalPages());
    assertNotEquals(
        page.content().getFirst().id(),
        service.publicReviews(102L, 1, 1).content().getFirst().id());

    service.moderate(
        first.review().id(),
        "admin@reviews.test",
        new ModerateReviewRequest(ReviewModerationStatus.HIDDEN, "Date personale"));
    assertEquals(1L, service.reputation(102L).ratings().reviewCount());
    assertEquals(1.0, service.reputation(102L).ratings().sellerRating());
    assertEquals(1L, service.publicReviews(102L, 0, 10).totalElements());
    assertEquals(
        1L,
        service
            .adminReviews("admin@reviews.test", ReviewModerationStatus.HIDDEN, "întâi", 0, 10)
            .totalElements());
    assertEquals(2, service.forOrder(2001L, "buyer@reviews.test").size());
    assertTrue(
        service.forOrder(2001L, "buyer@reviews.test").stream().noneMatch(row -> row.canReview()));

    service.moderate(
        first.review().id(),
        "admin@reviews.test",
        new ModerateReviewRequest(ReviewModerationStatus.VISIBLE, "Contestație acceptată"));
    assertEquals(2L, service.publicReviews(102L, 0, 10).totalElements());
    assertEquals(
        2,
        jdbc.queryForObject(
            "SELECT count(*) FROM admin_audit_logs WHERE target_type='REVIEW'", Integer.class));
  }

  @Test
  void databaseRejectsDuplicateEvenIfApplicationValidationIsBypassed() {
    service.create(2001L, 3001L, "buyer@reviews.test", new CreateReviewRequest(5, 5, 5, null));
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbc.update(
                """
        INSERT INTO reviews(book_id,reviewer_id,seller_id,book_title,order_item_id,rating,description_rating,condition_rating,moderation_status)
        VALUES (1001,101,102,'Duplicate',3001,5,5,5,'VISIBLE')
        """));
  }

  @Test
  void databaseRejectsVisibleUnverifiedReview() {
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbc.update(
                """
        INSERT INTO reviews(book_id,reviewer_id,seller_id,book_title,rating,moderation_status)
        VALUES (1001,101,102,'Unverified',5,'VISIBLE')
        """));
  }

  @Test
  void legacyFeedbackDoesNotAffectReputationAndCanCoexistWithVerifiedPurchase() {
    jdbc.update(
        """
        INSERT INTO reviews(book_id,reviewer_id,seller_id,book_title,rating,moderation_status,moderation_reason)
        VALUES (1001,101,102,'Legacy',5,'HIDDEN','No verified purchase')
        """);
    service.create(2001L, 3001L, "buyer@reviews.test", new CreateReviewRequest(2, 3, 4, null));
    assertEquals(2, reviews.count());
    assertEquals(1L, service.reputation(102L).ratings().reviewCount());
    assertEquals(2.0, service.reputation(102L).ratings().sellerRating());
  }
}
