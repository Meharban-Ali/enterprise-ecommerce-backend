package com.redis.cart.service;

import com.redis.inventory.exception.OutOfStockException;
import com.redis.common.exception.InvalidQuantityException;
import com.redis.product.exception.ProductNotFoundException;
import com.redis.cart.dto.request.CartItemRequest;
import com.redis.cart.dto.response.CartItemResponse;
import com.redis.cart.dto.response.CartResponse;
import com.redis.cart.entity.Cart;
import com.redis.cart.entity.CartItem;
import com.redis.product.entity.Product;
import com.redis.user.entity.User;
import com.redis.cart.repository.CartRepository;
import com.redis.product.repository.ProductRepository;
import com.redis.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(Long userId, CartItemRequest request) {
        Cart cart = getOrCreateCart(userId);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(request.getProductId()));

        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(request.getProductId()))
                .findFirst()
                .orElse(null);

        if (request.getQuantity() <= 0) {
            throw new InvalidQuantityException("Quantity must be at least 1");
        }

        if (existingItem != null) {
            int newQty = existingItem.getQuantity() + request.getQuantity();
            if (product.getStockQuantity() < newQty) {
                throw new OutOfStockException(
                        "Cannot add requested quantity — insufficient stock. Available: " + product.getStockQuantity());
            }
            existingItem.setQuantity(newQty);
        } else {
            if (product.getStockQuantity() < request.getQuantity()) {
                throw new OutOfStockException(
                        "Cannot add requested quantity — insufficient stock. Available: " + product.getStockQuantity());
            }
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cart.getItems().add(newItem);
        }

        Cart saved = cartRepository.save(cart);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public CartResponse updateItem(Long userId, Long itemId, CartItemRequest request) {
        if (request.getQuantity() <= 0) {
            throw new InvalidQuantityException("Quantity must be at least 1");
        }

        Cart cart = getOrCreateCart(userId);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found with ID: " + itemId));

        if (item.getProduct().getStockQuantity() < request.getQuantity()) {
            throw new OutOfStockException(
                    "Cannot set requested quantity — insufficient stock. Available: " + item.getProduct().getStockQuantity());
        }

        item.setQuantity(request.getQuantity());
        Cart saved = cartRepository.save(cart);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long itemId) {
        Cart cart = getOrCreateCart(userId);

        boolean removed = cart.getItems().removeIf(i -> i.getId().equals(itemId));
        if (!removed) {
            throw new IllegalArgumentException("Cart item not found with ID: " + itemId);
        }

        Cart saved = cartRepository.save(cart);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public CartResponse clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        Cart saved = cartRepository.save(cart);
        return toResponse(saved);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
                    Cart newCart = Cart.builder()
                            .user(user)
                            .items(new ArrayList<>())
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());

        BigDecimal total = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalQty = itemResponses.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(cart.getUser().getId())
                .items(itemResponses)
                .itemCount(itemResponses.size())
                .totalItems(itemResponses.size())
                .totalQuantity(totalQty)
                .totalAmount(total)
                .subtotal(total)
                .grandTotal(total)
                .discount(BigDecimal.ZERO)
                .shippingCharge(BigDecimal.ZERO)
                .tax(BigDecimal.ZERO)
                .couponDiscount(BigDecimal.ZERO)
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    private CartItemResponse toItemResponse(CartItem item) {
        Product product = item.getProduct();
        int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

        String stockStatus;
        if (stock == 0) stockStatus = "OUT_OF_STOCK";
        else if (stock <= 10) stockStatus = "LOW_STOCK";
        else stockStatus = "IN_STOCK";

        return CartItemResponse.builder()
                .itemId(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .unitPrice(product.getPrice())
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .stockStatus(stockStatus)
                .build();
    }
}
