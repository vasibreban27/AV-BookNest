package com.avbooknest.auth.service;

import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserSecurityService implements UserDetailsService {

  private final UserRepository userRepository;

  public UserSecurityService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String email) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
    return toUserDetails(user);
  }

  public UserDetails toUserDetails(User user) {
    return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
        .password(user.getPasswordHash())
        .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()))
        .disabled(!user.isEnabled())
        .build();
  }
}
