package com.avbooknest.book.repository;

import com.avbooknest.book.model.Book;
import com.avbooknest.book.model.BookStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {
  @EntityGraph(attributePaths = {"seller", "category"})
  List<Book> findAllByStatusOrderByCreatedAtDesc(BookStatus status);

  @EntityGraph(attributePaths = {"seller", "category"})
  List<Book> findAllBySellerIdOrderByCreatedAtDesc(Long sellerId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select book from Book book where book.id = :bookId")
  java.util.Optional<Book> findByIdForUpdate(@Param("bookId") Long bookId);
}
