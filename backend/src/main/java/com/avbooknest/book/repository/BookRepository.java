package com.avbooknest.book.repository;

import com.avbooknest.book.model.Book;
import com.avbooknest.book.model.BookCondition;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {
  @EntityGraph(attributePaths = {"seller", "category"})
  @Query(
      """
      select book from Book book
      where book.status = com.avbooknest.book.model.BookStatus.AVAILABLE
      and (:query = ''
        or lower(book.title) like concat('%', :query, '%')
        or lower(book.author) like concat('%', :query, '%')
        or lower(coalesce(book.isbn, '')) like concat('%', :query, '%'))
      and (:categorySlug is null or book.category.slug = :categorySlug)
      and (:condition is null or book.bookCondition = :condition)
      and (:minimumPrice is null or book.price >= :minimumPrice)
      and (:maximumPrice is null or book.price <= :maximumPrice)
      """)
  Page<Book> searchAvailable(
      @Param("query") String query,
      @Param("categorySlug") String categorySlug,
      @Param("condition") BookCondition condition,
      @Param("minimumPrice") BigDecimal minimumPrice,
      @Param("maximumPrice") BigDecimal maximumPrice,
      Pageable pageable);

  @EntityGraph(attributePaths = {"seller", "category"})
  List<Book> findAllBySellerIdOrderByCreatedAtDesc(Long sellerId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select book from Book book where book.id = :bookId")
  java.util.Optional<Book> findByIdForUpdate(@Param("bookId") Long bookId);
}
