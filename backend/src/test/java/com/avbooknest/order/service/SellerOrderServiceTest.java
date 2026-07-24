package com.avbooknest.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avbooknest.auth.model.Role;
import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.integration.model.IntegrationEvent;
import com.avbooknest.integration.repository.IntegrationEventRepository;
import com.avbooknest.notification.service.NotificationService;
import com.avbooknest.order.model.Order;
import com.avbooknest.order.model.OrderStatus;
import com.avbooknest.order.model.SellerOrder;
import com.avbooknest.order.model.SellerOrderStatus;
import com.avbooknest.order.repository.SellerOrderRepository;
import com.avbooknest.shipment.model.PackageSize;
import com.avbooknest.shipment.model.Shipment;
import com.avbooknest.shipment.model.ShipmentStatus;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SellerOrderServiceTest {
  @Mock private SellerOrderRepository sellerOrderRepository;
  @Mock private UserRepository userRepository;
  @Mock private IntegrationEventRepository integrationEventRepository;
  @Mock private NotificationService notificationService;

  @Test
  void acceptanceQueuesSamedayAwbAndStartsFortyEightHourDropoffWindow() {
    Instant now = Instant.now();
    User seller = user(2L, "seller@example.com");
    User buyer = user(1L, "buyer@example.com");
    Order order =
        Order.builder()
            .id(10L)
            .orderNumber("ORD-10")
            .buyer(buyer)
            .status(OrderStatus.PENDING)
            .subtotal(new BigDecimal("100.00"))
            .shippingCost(BigDecimal.ZERO)
            .totalAmount(new BigDecimal("100.00"))
            .currency("RON")
            .recipientName("Buyer User")
            .recipientEmail(buyer.getEmail())
            .recipientPhone("+40700111222")
            .placedAt(now)
            .updatedAt(now)
            .build();
    SellerOrder sellerOrder =
        SellerOrder.builder()
            .id(20L)
            .order(order)
            .seller(seller)
            .status(SellerOrderStatus.AWAITING_SELLER)
            .itemSubtotal(new BigDecimal("100.00"))
            .commissionRate(new BigDecimal("5.00"))
            .commissionAmount(new BigDecimal("5.00"))
            .sellerProceeds(new BigDecimal("95.00"))
            .shippingCost(BigDecimal.ZERO)
            .acceptBy(now.plus(Duration.ofHours(24)))
            .createdAt(now)
            .updatedAt(now)
            .build();
    Shipment shipment =
        Shipment.builder()
            .id(30L)
            .sellerOrder(sellerOrder)
            .easyboxId("locker-1")
            .easyboxName("Easybox Central")
            .packageSize(PackageSize.M)
            .packageWeightGrams(650)
            .packageLengthMm(230)
            .packageWidthMm(160)
            .packageHeightMm(50)
            .status(ShipmentStatus.NOT_CREATED)
            .createdAt(now)
            .updatedAt(now)
            .build();
    sellerOrder.assignShipment(shipment);
    order.addSellerOrder(sellerOrder);
    when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
    when(sellerOrderRepository.findByIdAndSellerIdForUpdate(20L, 2L))
        .thenReturn(Optional.of(sellerOrder));
    when(sellerOrderRepository.findAllByOrderIdForUpdate(10L)).thenReturn(List.of(sellerOrder));
    when(integrationEventRepository.save(any(IntegrationEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    SellerOrderService service =
        new SellerOrderService(
            sellerOrderRepository, userRepository, integrationEventRepository, notificationService);

    service.accept(20L, seller.getEmail());

    assertEquals(SellerOrderStatus.ACCEPTED, sellerOrder.getStatus());
    assertEquals(ShipmentStatus.AWB_PENDING, shipment.getStatus());
    assertEquals(PackageSize.M, shipment.getPackageSize());
    assertNotNull(sellerOrder.getDropoffBy());
    assertEquals(
        Duration.ofHours(48),
        Duration.between(sellerOrder.getAcceptedAt(), sellerOrder.getDropoffBy()));
    verify(integrationEventRepository).save(any(IntegrationEvent.class));
  }

  private User user(Long id, String email) {
    Instant now = Instant.now();
    return User.builder()
        .id(id)
        .firstName("Test")
        .lastName("User")
        .email(email)
        .passwordHash("password")
        .role(Role.builder().id(1L).name("USER").build())
        .enabled(true)
        .emailVerified(true)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }
}
