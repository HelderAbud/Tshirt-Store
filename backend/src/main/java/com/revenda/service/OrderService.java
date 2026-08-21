package com.revenda.service;

import com.revenda.domain.Cart;
import com.revenda.domain.CartItem;
import com.revenda.domain.OrderItem;
import com.revenda.domain.OrderStatus;
import com.revenda.domain.PaymentIdempotencyKey;
import com.revenda.domain.Product;
import com.revenda.domain.ProductVariant;
import com.revenda.domain.ShopOrder;
import com.revenda.domain.User;
import com.revenda.exception.EmptyCartException;
import com.revenda.exception.IdempotencyConflictException;
import com.revenda.exception.InsufficientStockException;
import com.revenda.exception.InvalidOrderStatusTransitionException;
import com.revenda.exception.ResourceNotFoundException;
import com.revenda.repository.CartRepository;
import com.revenda.repository.PaymentIdempotencyKeyRepository;
import com.revenda.repository.ProductRepository;
import com.revenda.repository.ProductVariantRepository;
import com.revenda.repository.ShopOrderRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

  private final CurrentUserService currentUserService;
  private final CartRepository cartRepository;
  private final ShopOrderRepository shopOrderRepository;
  private final ProductVariantRepository productVariantRepository;
  private final ProductRepository productRepository;
  private final PaymentIdempotencyKeyRepository paymentIdempotencyKeyRepository;

  public OrderService(
      CurrentUserService currentUserService,
      CartRepository cartRepository,
      ShopOrderRepository shopOrderRepository,
      ProductVariantRepository productVariantRepository,
      ProductRepository productRepository,
      PaymentIdempotencyKeyRepository paymentIdempotencyKeyRepository) {
    this.currentUserService = currentUserService;
    this.cartRepository = cartRepository;
    this.shopOrderRepository = shopOrderRepository;
    this.productVariantRepository = productVariantRepository;
    this.productRepository = productRepository;
    this.paymentIdempotencyKeyRepository = paymentIdempotencyKeyRepository;
  }

  @Transactional
  public ShopOrder checkout() {
    User user = currentUserService.requireUser();
    cartRepository.findByUserIdForUpdate(user.getId()).orElseThrow(EmptyCartException::new);
    Cart cart =
        cartRepository.findByUserIdWithItems(user.getId()).orElseThrow(EmptyCartException::new);
    if (cart.getItems().isEmpty()) {
      throw new EmptyCartException();
    }
    var lockedVariants = lockAndAssertStock(cart);

    BigDecimal total = BigDecimal.ZERO;
    ShopOrder order = new ShopOrder(user, OrderStatus.CREATED, BigDecimal.ZERO);
    for (CartItem cartItem : cart.getItems()) {
      ProductVariant variant = lockedVariants.get(cartItem.getVariant().getId());
      BigDecimal line = variant.getPrice().multiply(BigDecimal.valueOf(cartItem.getQty()));
      total = total.add(line);
      order.addItem(
          new OrderItem(
              order,
              variant,
              variant.getSku(),
              variant.getProduct().getName(),
              variant.getColor(),
              variant.getSize(),
              variant.getPrice(),
              cartItem.getQty()));
    }
    order.setTotal(total);
    ShopOrder saved = shopOrderRepository.save(order);
    cart.clearItems();
    cartRepository.save(cart);
    return saved;
  }

  private Map<Long, ProductVariant> lockAndAssertStock(Cart cart) {
    Map<Long, ProductVariant> locked = new LinkedHashMap<>();
    List<CartItem> ordered =
        cart.getItems().stream()
            .sorted(Comparator.comparing(item -> item.getVariant().getId()))
            .toList();
    for (CartItem cartItem : ordered) {
      ProductVariant variant =
          productVariantRepository
              .findByIdForUpdate(cartItem.getVariant().getId())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "Variant not found: " + cartItem.getVariant().getId()));
      if (cartItem.getQty() > variant.getStockQty()) {
        throw new InsufficientStockException();
      }
      locked.put(variant.getId(), variant);
    }
    return locked;
  }

  public List<ShopOrder> listMine() {
    User user = currentUserService.requireUser();
    return shopOrderRepository.findByUser_IdOrderByCreatedAtDesc(user.getId());
  }

  @Transactional(readOnly = true)
  public ShopOrder getMine(Long id) {
    User user = currentUserService.requireUser();
    return shopOrderRepository
        .findByIdAndUserIdWithItems(id, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
  }

  @Transactional
  public ShopOrder cancelAsAdmin(Long id) {
    ShopOrder order =
        shopOrderRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    if (order.getStatus() != OrderStatus.CREATED) {
      throw new InvalidOrderStatusTransitionException();
    }
    order.cancel();
    return shopOrderRepository.save(order);
  }

  @Transactional
  public ShopOrder pay(Long orderId, String idempotencyKey) {
    User user = currentUserService.requireUser();
    var existingKey =
        paymentIdempotencyKeyRepository.findByUser_IdAndIdempotencyKey(
            user.getId(), idempotencyKey);
    if (existingKey.isPresent()) {
      Long recordedOrderId = existingKey.get().getOrder().getId();
      if (!recordedOrderId.equals(orderId)) {
        throw new IdempotencyConflictException();
      }
      return shopOrderRepository
          .findByIdAndUserIdWithItems(orderId, user.getId())
          .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    shopOrderRepository
        .findByIdAndUserIdForUpdate(orderId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    ShopOrder order =
        shopOrderRepository
            .findByIdAndUserIdWithItems(orderId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    if (order.getStatus() != OrderStatus.CREATED) {
      throw new InvalidOrderStatusTransitionException();
    }

    List<OrderItem> orderedItems =
        order.getItems().stream()
            .sorted(Comparator.comparing(item -> item.getVariant().getId()))
            .toList();
    var productIds = new TreeSet<Long>();
    for (OrderItem item : orderedItems) {
      ProductVariant variant =
          productVariantRepository
              .findByIdForUpdate(item.getVariant().getId())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "Variant not found: " + item.getVariant().getId()));
      variant.decreaseStock(item.getQty());
      productIds.add(variant.getProductId());
    }
    syncParentStock(productIds);
    order.markPaid();
    shopOrderRepository.save(order);
    try {
      paymentIdempotencyKeyRepository.save(new PaymentIdempotencyKey(user, idempotencyKey, order));
    } catch (DataIntegrityViolationException ex) {
      return shopOrderRepository
          .findByIdAndUserIdWithItems(orderId, user.getId())
          .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }
    return order;
  }

  private void syncParentStock(TreeSet<Long> productIds) {
    for (Long productId : productIds) {
      Product product =
          productRepository
              .findByIdForUpdate(productId)
              .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
      int remaining =
          productVariantRepository.findByProduct_Id(productId).stream()
              .mapToInt(ProductVariant::getStockQty)
              .sum();
      product.syncStockQty(remaining);
    }
  }
}
