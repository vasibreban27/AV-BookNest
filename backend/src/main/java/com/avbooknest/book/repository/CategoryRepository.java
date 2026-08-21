package com.avbooknest.book.repository;

import com.avbooknest.book.model.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
  Optional<Category> findBySlug(String slug);

  Optional<Category> findByIdAndActiveTrue(Long id);

  List<Category> findAllByActiveTrueOrderByNameAsc();

  boolean existsByNameIgnoreCase(String name);

  boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
