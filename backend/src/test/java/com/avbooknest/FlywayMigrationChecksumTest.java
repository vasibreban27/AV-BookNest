package com.avbooknest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Test;

class FlywayMigrationChecksumTest {
  @Test
  void alreadyAppliedV17MigrationKeepsItsChecksum() throws Exception {
    assertEquals(-644462390, checksum("/db/migration/V17__add_admin_operations.sql"));
  }

  @Test
  void alreadyAppliedV20MigrationKeepsItsChecksum() throws Exception {
    assertEquals(-301408915, checksum("/db/migration/V20__create_support_tickets.sql"));
  }

  private int checksum(String resource) throws Exception {
    CRC32 checksum = new CRC32();
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(
                getClass().getResourceAsStream(resource), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        checksum.update(line.getBytes(StandardCharsets.UTF_8));
      }
    }

    return (int) checksum.getValue();
  }
}
