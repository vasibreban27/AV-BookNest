package com.avbooknest.review.repository;

import com.avbooknest.review.dto.ReviewStats;
import com.avbooknest.review.model.Review;
import com.avbooknest.review.model.ReviewModerationStatus;
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

public interface ReviewRepository extends JpaRepository<Review, Long> {
  boolean existsByOrderItemId(Long orderItemId);

  @EntityGraph(attributePaths = {"reviewer", "seller", "book", "orderItem"})
  List<Review> findAllByOrderItemOrderId(Long orderId);

  @EntityGraph(attributePaths = {"reviewer", "seller", "book", "orderItem"})
  @Query(
      "select r from Review r where r.seller.id = :sellerId and r.orderItem is not null and r.moderationStatus = com.avbooknest.review.model.ReviewModerationStatus.VISIBLE")
  Page<Review> publicReviews(@Param("sellerId") Long sellerId, Pageable pageable);

  @Query(
      """
      select new com.avbooknest.review.dto.ReviewStats(count(r), avg(r.sellerRating), avg(r.descriptionRating), avg(r.conditionRating))
      from Review r where r.seller.id = :sellerId and r.orderItem is not null
      and r.moderationStatus = com.avbooknest.review.model.ReviewModerationStatus.VISIBLE
      """)
  ReviewStats reputation(@Param("sellerId") Long sellerId);

  @EntityGraph(attributePaths = {"reviewer", "seller", "book", "orderItem"})
  @Query(
      """
      select r from Review r where (:status is null or r.moderationStatus = :status)
      and (:query = '' or lower(r.bookTitle) like concat('%', :query, '%')
        or lower(r.reviewer.firstName) like concat('%', :query, '%')
        or lower(r.reviewer.lastName) like concat('%', :query, '%')
        or lower(r.seller.firstName) like concat('%', :query, '%')
        or lower(r.seller.lastName) like concat('%', :query, '%'))
      """)
  Page<Review> searchForAdmin(
      @Param("status") ReviewModerationStatus status,
      @Param("query") String query,
      Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Review r where r.id = :id")
  Optional<Review> findByIdForUpdate(@Param("id") Long id);
}
