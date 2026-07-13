package com.redis.order.repository;

import com.redis.order.entity.Order;
import com.redis.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    List<Order> findByStatusAndOrderDateBefore(OrderStatus status, LocalDateTime time);

    @Override
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"items", "items.product"})
    Page<Order> findAll(Pageable pageable);

    /** Fetch all orders for a specific user, paginated. */
    @EntityGraph(attributePaths = {"items", "items.product"})
    Page<Order> findByUserId(Long userId, Pageable pageable);

    /** Admin: Filter orders by status. */
    @EntityGraph(attributePaths = {"items", "items.product"})
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /** Count orders by status (for analytics). */
    long countByStatus(OrderStatus status);

    /** Total revenue from delivered orders. */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'DELIVERED'")
    BigDecimal getTotalRevenue();

    /** Average order value. */
    @Query("SELECT COALESCE(AVG(o.totalAmount), 0) FROM Order o")
    BigDecimal getAverageOrderValue();
}
