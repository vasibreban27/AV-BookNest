package com.avbooknest.auth.repository;

import com.avbooknest.auth.model.User;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

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
