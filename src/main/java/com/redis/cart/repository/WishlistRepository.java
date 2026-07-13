package com.redis.cart.repository;

import com.redis.cart.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    /** Fetch wishlist with products by user ID. */
    @Query("SELECT w FROM Wishlist w LEFT JOIN FETCH w.products WHERE w.user.id = :userId")
    Optional<Wishlist> findByUserIdWithProducts(@Param("userId") Long userId);

    /** Check whether a wishlist exists for a given user. */
    boolean existsByUserId(Long userId);
}
