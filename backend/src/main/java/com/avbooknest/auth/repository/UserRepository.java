package com.avbooknest.auth.repository;

import com.avbooknest.auth.model.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from User u where u.id = :id")
  Optional<User> findByIdForUpdate(@Param("id") Long id);

  @Query(
      "select u.id from User u where u.enabled = false and u.suspendedUntil <= :now order by u.id")
  List<Long> findExpiredSuspensions(@Param("now") Instant now, Pageable pageable);

  @Query(
      "select u from User u where u.enabled = true and u.role.name = 'ADMIN' order by u.firstName, u.lastName, u.id")
  List<User> findSupportAdministrators();

  /** Public reputation must not turn buyer-only accounts into public profiles. */
  @Query(
      """
      select u from User u where u.id = :id and (
        exists (select b.id from Book b where b.seller = u
          and b.status <> com.avbooknest.book.model.BookStatus.DRAFT)
        or exists (select so.id from SellerOrder so where so.seller = u))
      """)
  Optional<User> findSellerById(@Param("id") Long id);

  Optional<User> findByStripeAccountId(String stripeAccountId);

  boolean existsByEmail(String email);

  @Query(
      """
      select u from User u
      where (:query = ''
        or lower(u.email) like concat('%', :query, '%')
        or lower(u.firstName) like concat('%', :query, '%')
        or lower(u.lastName) like concat('%', :query, '%'))
      and (:enabled is null or u.enabled = :enabled)
      """)
  @EntityGraph(attributePaths = "role")
  Page<User> searchForAdmin(
      @Param("query") String query, @Param("enabled") Boolean enabled, Pageable pageable);

  long countByCreatedAtGreaterThanEqual(Instant since);
}
