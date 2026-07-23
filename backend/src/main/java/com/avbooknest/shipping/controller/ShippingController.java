package com.avbooknest.shipping.controller;

import com.avbooknest.shipping.dto.EasyboxResponse;
import com.avbooknest.shipping.sameday.SamedayClient;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shipping")
public class ShippingController {
  private final SamedayClient samedayClient;

  public ShippingController(SamedayClient samedayClient) {
    this.samedayClient = samedayClient;
  }

  @GetMapping("/easyboxes")
  public List<EasyboxResponse> easyboxes(
      @RequestParam(name = "query", required = false) String query) {
    return samedayClient.lockers(query);
  }
}
