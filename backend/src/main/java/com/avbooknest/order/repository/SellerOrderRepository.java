package com.avbooknest.order.repository;

import com.avbooknest.order.model.OrderIssueStatus;
import com.avbooknest.order.model.SellerOrder;
import com.avbooknest.order.model.SellerOrderStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SellerOrderRepository extends JpaRepository<SellerOrder, Long> {

  @EntityGraph(attributePaths = {"order", "seller", "items", "items.seller", "shipment"})
  @Query(
      """
      select distinct so from SellerOrder so
      left join so.items item
      where so.seller.id = :sellerId
      and (:status is null or so.status = :status)
      and (:query = ''
        or lower(so.order.orderNumber) like concat('%', :query, '%')
        or lower(item.title) like concat('%', :query, '%'))
      order by so.createdAt desc
      """)
  List<SellerOrder> searchForSeller(
      @Param("sellerId") Long sellerId,
      @Param("status") SellerOrderStatus status,
      @Param("query") String query);

  @EntityGraph(attributePaths = {"order", "seller", "items", "items.seller", "shipment"})
  List<SellerOrder> findAllByOrderId(Long orderId);

  @EntityGraph(attributePaths = {"order", "order.buyer", "seller", "issueResolvedBy"})
  @Query(
      """
      select distinct so from SellerOrder so
      where (:status is null or so.issueStatus = :status)
      """)
  Page<SellerOrder> searchIssuesForAdmin(
      @Param("status") OrderIssueStatus status, Pageable pageable);

  long countBySellerId(Long sellerId);

  long countByIssueStatus(OrderIssueStatus status);

  @Query(
      "select coalesce(sum(so.commissionAmount), 0) from SellerOrder so where so.status <> com.avbooknest.order.model.SellerOrderStatus.CANCELLED")
  java.math.BigDecimal sumActiveCommission();

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
  @Query(
      """
      select so from SellerOrder so
      join fetch so.order o
      join fetch o.buyer
      join fetch so.seller
      left join fetch so.shipment
      where so.id = :id and o.id = :orderId and o.buyer.id = :buyerId
      """)
  Optional<SellerOrder> findForBuyerIssue(
      @Param("id") Long id, @Param("orderId") Long orderId, @Param("buyerId") Long buyerId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select so from SellerOrder so
      join fetch so.order
      join fetch so.seller
      left join fetch so.shipment
      where so.id = :id
      """)
  Optional<SellerOrder> findByIdForUpdate(@Param("id") Long id);

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

  @Query(
      """
      select so.id from SellerOrder so
      join so.shipment shipment
      where so.status = com.avbooknest.order.model.SellerOrderStatus.ACCEPTED
      and so.dropoffBy <= :now
      and shipment.status in (
        com.avbooknest.shipment.model.ShipmentStatus.NOT_CREATED,
        com.avbooknest.shipment.model.ShipmentStatus.AWB_PENDING,
        com.avbooknest.shipment.model.ShipmentStatus.AWB_CREATED,
        com.avbooknest.shipment.model.ShipmentStatus.AWAITING_DROPOFF
      )
      """)
  List<Long> findExpiredDropoffIds(@Param("now") Instant now);
}
