package com.avbooknest.book.service;

import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.book.dto.CategoryResponse;
import com.avbooknest.book.dto.CreateCategoryRequest;
import com.avbooknest.book.model.Category;
import com.avbooknest.book.repository.CategoryRepository;
import com.avbooknest.common.exception.ConflictException;
import com.avbooknest.common.exception.ForbiddenException;
import com.avbooknest.common.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CategoryService {
  private final CategoryRepository categoryRepository;
  private final UserRepository userRepository;

  public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository) {
    this.categoryRepository = categoryRepository;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<CategoryResponse> list() {
    return categoryRepository.findAll(Sort.by("name")).stream()
        .map(CategoryResponse::from)
        .toList();
  }

  public CategoryResponse create(CreateCategoryRequest request, String email) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException("User not found"));
    if (!"ADMIN".equals(user.getRole().getName())) {
      throw new ForbiddenException("Only administrators can create categories");
    }
    String slug = request.slug().trim();
    if (categoryRepository.existsByNameIgnoreCase(request.name().trim())
        || categoryRepository.findBySlug(slug).isPresent()) {
      throw new ConflictException("A category with this name or slug already exists");
    }
    Instant now = Instant.now();
    Category category =
        Category.builder()
            .name(request.name().trim())
            .slug(slug)
            .description(
                request.description() == null || request.description().isBlank()
                    ? null
                    : request.description().trim())
            .createdAt(now)
            .updatedAt(now)
            .build();
    return CategoryResponse.from(categoryRepository.save(category));
  }
}
