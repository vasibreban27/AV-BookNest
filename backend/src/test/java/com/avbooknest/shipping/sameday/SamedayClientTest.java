package com.avbooknest.shipping.sameday;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.avbooknest.shipping.dto.EasyboxResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class SamedayClientTest {

  private final SamedayClient client =
      new SamedayClient(
          new SamedayProperties(true, "https://sameday.invalid", "", "", null, null, true),
          RestClient.builder());

  @Test
  void mockLockerSearchIgnoresDiacriticsAndMatchesAllTerms() {
    List<EasyboxResponse> lockers = client.lockers("Cluj Marasti");

    assertEquals(1, lockers.size());
    assertEquals("TEST-CLJ-002", lockers.getFirst().id());
  }
}
