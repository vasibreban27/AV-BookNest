package com.avbooknest.shipment.controller;

import com.avbooknest.shipment.dto.ShipmentResponse;
import com.avbooknest.shipment.dto.TrackingNumberRequest;
import com.avbooknest.shipment.service.ShipmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

  private final ShipmentService shipmentService;

  public ShipmentController(ShipmentService shipmentService) {
    this.shipmentService = shipmentService;
  }

  @GetMapping("/mine")
  public List<ShipmentResponse> mine(Authentication authentication) {
    return shipmentService.listForSeller(authentication.getName());
  }

  @PatchMapping("/{shipmentId}/tracking-number")
  public ShipmentResponse registerTrackingNumber(
      @PathVariable Long shipmentId,
      @Valid @RequestBody TrackingNumberRequest request,
      Authentication authentication) {
    return shipmentService.registerTrackingNumber(
        shipmentId, request.trackingNumber(), authentication.getName());
  }
}
