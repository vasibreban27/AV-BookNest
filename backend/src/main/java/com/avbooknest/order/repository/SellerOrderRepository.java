package com.avbooknest.order.repository;

import com.avbooknest.order.model.SellerOrder;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SellerOrderRepository extends JpaRepository<SellerOrder, Long> {

  @EntityGraph(attributePaths = {"order", "seller", "items", "items.seller", "shipment"})
  List<SellerOrder> findAllBySellerIdOrderByCreatedAtDesc(Long sellerId);

  @EntityGraph(attributePaths = {"order", "seller", "items", "items.seller", "shipment"})
  List<SellerOrder> findAllByOrderId(Long orderId);

  @EntityGraph(attributePaths = {"order", "order.buyer", "seller", "items", "shipment"})
  @Query("select so from SellerOrder so where so.id = :id")
  Optional<SellerOrder> findDetailedById(@Param("id") Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select so from SellerOrder so
      join fetch so.order
      join fetch so.seller
      left join fetch so.shipment
      where so.id = :id and so.seller.id = :sellerId
      """)
  Optional<SellerOrder> findByIdAndSellerIdForUpdate(
      @Param("id") Long id, @Param("sellerId") Long sellerId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select so from SellerOrder so where so.order.id = :orderId")
  List<SellerOrder> findAllByOrderIdForUpdate(@Param("orderId") Long orderId);

  @Query(
      """
      select so.id from SellerOrder so
      where so.status = com.avbooknest.order.model.SellerOrderStatus.AWAITING_SELLER
      and so.acceptBy <= :now
      """)
  List<Long> findExpiredAcceptanceIds(@Param("now") Instant now);
}
