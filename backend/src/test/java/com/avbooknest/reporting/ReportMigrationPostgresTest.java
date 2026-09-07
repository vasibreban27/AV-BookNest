package com.avbooknest.reporting;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "BOOKNEST_TEST_DATABASE_URL", matches = ".+")
class ReportMigrationPostgresTest {
  @Test
  void upgradesAlreadyAppliedV22WithoutChangingExistingData() throws Exception {
    String url = System.getenv("BOOKNEST_TEST_DATABASE_URL");
    String user = System.getenv().getOrDefault("BOOKNEST_TEST_DATABASE_USER", "booknest_test");
    String password = System.getenv().getOrDefault("BOOKNEST_TEST_DATABASE_PASSWORD", "");
    String schema = "report_upgrade_" + UUID.randomUUID().toString().replace("-", "");
    Flyway.configure()
        .dataSource(url, user, password)
        .schemas(schema)
        .target("22")
        .load()
        .migrate();
    try (var connection = DriverManager.getConnection(url, user, password)) {
      connection.setSchema(schema);
      try (var s = connection.createStatement()) {
        s.execute(
            """
            INSERT INTO users(id,email,first_name,last_name,password_hash,role_id,enabled,suspension_reason)
            VALUES(901,'old@test.local','Old','User','test',(SELECT id FROM roles WHERE name='USER'),false,'Existing suspension');
            INSERT INTO support_tickets(reference,contact_name,contact_email,topic,subject,privacy_accepted_at)
            VALUES('BN-SUP-KEEP','Visitor','visitor@test.local','GENERAL','Existing request',CURRENT_TIMESTAMP)
            """);
        s.execute(
            """
            INSERT INTO users(id,email,first_name,last_name,password_hash,role_id,enabled)
            VALUES(902,'reporter@test.local','Old','Reporter','test',(SELECT id FROM roles WHERE name='USER'),true);
            INSERT INTO categories(id,name,slug) VALUES(9001,'Old reports','old-reports');
            INSERT INTO books(id,title,author,price,book_condition,language,seller_id,category_id)
            VALUES(9001,'Existing listing','Author',10,'GOOD','Română',901,9001);
            INSERT INTO content_reports(id,reporter_id,target_type,target_user_id,book_id,target_label,reason,description)
            VALUES(9001,902,'BOOK',901,9001,'Existing listing','SPAM','Keep this report')
            """);
      }
      var latest = Flyway.configure().dataSource(url, user, password).schemas(schema).load();
      assertEquals(1, latest.migrate().migrationsExecuted);
      latest.validate();
      try (var s = connection.createStatement();
          var row =
              s.executeQuery(
                  "SELECT enabled,suspension_reason,suspended_until FROM users WHERE id=901")) {
        assertTrue(row.next());
        assertFalse(row.getBoolean(1));
        assertEquals("Existing suspension", row.getString(2));
        assertNull(row.getTimestamp(3));
      }
      try (var s = connection.createStatement();
          var row =
              s.executeQuery("SELECT subject FROM support_tickets WHERE reference='BN-SUP-KEEP'")) {
        assertTrue(row.next());
        assertEquals("Existing request", row.getString(1));
      }
      try (var s = connection.createStatement();
          var row =
              s.executeQuery("SELECT target_id,description FROM content_reports WHERE id=9001")) {
        assertTrue(row.next());
        assertEquals(9001L, row.getLong(1));
        assertEquals("Keep this report", row.getString(2));
      }
      try (var s = connection.createStatement()) {
        s.execute("DELETE FROM books WHERE id=9001");
      }
      try (var s = connection.createStatement();
          var row = s.executeQuery("SELECT book_id,target_id FROM content_reports WHERE id=9001")) {
        assertTrue(row.next());
        assertNull(row.getObject(1));
        assertEquals(9001L, row.getLong(2));
      }
    }
  }
}
