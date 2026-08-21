package com.revenda.domain;

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

  @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CartItem> items = new ArrayList<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Cart() {}

  public Cart(User user) {
    this.user = user;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  public Long getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public List<CartItem> getItems() {
    return items;
  }

  public void touch() {
    this.updatedAt = Instant.now();
  }

  public CartItem addOrReplaceItem(ProductVariant variant, int qty) {
    for (CartItem item : items) {
      if (item.getVariant().getId().equals(variant.getId())) {
        item.setQty(qty);
        touch();
        return item;
      }
    }
    CartItem created = new CartItem(this, variant, qty);
    items.add(created);
    touch();
    return created;
  }

  public boolean removeItemByVariantId(Long variantId) {
    boolean removed = items.removeIf(item -> item.getVariant().getId().equals(variantId));
    if (removed) {
      touch();
    }
    return removed;
  }

  public void clearItems() {
    items.clear();
    touch();
  }
}
