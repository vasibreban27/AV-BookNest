package com.avbooknest.cart.model;

import com.avbooknest.auth.model.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
public class Cart {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CartItem> items = new ArrayList<>();

  protected Cart() {}

  private Cart(Builder builder) {
    user = builder.user;
    createdAt = builder.createdAt;
    updatedAt = builder.updatedAt;
  }

  public Long getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public List<CartItem> getItems() {
    return items;
  }

  public void addItem(CartItem item) {
    items.add(item);
    touch();
  }

  public void removeItem(CartItem item) {
    items.remove(item);
    touch();
  }

  public void clearItems() {
    items.clear();
    touch();
  }

  private void touch() {
    updatedAt = Instant.now();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private User user;
    private Instant createdAt;
    private Instant updatedAt;

    public Builder user(User value) {
      user = value;
      return this;
    }

    public Builder createdAt(Instant value) {
      createdAt = value;
      return this;
    }

    public Builder updatedAt(Instant value) {
      updatedAt = value;
      return this;
    }

    public Cart build() {
      return new Cart(this);
    }
  }
}
