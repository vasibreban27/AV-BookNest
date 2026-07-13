package com.avbooknest.cart.service;

import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.book.model.Book;
import com.avbooknest.book.model.BookStatus;
import com.avbooknest.book.repository.BookRepository;
import com.avbooknest.cart.dto.CartResponse;
import com.avbooknest.cart.model.Cart;
import com.avbooknest.cart.model.CartItem;
import com.avbooknest.cart.repository.CartItemRepository;
import com.avbooknest.cart.repository.CartRepository;
import com.avbooknest.common.exception.ConflictException;
import com.avbooknest.common.exception.ForbiddenException;
import com.avbooknest.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
@Transactional
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository, BookRepository bookRepository,
                       UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    public CartResponse get(String email) {
        return CartResponse.from(currentCart(email));
    }

    public CartResponse addItem(Long bookId, String email) {
        User user = currentUser(email);
        Cart cart = currentCart(user);
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new NotFoundException("Book not found"));
        if (book.getStatus() != BookStatus.AVAILABLE) {
            throw new ConflictException("Only available books can be added to the cart");
        }
        if (book.getSeller().getId().equals(user.getId())) {
            throw new ForbiddenException("You cannot add your own book to the cart");
        }
        if (cartItemRepository.existsByCartIdAndBookId(cart.getId(), bookId)) {
            throw new ConflictException("This book is already in the cart");
        }
        cart.addItem(CartItem.builder().cart(cart).book(book).addedAt(Instant.now()).build());
        return CartResponse.from(cartRepository.save(cart));
    }

    public void removeItem(Long bookId, String email) {
        Cart cart = currentCart(email);
        CartItem item = cart.getItems().stream().filter(candidate -> candidate.getBook().getId().equals(bookId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Book is not in the cart"));
        cart.removeItem(item);
    }

    public void clear(String email) {
        currentCart(email).clearItems();
    }

    private Cart currentCart(String email) {
        return currentCart(currentUser(email));
    }
    private Cart currentCart(User user) {
        return cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            Instant now = Instant.now();
            return cartRepository.save(Cart.builder().user(user).createdAt(now).updatedAt(now).build());
        });
    }
    private User currentUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
    }
}
