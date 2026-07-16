package com.avbooknest.cart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.avbooknest.auth.model.Role;
import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.book.model.Book;
import com.avbooknest.book.model.BookCondition;
import com.avbooknest.book.model.BookStatus;
import com.avbooknest.book.model.Category;
import com.avbooknest.book.repository.BookRepository;
import com.avbooknest.cart.dto.CartResponse;
import com.avbooknest.cart.model.Cart;
import com.avbooknest.cart.model.CartItem;
import com.avbooknest.cart.repository.CartItemRepository;
import com.avbooknest.cart.repository.CartRepository;
import com.avbooknest.common.exception.ForbiddenException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {
  @Mock private CartRepository cartRepository;
  @Mock private CartItemRepository cartItemRepository;
  @Mock private BookRepository bookRepository;
  @Mock private UserRepository userRepository;
  private CartService cartService;

  @BeforeEach
  void setUp() {
    cartService =
        new CartService(cartRepository, cartItemRepository, bookRepository, userRepository);
  }

  @Test
  void addAvailableBookAddsItToCurrentUsersCartAndCalculatesTotal() {
    User buyer = user(1L, "buyer@example.com");
    Cart cart = cart(buyer);
    Book book = book(9L, user(2L, "seller@example.com"), BookStatus.AVAILABLE);
    when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(buyer));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
    when(bookRepository.findById(9L)).thenReturn(Optional.of(book));
    when(cartItemRepository.existsByCartIdAndBookId(any(), any())).thenReturn(false);
    when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

    CartResponse response = cartService.addItem(9L, "buyer@example.com");

    assertEquals(1, response.items().size());
    assertEquals("Refactoring", response.items().getFirst().book().title());
    assertEquals(new BigDecimal("30.00"), response.total());
  }

  @Test
  void addOwnBookIsRejected() {
    User user = user(1L, "buyer@example.com");
    when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart(user)));
    when(bookRepository.findById(9L)).thenReturn(Optional.of(book(9L, user, BookStatus.AVAILABLE)));

    assertThrows(ForbiddenException.class, () -> cartService.addItem(9L, "buyer@example.com"));
  }

  @Test
  void removeItemRemovesTheMatchingBookFromCart() {
    User buyer = user(1L, "buyer@example.com");
    Cart cart = cart(buyer);
    Book book = book(9L, user(2L, "seller@example.com"), BookStatus.AVAILABLE);
    cart.addItem(CartItem.builder().cart(cart).book(book).addedAt(Instant.now()).build());
    when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(buyer));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

    cartService.removeItem(9L, "buyer@example.com");

    assertEquals(0, cart.getItems().size());
  }

  private Cart cart(User user) {
    Instant now = Instant.now();
    return Cart.builder().user(user).createdAt(now).updatedAt(now).build();
  }

  private Book book(Long id, User seller, BookStatus status) {
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
        .seller(seller)
        .category(category)
        .status(status)
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
