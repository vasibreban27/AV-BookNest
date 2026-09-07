package com.avbooknest.support;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "BOOKNEST_TEST_DATABASE_URL", matches = ".+")
class SupportMigrationPostgresTest {
  @Test
  void upgradeFromAppliedV20PreservesTicketsAndDoesNotRequireRepair() throws Exception {
    String url = System.getenv("BOOKNEST_TEST_DATABASE_URL");
    String user = System.getenv().getOrDefault("BOOKNEST_TEST_DATABASE_USER", "booknest_test");
    String password = System.getenv().getOrDefault("BOOKNEST_TEST_DATABASE_PASSWORD", "");
    String schema = "support_upgrade_" + UUID.randomUUID().toString().replace("-", "");
    Flyway.configure()
        .dataSource(url, user, password)
        .schemas(schema)
        .target("20")
        .load()
        .migrate();
    try (Connection connection = DriverManager.getConnection(url, user, password)) {
      connection.setSchema(schema);
      assertEquals(200, columnLength(connection));
      try (var statement = connection.createStatement()) {
        statement.execute(
            """
            INSERT INTO support_tickets(id,reference,contact_name,contact_email,topic,subject,privacy_accepted_at)
            VALUES (1001,'BN-SUP-UPGRADE','Existing visitor','visitor@support.test','GENERAL','Keep this ticket',CURRENT_TIMESTAMP);
            INSERT INTO support_messages(id,ticket_id,kind,body)
            VALUES (2001,1001,'REQUESTER','Keep this conversation');
            INSERT INTO support_email_deliveries(message_id,recipient,status)
            VALUES (2001,'team@support.test','DISABLED');
            """);
      }

      // This regression specifically verifies the V20 -> V21 corrective migration.
      Flyway latest =
          Flyway.configure().dataSource(url, user, password).schemas(schema).target("21").load();
      assertEquals(1, latest.migrate().migrationsExecuted);
      latest.validate();
      assertEquals(201, columnLength(connection));
      try (var statement = connection.createStatement();
          var rows =
              statement.executeQuery(
                  """
              SELECT t.subject,m.body,e.status FROM support_tickets t
              JOIN support_messages m ON m.ticket_id=t.id
              JOIN support_email_deliveries e ON e.message_id=m.id WHERE t.id=1001
              """)) {
        assertTrue(rows.next());
        assertEquals("Keep this ticket", rows.getString(1));
        assertEquals("Keep this conversation", rows.getString(2));
        assertEquals("DISABLED", rows.getString(3));
        assertFalse(rows.next());
      }
      try (var statement =
          connection.prepareStatement("UPDATE support_tickets SET contact_name=? WHERE id=1001")) {
        statement.setString(1, "A".repeat(100) + " " + "B".repeat(100));
        assertEquals(1, statement.executeUpdate());
      }
      try (var statement = connection.createStatement();
          var rows =
              statement.executeQuery(
                  "SELECT checksum FROM flyway_schema_history WHERE version='20'")) {
        assertTrue(rows.next());
        assertEquals(-301408915, rows.getInt(1));
      }
    }
  }

  private int columnLength(Connection connection) throws Exception {
    try (var statement = connection.createStatement();
        var rows =
            statement.executeQuery(
                """
            SELECT character_maximum_length FROM information_schema.columns
            WHERE table_schema=current_schema() AND table_name='support_tickets' AND column_name='contact_name'
            """)) {
      assertTrue(rows.next());
      return rows.getInt(1);
    }
  }
}
