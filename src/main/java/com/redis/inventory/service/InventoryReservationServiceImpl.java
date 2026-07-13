package com.redis.inventory.service;

import com.redis.audit.entity.AuditActionType;
import com.redis.notification.event.NotificationEventPublisher;
import com.redis.common.entity.ResourceType;
import com.redis.audit.event.AuditEventPublisher;
import com.redis.audit.entity.AuditStatus;

import com.redis.infrastructure.config.NotificationProperties;
import com.redis.order.entity.Order;
import com.redis.order.entity.OrderItem;
import com.redis.product.entity.Product;
import com.redis.user.entity.Role;
import com.redis.user.entity.User;
import com.redis.product.repository.ProductRepository;
import com.redis.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryReservationServiceImpl implements InventoryReservationService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final NotificationProperties notificationProperties;
    private final NotificationEventPublisher notificationEventPublisher;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.redis.audit.event.AuditEventPublisher auditEventPublisher;

    @Override
    @Transactional
    public void reserveInventory(Order order) {
        log.info("Reserving inventory for order ID: {}", order.getId());
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product '" + product.getName() + "'. Available: " + product.getStockQuantity());
            }
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            Product savedProduct = productRepository.save(product);
            Product productToNotify = (savedProduct != null) ? savedProduct : product;

            // Check stock level and trigger notifications for administrators
            try {
                checkAndTriggerStockNotifications(productToNotify);
            } catch (Exception e) {
                log.error("Failed to process stock notifications for product: {}", productToNotify.getName(), e);
            }
        }
        log.info("Inventory reserved successfully for order ID: {}", order.getId());
        if (auditEventPublisher != null) {
            Long userId = order.getUser() != null ? order.getUser().getId() : null;
            String email = order.getUser() != null ? order.getUser().getEmail() : null;
            auditEventPublisher.publish(userId, email, com.redis.audit.entity.AuditActionType.INVENTORY_RESERVED, com.redis.audit.entity.AuditStatus.SUCCESS,
                    com.redis.common.entity.ResourceType.INVENTORY, String.valueOf(order.getId()), "Inventory reserved for order ID: " + order.getId());
        }
    }

    private void checkAndTriggerStockNotifications(Product product) {
        int threshold = notificationProperties.getLowStockThreshold();
        int stock = product.getStockQuantity();

        if (stock == 0) {
            log.warn("Product is OUT OF STOCK: {}", product.getName());
            List<User> admins = userRepository.findByRole(Role.ROLE_ADMIN);
            for (User admin : admins) {
                notificationEventPublisher.publishOutOfStock(admin.getId(), product.getName());
            }
        } else if (stock <= threshold) {
            log.warn("Product stock level is LOW: {} (current: {})", product.getName(), stock);
            List<User> admins = userRepository.findByRole(Role.ROLE_ADMIN);
            for (User admin : admins) {
                notificationEventPublisher.publishLowStock(admin.getId(), product.getName(), stock);
            }
        }
    }

    @Override
    @Transactional
    public void releaseReservation(Order order) {
        log.info("Releasing reserved inventory for order ID: {}", order.getId());
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }
        log.info("Inventory released successfully for order ID: {}", order.getId());
        if (auditEventPublisher != null) {
            Long userId = order.getUser() != null ? order.getUser().getId() : null;
            String email = order.getUser() != null ? order.getUser().getEmail() : null;
            auditEventPublisher.publish(userId, email, com.redis.audit.entity.AuditActionType.INVENTORY_RELEASED, com.redis.audit.entity.AuditStatus.SUCCESS,
                    com.redis.common.entity.ResourceType.INVENTORY, String.valueOf(order.getId()), "Inventory reservation released for order ID: " + order.getId());
        }
    }

    @Override
    @Transactional
    public void commitReservation(Order order) {
        log.info("Finalizing/committing inventory reservation for order ID: {}", order.getId());
        // Option B (Stock reservation): stock already decremented at placeOrder. Confirm success log.
    }
}
