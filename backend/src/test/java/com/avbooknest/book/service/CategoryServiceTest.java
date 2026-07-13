package com.avbooknest.book.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.avbooknest.auth.model.Role;
import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.book.dto.CategoryResponse;
import com.avbooknest.book.dto.CreateCategoryRequest;
import com.avbooknest.book.model.Category;
import com.avbooknest.book.repository.CategoryRepository;
import com.avbooknest.common.exception.ForbiddenException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
  @Mock private CategoryRepository categoryRepository;
  @Mock private UserRepository userRepository;
  private CategoryService categoryService;

  @BeforeEach
  void setUp() {
    categoryService = new CategoryService(categoryRepository, userRepository);
  }

  @Test
  void adminCanCreateCategory() {
    when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user("ADMIN")));
    when(categoryRepository.existsByNameIgnoreCase("Technology")).thenReturn(false);
    when(categoryRepository.findBySlug("technology")).thenReturn(Optional.empty());
    when(categoryRepository.save(any(Category.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CategoryResponse response =
        categoryService.create(
            new CreateCategoryRequest(" Technology ", "technology", " Books about technology "),
            "admin@example.com");

    assertEquals("Technology", response.name());
    assertEquals("technology", response.slug());
  }

  @Test
  void regularUserCannotCreateCategory() {
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user("USER")));

    assertThrows(
        ForbiddenException.class,
        () ->
            categoryService.create(
                new CreateCategoryRequest("Technology", "technology", null), "user@example.com"));
  }

  private User user(String roleName) {
    Instant now = Instant.now();
    return User.builder()
        .id(1L)
        .firstName("Test")
        .lastName("User")
        .email("user@example.com")
        .passwordHash("password")
        .role(Role.builder().id(1L).name(roleName).build())
        .enabled(true)
        .emailVerified(true)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }
}
