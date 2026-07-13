package com.avbooknest.wishlist.repository;

import com.avbooknest.wishlist.model.WishlistItem;
import com.avbooknest.wishlist.model.WishlistItemId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, WishlistItemId> {
  @Query(
      """
      select item from WishlistItem item
      join fetch item.book book
      join fetch book.seller
      join fetch book.category
      where item.user.id = :userId
      order by item.createdAt desc
      """)
  List<WishlistItem> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

  Optional<WishlistItem> findByUserIdAndBookId(Long userId, Long bookId);

  boolean existsByUserIdAndBookId(Long userId, Long bookId);
}
