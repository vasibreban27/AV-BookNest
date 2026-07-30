package com.avbooknest.auth.repository;

import com.avbooknest.auth.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  Optional<User> findByStripeAccountId(String stripeAccountId);

  boolean existsByEmail(String email);
}
