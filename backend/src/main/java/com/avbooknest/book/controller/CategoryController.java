package com.avbooknest.book.controller;

import com.avbooknest.book.dto.CategoryResponse;
import com.avbooknest.book.dto.CreateCategoryRequest;
import com.avbooknest.book.service.CategoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
  private final CategoryService categoryService;

  public CategoryController(CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  @GetMapping
  public List<CategoryResponse> list() {
    return categoryService.list();
  }

  @PostMapping
  public ResponseEntity<CategoryResponse> create(
      @Valid @RequestBody CreateCategoryRequest request, Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(categoryService.create(request, authentication.getName()));
  }
}
