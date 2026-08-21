package com.avbooknest.book.repository;

import com.avbooknest.book.dto.CatalogCategoryResponse;
import com.avbooknest.book.model.Book;
import com.avbooknest.book.model.BookCondition;
import com.avbooknest.book.model.BookModerationReason;
import com.avbooknest.book.model.BookModerationStatus;
import com.avbooknest.book.model.BookStatus;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {
  @EntityGraph(attributePaths = {"seller", "category"})
  @Query(
      """
      select book from Book book
      where book.status = com.avbooknest.book.model.BookStatus.AVAILABLE
      and book.moderationStatus = com.avbooknest.book.model.BookModerationStatus.VISIBLE
      and book.category.active = true
      and (:query = ''
        or translate(lower(book.title), 'ăâîșşțţáàäãåéèëêíìïóòöôõúùüûçñ', 'aaissttaaaaaeeeeiiiooooouuuucn')
          like concat('%', :query, '%')
        or translate(lower(book.author), 'ăâîșşțţáàäãåéèëêíìïóòöôõúùüûçñ', 'aaissttaaaaaeeeeiiiooooouuuucn')
          like concat('%', :query, '%')
        or lower(coalesce(book.isbn, '')) like concat('%', :query, '%')
        or translate(lower(coalesce(book.publisher, '')), 'ăâîșşțţáàäãåéèëêíìïóòöôõúùüûçñ', 'aaissttaaaaaeeeeiiiooooouuuucn')
          like concat('%', :query, '%')
        or translate(lower(concat(book.title, ' ', book.author)), 'ăâîșşțţáàäãåéèëêíìïóòöôõúùüûçñ', 'aaissttaaaaaeeeeiiiooooouuuucn')
          like concat('%', replace(:query, ' ', '%'), '%'))
      and (:categorySlug is null or book.category.slug = :categorySlug)
      and (:condition is null or book.bookCondition = :condition)
      and (:minimumPrice is null or book.price >= :minimumPrice)
      and (:maximumPrice is null or book.price <= :maximumPrice)
      and (:language = '' or lower(book.language) = :language)
      and (:minimumYear is null or book.publishedYear >= :minimumYear)
      and (:maximumYear is null or book.publishedYear <= :maximumYear)
      """)
  Page<Book> searchAvailable(
      @Param("query") String query,
      @Param("categorySlug") String categorySlug,
      @Param("condition") BookCondition condition,
      @Param("minimumPrice") BigDecimal minimumPrice,
      @Param("maximumPrice") BigDecimal maximumPrice,
      @Param("language") String language,
      @Param("minimumYear") Integer minimumYear,
      @Param("maximumYear") Integer maximumYear,
      Pageable pageable);

  @Query(
      """
      select distinct book.language from Book book
      where book.status = com.avbooknest.book.model.BookStatus.AVAILABLE
      and book.moderationStatus = com.avbooknest.book.model.BookModerationStatus.VISIBLE
      and book.category.active = true
      order by book.language
      """)
  List<String> findAvailableLanguages();

  @Query(
      """
      select new com.avbooknest.book.dto.CatalogCategoryResponse(
        book.category.id,
        book.category.name,
        book.category.slug,
        count(book.id)
      )
      from Book book
      where book.status = com.avbooknest.book.model.BookStatus.AVAILABLE
      and book.moderationStatus = com.avbooknest.book.model.BookModerationStatus.VISIBLE
      and book.category.active = true
      group by book.category.id, book.category.name, book.category.slug
      order by book.category.name
      """)
  List<CatalogCategoryResponse> findAvailableCategories();

  @EntityGraph(attributePaths = {"seller", "category"})
  List<Book> findAllBySellerIdOrderByCreatedAtDesc(Long sellerId);

  @EntityGraph(attributePaths = {"seller", "category", "moderatedBy"})
  @Query(
      """
      select book from Book book
      where (:query = ''
        or lower(book.title) like concat('%', :query, '%')
        or lower(book.author) like concat('%', :query, '%')
        or lower(book.seller.email) like concat('%', :query, '%'))
      and (:status is null or book.status = :status)
      and (:moderationStatus is null or book.moderationStatus = :moderationStatus)
      """)
  Page<Book> searchForAdmin(
      @Param("query") String query,
      @Param("status") BookStatus status,
      @Param("moderationStatus") BookModerationStatus moderationStatus,
      Pageable pageable);

  List<Book> findAllBySellerIdAndModerationStatus(Long sellerId, BookModerationStatus status);

  List<Book> findAllBySellerIdAndModerationStatusAndModerationReason(
      Long sellerId, BookModerationStatus status, BookModerationReason reason);

  long countBySellerId(Long sellerId);

  long countByStatusAndModerationStatus(BookStatus status, BookModerationStatus moderationStatus);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select book from Book book where book.id = :bookId")
  java.util.Optional<Book> findByIdForUpdate(@Param("bookId") Long bookId);
}
