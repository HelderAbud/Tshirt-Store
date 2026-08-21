package com.revenda.service;

import com.revenda.domain.Cart;
import com.revenda.domain.ProductVariant;
import com.revenda.domain.User;
import com.revenda.exception.ResourceNotFoundException;
import com.revenda.repository.CartRepository;
import com.revenda.repository.ProductVariantRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

  private final CurrentUserService currentUserService;
  private final CartRepository cartRepository;
  private final ProductVariantRepository productVariantRepository;

  public CartService(
      CurrentUserService currentUserService,
      CartRepository cartRepository,
      ProductVariantRepository productVariantRepository) {
    this.currentUserService = currentUserService;
    this.cartRepository = cartRepository;
    this.productVariantRepository = productVariantRepository;
  }

  @Transactional
  public Cart getOrCreate() {
    return loadCart(false);
  }

  @Transactional
  public Cart upsertItem(Long variantId, int qty) {
    ProductVariant variant =
        productVariantRepository
            .findById(variantId)
            .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));
    Cart cart = loadCart(true);
    cart.addOrReplaceItem(variant, qty);
    cartRepository.save(cart);
    return cartRepository.findByUserIdWithItems(cart.getUser().getId()).orElse(cart);
  }

  @Transactional
  public void removeItem(Long variantId) {
    Cart cart = loadCart(true);
    if (!cart.removeItemByVariantId(variantId)) {
      throw new ResourceNotFoundException("Cart item not found for variant: " + variantId);
    }
    cartRepository.save(cart);
  }

  private Cart loadCart(boolean forUpdate) {
    User user = currentUserService.requireUser();
    if (forUpdate) {
      return cartRepository.findByUserIdForUpdate(user.getId()).orElseGet(() -> createCart(user));
    }
    return cartRepository.findByUserIdWithItems(user.getId()).orElseGet(() -> createCart(user));
  }

  private Cart createCart(User user) {
    try {
      return cartRepository.save(new Cart(user));
    } catch (DataIntegrityViolationException ex) {
      return cartRepository
          .findByUserIdForUpdate(user.getId())
          .or(() -> cartRepository.findByUserIdWithItems(user.getId()))
          .orElseThrow(() -> ex);
    }
  }
}
