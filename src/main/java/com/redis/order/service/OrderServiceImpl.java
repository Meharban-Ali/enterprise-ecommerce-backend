package com.redis.order.service;

import com.redis.product.entity.Product;
import com.redis.order.entity.Order;
import com.redis.cart.service.CartService;
import com.redis.inventory.service.InventoryReservationService;
import com.redis.audit.entity.AuditActionType;
import com.redis.notification.event.NotificationEventPublisher;
import com.redis.common.entity.ResourceType;
import com.redis.audit.entity.AuditStatus;
import com.redis.audit.event.AuditEventPublisher;
import com.redis.cart.entity.Cart;
import com.redis.cart.entity.CartItem;
import com.redis.user.entity.User;
import com.redis.order.entity.OrderItem;

import com.redis.order.exception.InvalidOrderStateException;
import com.redis.product.exception.ProductNotFoundException;
import com.redis.order.dto.request.OrderRequest;
import com.redis.order.dto.request.OrderStatusUpdateRequest;
import com.redis.order.dto.response.OrderItemResponse;
import com.redis.order.dto.response.OrderResponse;
import com.redis.order.entity.OrderStatus;
import com.redis.cart.repository.CartRepository;
import com.redis.order.repository.OrderRepository;
import com.redis.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final InventoryReservationService inventoryReservationService;
    private final NotificationEventPublisher notificationEventPublisher;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.redis.audit.event.AuditEventPublisher auditEventPublisher;

    @Override
    @Transactional
    public OrderResponse placeOrder(Long userId, OrderRequest request) {
        log.info("Placing order for user ID: {}", userId);

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart is empty. Add items before placing an order."));

        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty. Add items before placing an order.");
        }

        User user = cart.getUser();
        Order order = Order.builder()
                .user(user)
                .shippingAddress(request.getShippingAddress())
                .notes(request.getNotes())
                .status(OrderStatus.PENDING_PAYMENT)
                .items(new ArrayList<>())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            BigDecimal unitPrice = product.getPrice();
            int qty = cartItem.getQuantity();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .unitPrice(unitPrice)
                    .quantity(qty)
                    .subtotal(subtotal)
                    .build();

            order.getItems().add(orderItem);
        }

        order.setTotalAmount(totalAmount);
        
        // Save order structure first so that items have reference keys
        Order savedOrder = orderRepository.save(order);

        // Perform inventory soft reservation (Option B Reservation)
        inventoryReservationService.reserveInventory(savedOrder);

        // Save order state
        savedOrder = orderRepository.save(savedOrder);

        log.info("Order created in PENDING_PAYMENT status — ID: {}, Total: {}", savedOrder.getId(), totalAmount);
        
        if (auditEventPublisher != null) {
            auditEventPublisher.publish(userId, user.getEmail(), com.redis.audit.entity.AuditActionType.ORDER_CREATED, com.redis.audit.entity.AuditStatus.SUCCESS,
                    com.redis.common.entity.ResourceType.ORDER, String.valueOf(savedOrder.getId()), "Order placed successfully in PENDING_PAYMENT status");
        }

        try {
            notificationEventPublisher.publishOrderCreated(userId, savedOrder.getId(), totalAmount);
        } catch (Exception e) {
            log.error("Failed to publish order created event for order ID: {}", savedOrder.getId(), e);
        }

        return toResponse(savedOrder);
    }

    @Override
    @Transactional
    public void completeOrderPayment(Long orderId) {
        log.info("Completing order payment for ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidOrderStateException("Order not in PENDING_PAYMENT status: " + order.getStatus());
        }

        inventoryReservationService.commitReservation(order);
        cartService.clearCart(order.getUser().getId());

        order.setStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);
        log.info("Order successfully completed and transitioned to PROCESSING — ID: {}", orderId);

        try {
            notificationEventPublisher.publishOrderProcessing(order.getUser().getId(), order.getId());
        } catch (Exception e) {
            log.error("Failed to publish order processing event for order ID: {}", orderId, e);
        }
    }

    @Override
    @Transactional
    public void failOrderPayment(Long orderId) {
        log.info("Failing payment for order ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            inventoryReservationService.releaseReservation(order);
            orderRepository.save(order);
            log.info("Order transitioned to PAYMENT_FAILED and reservation released — ID: {}", orderId);
        }
    }

    @Override
    @Transactional
    public void expireOrder(Long orderId) {
        log.info("Expiring payment timeout for order ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
        expireOrder(order);
    }

    @Override
    @Transactional
    public void expireOrder(Order order) {
        if (order == null) return;
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.setStatus(OrderStatus.EXPIRED);
            inventoryReservationService.releaseReservation(order);
            orderRepository.save(order);
            log.info("Order transitioned to EXPIRED and reservation released — ID: {}", order.getId());

            try {
                notificationEventPublisher.publishOrderExpired(order.getUser().getId(), order.getId());
            } catch (Exception e) {
                log.error("Failed to publish order expired event for order ID: {}", order.getId(), e);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        if (!isAdmin && !order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Access denied — you can only view your own orders.");
        }

        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Access denied — you can only cancel your own orders.");
        }

        OrderStatus status = order.getStatus();
        if (status != OrderStatus.PENDING_PAYMENT && status != OrderStatus.PROCESSING) {
            throw new InvalidOrderStateException("Order cannot be cancelled in state: " + status);
        }

        // Release inventory reservation if it was reserved and not committed/restored
        if (status == OrderStatus.PENDING_PAYMENT || status == OrderStatus.PROCESSING) {
            inventoryReservationService.releaseReservation(order);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        log.info("Order cancelled — ID: {}", orderId);

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(saved.getUser().getId(), saved.getUser().getEmail(), com.redis.audit.entity.AuditActionType.ORDER_CANCELLED, com.redis.audit.entity.AuditStatus.SUCCESS,
                    com.redis.common.entity.ResourceType.ORDER, String.valueOf(saved.getId()), "Order cancelled successfully");
        }

        try {
            notificationEventPublisher.publishOrderCancelled(saved.getUser().getId(), saved.getId());
        } catch (Exception e) {
            log.error("Failed to publish order cancelled event for order ID: {}", orderId, e);
        }

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                return orderRepository.findByStatus(orderStatus, pageable).map(this::toResponse);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid order status: " + status);
            }
        }
        return orderRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid order status: " + request.getStatus());
        }

        if (!isValidTransition(currentStatus, newStatus)) {
            throw new InvalidOrderStateException("Illegal status transition from " + currentStatus + " to " + newStatus);
        }

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);
        log.info("Order status updated — ID: {}, New Status: {}", orderId, saved.getStatus());

        com.redis.audit.entity.AuditActionType auditAction = com.redis.audit.entity.AuditActionType.ORDER_UPDATED;
        if (newStatus == OrderStatus.SHIPPED) {
            auditAction = com.redis.audit.entity.AuditActionType.ORDER_SHIPPED;
        } else if (newStatus == OrderStatus.DELIVERED) {
            auditAction = com.redis.audit.entity.AuditActionType.ORDER_DELIVERED;
        } else if (newStatus == OrderStatus.CANCELLED) {
            auditAction = com.redis.audit.entity.AuditActionType.ORDER_CANCELLED;
        } else if (newStatus == OrderStatus.EXPIRED) {
            auditAction = com.redis.audit.entity.AuditActionType.ORDER_EXPIRED;
        }
        if (auditEventPublisher != null) {
            auditEventPublisher.publish(saved.getUser().getId(), saved.getUser().getEmail(), auditAction, com.redis.audit.entity.AuditStatus.SUCCESS,
                    com.redis.common.entity.ResourceType.ORDER, String.valueOf(saved.getId()), "Order status updated to " + newStatus);
        }

        try {
            if (newStatus == OrderStatus.SHIPPED) {
                notificationEventPublisher.publishOrderShipped(saved.getUser().getId(), saved.getId());
            } else if (newStatus == OrderStatus.DELIVERED) {
                notificationEventPublisher.publishOrderDelivered(saved.getUser().getId(), saved.getId());
            } else if (newStatus == OrderStatus.CANCELLED) {
                notificationEventPublisher.publishOrderCancelled(saved.getUser().getId(), saved.getId());
            } else if (newStatus == OrderStatus.EXPIRED) {
                notificationEventPublisher.publishOrderExpired(saved.getUser().getId(), saved.getId());
            } else if (newStatus == OrderStatus.PROCESSING) {
                notificationEventPublisher.publishOrderProcessing(saved.getUser().getId(), saved.getId());
            }
        } catch (Exception e) {
            log.error("Failed to publish order status change event for order ID: {}", orderId, e);
        }

        return toResponse(saved);
    }

    private boolean isValidTransition(OrderStatus current, OrderStatus next) {
        if (current == next) {
            return true;
        }
        switch (current) {
            case PENDING:
                return next == OrderStatus.PROCESSING || next == OrderStatus.CANCELLED;
            case PENDING_PAYMENT:
                return next == OrderStatus.PROCESSING || next == OrderStatus.PAYMENT_FAILED || next == OrderStatus.EXPIRED || next == OrderStatus.CANCELLED;
            case PAYMENT_FAILED:
                return next == OrderStatus.PENDING_PAYMENT || next == OrderStatus.CANCELLED;
            case EXPIRED:
                return next == OrderStatus.PENDING_PAYMENT;
            case PROCESSING:
                return next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED;
            case SHIPPED:
                return next == OrderStatus.DELIVERED;
            case DELIVERED:
            case CANCELLED:
            default:
                return false;
        }
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .itemId(item.getId())
                        .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .orderId(order.getId())
                .userId(order.getUser().getId())
                .shippingAddress(order.getShippingAddress())
                .notes(order.getNotes())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .items(itemResponses)
                .orderDate(order.getOrderDate())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
