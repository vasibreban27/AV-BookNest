package com.avbooknest.review;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "BOOKNEST_TEST_DATABASE_URL", matches = ".+")
class ReviewMigrationPostgresTest {
  @Test
  void upgradePreservesLegacyFeedbackButDoesNotInventVerifiedPurchases() throws Exception {
    String url = System.getenv("BOOKNEST_TEST_DATABASE_URL");
    String user = System.getenv().getOrDefault("BOOKNEST_TEST_DATABASE_USER", "booknest_test");
    String password = System.getenv().getOrDefault("BOOKNEST_TEST_DATABASE_PASSWORD", "");
    String schema = "review_upgrade_" + UUID.randomUUID().toString().replace("-", "");
    Flyway.configure()
        .dataSource(url, user, password)
        .schemas(schema)
        .target("18")
        .load()
        .migrate();
    try (var connection = DriverManager.getConnection(url, user, password)) {
      connection.setSchema(schema);
      try (var statement = connection.createStatement()) {
        statement.execute(
            """
            INSERT INTO users(id,email,first_name,last_name,password_hash,role_id)
            VALUES (101,'legacy-buyer@reviews.test','Ana','Popescu','test',(SELECT id FROM roles WHERE name='USER')),
            (102,'legacy-seller@reviews.test','Ion','Ionescu','test',(SELECT id FROM roles WHERE name='USER'));
            INSERT INTO categories(id,name,slug) VALUES(1001,'Legacy','legacy');
            INSERT INTO books(id,title,author,price,book_condition,language,seller_id,category_id)
            VALUES(1001,'Legacy title','Autor',20,'GOOD','Română',102,1001);
            INSERT INTO reviews(id,book_id,reviewer_id,rating,comment)
            VALUES(4001,1001,101,4,'Keep this feedback');
            """);
      }
      Flyway.configure().dataSource(url, user, password).schemas(schema).load().migrate();
      try (var statement = connection.createStatement();
          var rows = statement.executeQuery("SELECT * FROM reviews WHERE id=4001")) {
        assertTrue(rows.next());
        assertEquals("Keep this feedback", rows.getString("comment"));
        assertEquals(4, rows.getInt("rating"));
        assertEquals(102L, rows.getLong("seller_id"));
        assertEquals("Legacy title", rows.getString("book_title"));
        assertEquals("HIDDEN", rows.getString("moderation_status"));
        assertNotNull(rows.getString("moderation_reason"));
        assertNull(rows.getObject("order_item_id"));
        assertNull(rows.getObject("description_rating"));
        assertFalse(rows.next());
      }
    }
  }
}
