package com.avbooknest.wishlist.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avbooknest.auth.model.Role;
import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.book.model.Book;
import com.avbooknest.book.model.BookCondition;
import com.avbooknest.book.model.BookStatus;
import com.avbooknest.book.model.Category;
import com.avbooknest.book.repository.BookRepository;
import com.avbooknest.common.exception.ConflictException;
import com.avbooknest.common.exception.NotFoundException;
import com.avbooknest.wishlist.dto.WishlistItemResponse;
import com.avbooknest.wishlist.model.WishlistItem;
import com.avbooknest.wishlist.repository.WishlistItemRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {
  @Mock private WishlistItemRepository wishlistItemRepository;
  @Mock private BookRepository bookRepository;
  @Mock private UserRepository userRepository;

  private WishlistService wishlistService;

  @BeforeEach
  void setUp() {
    wishlistService = new WishlistService(wishlistItemRepository, bookRepository, userRepository);
  }

  @Test
  void addBookStoresItInCurrentUsersWishlist() {
    User buyer = user(1L, "buyer@example.com");
    Book book = book(9L);
    when(userRepository.findByEmail(buyer.getEmail())).thenReturn(Optional.of(buyer));
    when(wishlistItemRepository.existsByUserIdAndBookId(buyer.getId(), book.getId()))
        .thenReturn(false);
    when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
    when(wishlistItemRepository.save(any(WishlistItem.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    WishlistItemResponse response = wishlistService.add(book.getId(), buyer.getEmail());

    assertEquals(book.getId(), response.book().id());
    assertEquals(book.getTitle(), response.book().title());
    verify(wishlistItemRepository).save(any(WishlistItem.class));
  }

  @Test
  void addDuplicateBookIsRejected() {
    User buyer = user(1L, "buyer@example.com");
    when(userRepository.findByEmail(buyer.getEmail())).thenReturn(Optional.of(buyer));
    when(wishlistItemRepository.existsByUserIdAndBookId(buyer.getId(), 9L)).thenReturn(true);

    assertThrows(ConflictException.class, () -> wishlistService.add(9L, buyer.getEmail()));
  }

  @Test
  void removeDeletesBookFromCurrentUsersWishlist() {
    User buyer = user(1L, "buyer@example.com");
    WishlistItem item =
        WishlistItem.builder().user(buyer).book(book(9L)).createdAt(Instant.now()).build();
    when(userRepository.findByEmail(buyer.getEmail())).thenReturn(Optional.of(buyer));
    when(wishlistItemRepository.findByUserIdAndBookId(buyer.getId(), 9L))
        .thenReturn(Optional.of(item));

    wishlistService.remove(9L, buyer.getEmail());

    verify(wishlistItemRepository).delete(item);
  }

  @Test
  void removeMissingBookIsRejected() {
    User buyer = user(1L, "buyer@example.com");
    when(userRepository.findByEmail(buyer.getEmail())).thenReturn(Optional.of(buyer));
    when(wishlistItemRepository.findByUserIdAndBookId(buyer.getId(), 9L))
        .thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> wishlistService.remove(9L, buyer.getEmail()));
  }

  private Book book(Long id) {
    Instant now = Instant.now();
    Category category =
        Category.builder()
            .id(5L)
            .name("Programming")
            .slug("programming")
            .createdAt(now)
            .updatedAt(now)
            .build();
    return Book.builder()
        .id(id)
        .title("Refactoring")
        .author("Martin Fowler")
        .price(new BigDecimal("30.00"))
        .bookCondition(BookCondition.GOOD)
        .language("English")
        .seller(user(2L, "seller@example.com"))
        .category(category)
        .status(BookStatus.AVAILABLE)
        .createdAt(now)
        .updatedAt(now)
        .build();
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
        .emailVerified(true)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }
}
