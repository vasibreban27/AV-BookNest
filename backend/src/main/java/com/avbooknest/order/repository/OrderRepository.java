package com.avbooknest.order.repository;

import com.avbooknest.order.model.Order;
import com.avbooknest.order.model.OrderStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {
  @EntityGraph(attributePaths = {"items", "items.seller"})
  @Query(
      """
      select distinct o from Order o
      left join o.items item
      where o.buyer.id = :buyerId
      and (:status is null or o.status = :status)
      and (:query = ''
        or lower(o.orderNumber) like concat('%', :query, '%')
        or lower(item.title) like concat('%', :query, '%'))
      order by o.placedAt desc
      """)
  List<Order> searchForBuyer(
      @Param("buyerId") Long buyerId,
      @Param("status") OrderStatus status,
      @Param("query") String query);

  @EntityGraph(attributePaths = {"items", "items.seller"})
  Optional<Order> findByIdAndBuyerId(Long id, Long buyerId);

  @EntityGraph(attributePaths = {"buyer"})
  @Query(
      """
      select o from Order o
      where (:query = ''
        or lower(o.orderNumber) like concat('%', :query, '%')
        or lower(o.buyer.email) like concat('%', :query, '%'))
      and (:status is null or o.status = :status)
      """)
  Page<Order> searchForAdmin(
      @Param("query") String query, @Param("status") OrderStatus status, Pageable pageable);

  @EntityGraph(attributePaths = {"buyer", "items", "items.seller"})
  @Query("select distinct o from Order o where o.id = :id")
  Optional<Order> findDetailedById(@Param("id") Long id);

  long countByBuyerId(Long buyerId);

  long countByStatusIn(List<OrderStatus> statuses);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from Order o where o.id = :id")
  Optional<Order> findByIdForUpdate(@Param("id") Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from Order o where o.id = :id and o.buyer.id = :buyerId")
  Optional<Order> findByIdAndBuyerIdForUpdate(@Param("id") Long id, @Param("buyerId") Long buyerId);
}
