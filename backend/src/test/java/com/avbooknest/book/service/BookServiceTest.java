package com.avbooknest.book.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avbooknest.auth.model.Role;
import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.book.dto.BookRequest;
import com.avbooknest.book.dto.BookResponse;
import com.avbooknest.book.model.Book;
import com.avbooknest.book.model.BookCondition;
import com.avbooknest.book.model.BookStatus;
import com.avbooknest.book.model.Category;
import com.avbooknest.book.repository.BookRepository;
import com.avbooknest.book.repository.CategoryRepository;
import com.avbooknest.book.storage.BookCoverStorage;
import com.avbooknest.book.storage.StoredBookCover;
import com.avbooknest.common.exception.ConflictException;
import com.avbooknest.common.exception.ForbiddenException;
import com.avbooknest.common.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {
  @Mock private BookRepository bookRepository;
  @Mock private CategoryRepository categoryRepository;
  @Mock private UserRepository userRepository;
  @Mock private BookCoverStorage bookCoverStorage;
  private BookService bookService;

  @BeforeEach
  void setUp() {
    bookService =
        new BookService(bookRepository, categoryRepository, userRepository, bookCoverStorage);
  }

  @Test
  void listReturnsOnlyAvailableBooks() {
    User seller = user(10L, "seller@example.com");
    when(bookRepository.findAllByStatusOrderByCreatedAtDesc(BookStatus.AVAILABLE))
        .thenReturn(List.of(book(1L, seller, BookStatus.AVAILABLE)));

    List<BookResponse> response = bookService.list();

    assertEquals(1, response.size());
    assertEquals(BookStatus.AVAILABLE, response.getFirst().status());
  }

  @Test
  void createUsesAuthenticatedUserCategoryAndAvailableStatus() {
    User seller = user(10L, "seller@example.com");
    Category category = category(4L);
    when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(seller));
    when(categoryRepository.findById(4L)).thenReturn(Optional.of(category));
    when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

    BookResponse response = bookService.create(request(), "seller@example.com");

    ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
    verify(bookRepository).save(bookCaptor.capture());
    Book saved = bookCaptor.getValue();
    assertEquals("Clean Code", saved.getTitle());
    assertEquals(seller, saved.getSeller());
    assertEquals(category, saved.getCategory());
    assertEquals(BookStatus.AVAILABLE, saved.getStatus());
    assertNull(saved.getCoverImageUrl());
    assertEquals(new BigDecimal("45.50"), response.price());
  }

  @Test
  void getHidesArchivedBookFromAnotherUser() {
    User owner = user(2L, "owner@example.com");
    User viewer = user(3L, "viewer@example.com");
    when(bookRepository.findById(11L))
        .thenReturn(Optional.of(book(11L, owner, BookStatus.ARCHIVED)));
    when(userRepository.findByEmail("viewer@example.com")).thenReturn(Optional.of(viewer));

    assertThrows(NotFoundException.class, () -> bookService.get(11L, "viewer@example.com"));
  }

  @Test
  void updateRejectsAUserWhoDoesNotOwnTheBook() {
    User owner = user(2L, "owner@example.com");
    User anotherUser = user(3L, "other@example.com");
    Book book = book(11L, owner, BookStatus.AVAILABLE);
    when(bookRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(book));
    when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(anotherUser));

    assertThrows(
        ForbiddenException.class, () -> bookService.update(11L, request(), "other@example.com"));
  }

  @Test
  void updateRejectsReservedBook() {
    User owner = user(2L, "owner@example.com");
    when(bookRepository.findByIdForUpdate(11L))
        .thenReturn(Optional.of(book(11L, owner, BookStatus.RESERVED)));
    when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));

    assertThrows(
        ConflictException.class, () -> bookService.update(11L, request(), "owner@example.com"));
    verify(categoryRepository, never()).findById(any());
  }

  @Test
  void updateCoverStoresNewImageAndDeletesPreviousCloudinaryAsset() {
    User owner = user(2L, "owner@example.com");
    Book book =
        bookBuilder(11L, owner, BookStatus.AVAILABLE)
            .coverImageUrl("https://old.example/cover.jpg")
            .coverImagePublicId("booknest/book-covers/old-cover")
            .build();
    MockMultipartFile cover =
        new MockMultipartFile("file", "cover.jpg", "image/jpeg", new byte[] {1, 2, 3});
    when(bookRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(book));
    when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
    when(bookCoverStorage.upload(cover))
        .thenReturn(
            new StoredBookCover(
                "booknest/book-covers/new-cover", "https://cloudinary.example/new-cover.jpg"));

    BookResponse response = bookService.updateCover(11L, cover, "owner@example.com");

    assertEquals("https://cloudinary.example/new-cover.jpg", response.coverImageUrl());
    assertEquals("booknest/book-covers/new-cover", book.getCoverImagePublicId());
    verify(bookRepository).saveAndFlush(book);
    verify(bookCoverStorage).delete("booknest/book-covers/old-cover");
  }

  @Test
  void archiveAndPublishFollowAllowedTransitions() {
    User owner = user(2L, "owner@example.com");
    Book book = book(11L, owner, BookStatus.AVAILABLE);
    when(bookRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(book));
    when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));

    assertEquals(BookStatus.ARCHIVED, bookService.archive(11L, "owner@example.com").status());
    assertEquals(BookStatus.AVAILABLE, bookService.publish(11L, "owner@example.com").status());
  }

  @Test
  void deleteRejectsAvailableBook() {
    User owner = user(2L, "owner@example.com");
    Book book = book(11L, owner, BookStatus.AVAILABLE);
    when(bookRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(book));
    when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));

    assertThrows(ConflictException.class, () -> bookService.delete(11L, "owner@example.com"));
    verify(bookRepository, never()).delete(any());
  }

  private BookRequest request() {
    return new BookRequest(
        " Clean Code ",
        "Robert C. Martin",
        null,
        "A programming book",
        new BigDecimal("45.50"),
        BookCondition.VERY_GOOD,
        "English",
        "Prentice Hall",
        (short) 2008,
        4L);
  }

  private Category category(Long id) {
    return Category.builder()
        .id(id)
        .name("Programming")
        .slug("programming")
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  private Book book(Long id, User seller, BookStatus status) {
    return bookBuilder(id, seller, status).build();
  }

  private Book.Builder bookBuilder(Long id, User seller, BookStatus status) {
    Instant now = Instant.now();
    return Book.builder()
        .id(id)
        .title("Clean Code")
        .author("Robert C. Martin")
        .price(new BigDecimal("45.50"))
        .bookCondition(BookCondition.GOOD)
        .language("English")
        .seller(seller)
        .category(category(4L))
        .status(status)
        .createdAt(now)
        .updatedAt(now);
  }

  private User user(Long id, String email) {
    Instant now = Instant.now();
    return User.builder()
        .id(id)
        .firstName("Test")
        .lastName("User")
        .email(email)
        .passwordHash("password")
        .role(Role.builder().id(1L).name("USER").build())
        .enabled(true)
        .emailVerified(false)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }
}
