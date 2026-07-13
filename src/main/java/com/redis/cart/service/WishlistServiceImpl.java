package com.redis.cart.service;

import com.redis.product.exception.ProductNotFoundException;
import com.redis.cart.dto.response.WishlistResponse;
import com.redis.product.entity.Product;
import com.redis.user.entity.User;
import com.redis.cart.entity.Wishlist;
import com.redis.product.mapper.ProductMapper;
import com.redis.product.repository.ProductRepository;
import com.redis.user.repository.UserRepository;
import com.redis.cart.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public WishlistResponse getWishlist(Long userId) {
        Wishlist wishlist = getOrCreateWishlist(userId);
        return toResponse(wishlist);
    }

    @Override
    @Transactional
    public WishlistResponse addProduct(Long userId, Long productId) {
        Wishlist wishlist = getOrCreateWishlist(userId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        boolean alreadyPresent = wishlist.getProducts().stream()
                .anyMatch(p -> p.getId().equals(productId));

        if (!alreadyPresent) {
            wishlist.getProducts().add(product);
            wishlist = wishlistRepository.save(wishlist);
        }

        return toResponse(wishlist);
    }

    @Override
    @Transactional
    public WishlistResponse removeProduct(Long userId, Long productId) {
        Wishlist wishlist = getOrCreateWishlist(userId);

        boolean removed = wishlist.getProducts().removeIf(p -> p.getId().equals(productId));
        if (!removed) {
            throw new IllegalArgumentException("Product not found in wishlist: " + productId);
        }

        wishlist = wishlistRepository.save(wishlist);
        return toResponse(wishlist);
    }

    @Override
    @Transactional
    public WishlistResponse clearWishlist(Long userId) {
        Wishlist wishlist = getOrCreateWishlist(userId);
        wishlist.getProducts().clear();
        wishlist = wishlistRepository.save(wishlist);
        return toResponse(wishlist);
    }

    private Wishlist getOrCreateWishlist(Long userId) {
        return wishlistRepository.findByUserIdWithProducts(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
                    Wishlist newWishlist = Wishlist.builder()
                            .user(user)
                            .products(new ArrayList<>())
                            .build();
                    return wishlistRepository.save(newWishlist);
                });
    }

    private WishlistResponse toResponse(Wishlist wishlist) {
        return WishlistResponse.builder()
                .wishlistId(wishlist.getId())
                .userId(wishlist.getUser().getId())
                .products(wishlist.getProducts().stream()
                        .map(productMapper::toResponse)
                        .collect(Collectors.toList()))
                .productCount(wishlist.getProducts().size())
                .updatedAt(wishlist.getUpdatedAt())
                .build();
    }
}
