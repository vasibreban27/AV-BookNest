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
import com.avbooknest.book.storage.BookCoverStorage;
import com.avbooknest.book.storage.StoredBookCover;
import com.avbooknest.common.exception.ConflictException;
import com.avbooknest.common.exception.ForbiddenException;
import com.avbooknest.common.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class BookService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BookService.class);
  private final BookRepository bookRepository;
  private final CategoryRepository categoryRepository;
  private final UserRepository userRepository;
  private final BookCoverStorage bookCoverStorage;

  public BookService(
      BookRepository bookRepository,
      CategoryRepository categoryRepository,
      UserRepository userRepository,
      BookCoverStorage bookCoverStorage) {
    this.bookRepository = bookRepository;
    this.categoryRepository = categoryRepository;
    this.userRepository = userRepository;
    this.bookCoverStorage = bookCoverStorage;
  }

  @Transactional(readOnly = true)
  public List<BookResponse> list() {
    return bookRepository.findAllByStatusOrderByCreatedAtDesc(BookStatus.AVAILABLE).stream()
        .map(BookResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<BookResponse> listMine(String email) {
    return bookRepository.findAllBySellerIdOrderByCreatedAtDesc(currentUser(email).getId()).stream()
        .map(BookResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public BookResponse get(Long bookId, String email) {
    Book book = findBook(bookId);
    if (book.getStatus() != BookStatus.AVAILABLE && !isOwner(book, currentUser(email))) {
      throw new NotFoundException("Book not found");
    }
    return BookResponse.from(book);
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
            .weightGrams(request.weightGrams())
            .lengthMm(request.lengthMm())
            .widthMm(request.widthMm())
            .heightMm(request.heightMm())
            .seller(currentUser(email))
            .category(findCategory(request.categoryId()))
            .status(BookStatus.AVAILABLE)
            .createdAt(now)
            .updatedAt(now)
            .build();
    return BookResponse.from(bookRepository.save(book));
  }

  public BookResponse update(Long bookId, BookRequest request, String email) {
    Book book = findBookForUpdate(bookId);
    requireOwner(book, currentUser(email));
    requireEditable(book);
    book.updateDetails(
        request.title().trim(),
        request.author().trim(),
        trimToNull(request.isbn()),
        trimToNull(request.description()),
        request.price(),
        request.bookCondition(),
        request.language().trim(),
        trimToNull(request.publisher()),
        request.publishedYear(),
        request.weightGrams(),
        request.lengthMm(),
        request.widthMm(),
        request.heightMm(),
        findCategory(request.categoryId()));
    return BookResponse.from(book);
  }

  public BookResponse updateCover(Long bookId, MultipartFile cover, String email) {
    Book book = findBookForUpdate(bookId);
    requireOwner(book, currentUser(email));
    requireEditable(book);
    StoredBookCover storedCover = bookCoverStorage.upload(cover);
    String previousPublicId = book.getCoverImagePublicId();
    book.updateCover(storedCover.secureUrl(), storedCover.publicId());
    try {
      bookRepository.saveAndFlush(book);
    } catch (RuntimeException exception) {
      deleteCoverQuietly(storedCover.publicId());
      throw exception;
    }
    deleteCoverQuietly(previousPublicId);
    return BookResponse.from(book);
  }

  public BookResponse removeCover(Long bookId, String email) {
    Book book = findBookForUpdate(bookId);
    requireOwner(book, currentUser(email));
    requireEditable(book);
    String previousPublicId = book.getCoverImagePublicId();
    book.removeCover();
    bookRepository.saveAndFlush(book);
    deleteCoverQuietly(previousPublicId);
    return BookResponse.from(book);
  }

  public BookResponse archive(Long bookId, String email) {
    Book book = findBookForUpdate(bookId);
    requireOwner(book, currentUser(email));
    if (book.getStatus() != BookStatus.AVAILABLE) {
      throw new ConflictException("Only available books can be archived");
    }
    book.archive();
    return BookResponse.from(book);
  }

  public BookResponse publish(Long bookId, String email) {
    Book book = findBookForUpdate(bookId);
    requireOwner(book, currentUser(email));
    if (book.getStatus() != BookStatus.ARCHIVED && book.getStatus() != BookStatus.DRAFT) {
      throw new ConflictException("Only draft or archived books can be published");
    }
    book.publish();
    return BookResponse.from(book);
  }

  public void delete(Long bookId, String email) {
    Book book = findBookForUpdate(bookId);
    requireOwner(book, currentUser(email));
    if (book.getStatus() != BookStatus.DRAFT && book.getStatus() != BookStatus.ARCHIVED) {
      throw new ConflictException("Only draft or archived books can be deleted");
    }
    String publicId = book.getCoverImagePublicId();
    bookRepository.delete(book);
    bookRepository.flush();
    deleteCoverQuietly(publicId);
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

  private void requireOwner(Book book, User user) {
    if (!isOwner(book, user)) {
      throw new ForbiddenException("Only the seller can modify this book");
    }
  }

  private Book findBookForUpdate(Long bookId) {
    return bookRepository
        .findByIdForUpdate(bookId)
        .orElseThrow(() -> new NotFoundException("Book not found"));
  }

  private boolean isOwner(Book book, User user) {
    return book.getSeller().getId().equals(user.getId());
  }

  private void requireEditable(Book book) {
    if (book.getStatus() == BookStatus.RESERVED || book.getStatus() == BookStatus.SOLD) {
      throw new ConflictException("Reserved or sold books cannot be modified");
    }
  }

  private void deleteCoverQuietly(String publicId) {
    if (publicId == null || publicId.isBlank()) return;
    try {
      bookCoverStorage.delete(publicId);
    } catch (RuntimeException exception) {
      LOGGER.warn("Could not delete Cloudinary cover {}", publicId, exception);
    }
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
