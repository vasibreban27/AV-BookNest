package com.avbooknest.book.service;

import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.book.dto.BookRequest;
import com.avbooknest.book.dto.BookResponse;
import com.avbooknest.book.model.Book;
import com.avbooknest.book.model.BookStatus;
import com.avbooknest.book.model.Category;
import com.avbooknest.book.repository.BookRepository;
import com.avbooknest.book.repository.CategoryRepository;
import com.avbooknest.common.exception.ForbiddenException;
import com.avbooknest.common.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BookService {
  private final BookRepository bookRepository;
  private final CategoryRepository categoryRepository;
  private final UserRepository userRepository;

  public BookService(
      BookRepository bookRepository,
      CategoryRepository categoryRepository,
      UserRepository userRepository) {
    this.bookRepository = bookRepository;
    this.categoryRepository = categoryRepository;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<BookResponse> list() {
    return bookRepository.findAll().stream().map(BookResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public List<BookResponse> listMine(String email) {
    return bookRepository.findAllBySellerIdOrderByCreatedAtDesc(currentUser(email).getId()).stream()
        .map(BookResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public BookResponse get(Long bookId) {
    return BookResponse.from(findBook(bookId));
  }

  public BookResponse create(BookRequest request, String email) {
    Instant now = Instant.now();
    Book book =
        Book.builder()
            .title(request.title().trim())
            .author(request.author().trim())
            .isbn(trimToNull(request.isbn()))
            .description(trimToNull(request.description()))
            .price(request.price())
            .bookCondition(request.bookCondition())
            .language(request.language().trim())
            .publisher(trimToNull(request.publisher()))
            .publishedYear(request.publishedYear())
            .coverImageUrl(trimToNull(request.coverImageUrl()))
            .seller(currentUser(email))
            .category(findCategory(request.categoryId()))
            .status(request.status() == null ? BookStatus.AVAILABLE : request.status())
            .createdAt(now)
            .updatedAt(now)
            .build();
    return BookResponse.from(bookRepository.save(book));
  }

  public BookResponse update(Long bookId, BookRequest request, String email) {
    Book book = findBook(bookId);
    requireOwner(book, email);
    book.update(
        request.title().trim(),
        request.author().trim(),
        trimToNull(request.isbn()),
        trimToNull(request.description()),
        request.price(),
        request.bookCondition(),
        request.language().trim(),
        trimToNull(request.publisher()),
        request.publishedYear(),
        trimToNull(request.coverImageUrl()),
        findCategory(request.categoryId()),
        request.status() == null ? book.getStatus() : request.status());
    return BookResponse.from(book);
  }

  public void delete(Long bookId, String email) {
    Book book = findBook(bookId);
    requireOwner(book, email);
    bookRepository.delete(book);
  }

  private Book findBook(Long bookId) {
    return bookRepository
        .findById(bookId)
        .orElseThrow(() -> new NotFoundException("Book not found"));
  }

  private Category findCategory(Long categoryId) {
    return categoryRepository
        .findById(categoryId)
        .orElseThrow(() -> new NotFoundException("Category not found"));
  }

  private User currentUser(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new NotFoundException("User not found"));
  }

  private void requireOwner(Book book, String email) {
    if (!book.getSeller().getId().equals(currentUser(email).getId())) {
      throw new ForbiddenException("Only the seller can modify this book");
    }
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
