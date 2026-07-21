package com.redis.order.service;

import com.redis.order.dto.request.OrderRequest;
import com.redis.order.dto.request.OrderStatusUpdateRequest;
import com.redis.order.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    /** User: Place a new order from the current cart. */
    OrderResponse placeOrder(Long userId, OrderRequest request);

    /** User: View own orders, paginated. */
    Page<OrderResponse> getMyOrders(Long userId, Pageable pageable);

    /** User/Admin: Get a single order by ID. */
    OrderResponse getOrderById(Long orderId, Long userId, boolean isAdmin);

    /** User: Cancel own order (only if PENDING). */
    OrderResponse cancelOrder(Long orderId, Long userId);

    /** Admin: Get all orders (optionally filter by status). */
    Page<OrderResponse> getAllOrders(String status, Pageable pageable);

    /** Admin: Update order status. */
    OrderResponse updateOrderStatus(Long orderId, OrderStatusUpdateRequest request);

    /** Internal: Finalize order payment success (Option B Commit). */
    void completeOrderPayment(Long orderId);

    /** Internal: Mark order payment failed. */
    void failOrderPayment(Long orderId);

    /** Internal: Expire order payment timeout. */
    void expireOrder(Long orderId);

    /** Internal: Expire order payment timeout. */
    void expireOrder(com.redis.order.entity.Order order);
}
