package com.avbooknest.wishlist.service;

import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.book.model.Book;
import com.avbooknest.book.repository.BookRepository;
import com.avbooknest.common.exception.ConflictException;
import com.avbooknest.common.exception.NotFoundException;
import com.avbooknest.wishlist.dto.WishlistItemResponse;
import com.avbooknest.wishlist.model.WishlistItem;
import com.avbooknest.wishlist.repository.WishlistItemRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WishlistService {
  private final WishlistItemRepository wishlistItemRepository;
  private final BookRepository bookRepository;
  private final UserRepository userRepository;

  public WishlistService(
      WishlistItemRepository wishlistItemRepository,
      BookRepository bookRepository,
      UserRepository userRepository) {
    this.wishlistItemRepository = wishlistItemRepository;
    this.bookRepository = bookRepository;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<WishlistItemResponse> list(String email) {
    return wishlistItemRepository
        .findAllByUserIdOrderByCreatedAtDesc(currentUser(email).getId())
        .stream()
        .map(WishlistItemResponse::from)
        .toList();
  }

  public WishlistItemResponse add(Long bookId, String email) {
    User user = currentUser(email);
    if (wishlistItemRepository.existsByUserIdAndBookId(user.getId(), bookId)) {
      throw new ConflictException("This book is already in the wishlist");
    }

    Book book =
        bookRepository.findById(bookId).orElseThrow(() -> new NotFoundException("Book not found"));
    WishlistItem item =
        WishlistItem.builder().user(user).book(book).createdAt(Instant.now()).build();
    return WishlistItemResponse.from(wishlistItemRepository.save(item));
  }

  public void remove(Long bookId, String email) {
    Long userId = currentUser(email).getId();
    WishlistItem item =
        wishlistItemRepository
            .findByUserIdAndBookId(userId, bookId)
            .orElseThrow(() -> new NotFoundException("Book is not in the wishlist"));
    wishlistItemRepository.delete(item);
  }

  private User currentUser(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new NotFoundException("User not found"));
  }
}
