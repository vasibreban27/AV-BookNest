package com.avbooknest.book.controller;

import com.avbooknest.book.dto.BookRequest;
import com.avbooknest.book.dto.BookResponse;
import com.avbooknest.book.dto.CatalogCategoryResponse;
import com.avbooknest.book.model.BookCondition;
import com.avbooknest.book.service.BookService;
import com.avbooknest.common.dto.PageResponse;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/books")
public class BookController {
  private final BookService bookService;

  public BookController(BookService bookService) {
    this.bookService = bookService;
  }

  @GetMapping
  public PageResponse<BookResponse> list(
      @RequestParam(required = false, name = "q") String query,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) BookCondition condition,
      @RequestParam(required = false) BigDecimal minPrice,
      @RequestParam(required = false) BigDecimal maxPrice,
      @RequestParam(required = false) String language,
      @RequestParam(required = false) Integer minYear,
      @RequestParam(required = false) Integer maxYear,
      @RequestParam(defaultValue = "newest") String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size) {
    return bookService.list(
        query, category, condition, minPrice, maxPrice, language, minYear, maxYear, sort, page,
        size);
  }

  @GetMapping("/languages")
  public List<String> languages() {
    return bookService.listAvailableLanguages();
  }

  @GetMapping("/catalog-categories")
  public List<CatalogCategoryResponse> catalogCategories() {
    return bookService.listAvailableCategories();
  }

  @GetMapping("/mine")
  public List<BookResponse> mine(Authentication authentication) {
    return bookService.listMine(authentication.getName());
  }

  @GetMapping("/{bookId}")
  public BookResponse get(@PathVariable Long bookId, Authentication authentication) {
    return bookService.get(bookId, authentication == null ? null : authentication.getName());
  }

  @PostMapping(path = "/{bookId}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public BookResponse updateCover(
      @PathVariable Long bookId,
      @RequestParam("file") MultipartFile file,
      Authentication authentication) {
    return bookService.updateCover(bookId, file, authentication.getName());
  }

  @DeleteMapping("/{bookId}/cover")
  public BookResponse removeCover(@PathVariable Long bookId, Authentication authentication) {
    return bookService.removeCover(bookId, authentication.getName());
  }

  @PatchMapping("/{bookId}/archive")
  public BookResponse archive(@PathVariable Long bookId, Authentication authentication) {
    return bookService.archive(bookId, authentication.getName());
  }

  @PatchMapping("/{bookId}/publish")
  public BookResponse publish(@PathVariable Long bookId, Authentication authentication) {
    return bookService.publish(bookId, authentication.getName());
  }

  @PostMapping
  public ResponseEntity<BookResponse> create(
      @Valid @RequestBody BookRequest request, Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(bookService.create(request, authentication.getName()));
  }

  @PutMapping("/{bookId}")
  public BookResponse update(
      @PathVariable Long bookId,
      @Valid @RequestBody BookRequest request,
      Authentication authentication) {
    return bookService.update(bookId, request, authentication.getName());
  }

  @DeleteMapping("/{bookId}")
  public ResponseEntity<Void> delete(@PathVariable Long bookId, Authentication authentication) {
    bookService.delete(bookId, authentication.getName());
    return ResponseEntity.noContent().build();
  }
}
