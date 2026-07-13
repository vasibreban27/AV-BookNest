package com.avbooknest.book.repository;

import com.avbooknest.book.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findAllBySellerIdOrderByCreatedAtDesc(Long sellerId);
}
