package com.redis.notification.entity;

import com.redis.product.entity.Product;
import com.redis.order.entity.Order;
import com.redis.user.entity.Role;
import com.redis.inventory.service.InventoryReservationService;
import com.redis.order.entity.OrderStatus;
import com.redis.user.entity.User;
import com.redis.order.entity.OrderItem;

import com.redis.infrastructure.config.NotificationProperties;
import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.cart.repository.CartRepository;
import com.redis.notification.repository.NotificationRepository;
import com.redis.product.repository.ProductRepository;
import com.redis.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class InventoryNotificationIntegrationTest {

    @Autowired
    private InventoryReservationService inventoryReservationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationProperties notificationProperties;

    @MockBean
    private MailClient mailClient;

    private User adminUser;
    private Product product;
    private Order order;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        cartRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        // Create Admin User
        adminUser = User.builder()
                .username("adminuser")
                .email("admin@ecommerce.com")
                .password("Password123!")
                .role(Role.ROLE_ADMIN)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
        adminUser = userRepository.save(adminUser);

        // Product with stock just above threshold (e.g. 6, where threshold is 5)
        product = Product.builder()
                .name("Low Stock Prod")
                .price(new BigDecimal("10.00"))
                .rating(new BigDecimal("4.5"))
                .stockQuantity(6)
                .build();
        product = productRepository.save(product);

        // Order that consumes 2 quantities, bringing stock down to 4 (which is below threshold 5)
        order = Order.builder()
                .user(adminUser)
                .shippingAddress("Addr")
                .totalAmount(new BigDecimal("20.00"))
                .orderDate(LocalDateTime.now())
                .status(com.redis.order.entity.OrderStatus.PENDING_PAYMENT)
                .items(new ArrayList<>())
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(product)
                .productName(product.getName())
                .quantity(2)
                .unitPrice(product.getPrice())
                .subtotal(product.getPrice().multiply(new BigDecimal("2")))
                .build();
        order.getItems().add(item);
    }

    @Test
    void testInventoryDeductionTriggersLowStockAdminNotification() {
        // Assert low stock threshold value from properties
        assertTrue(notificationProperties.getLowStockThreshold() >= 5);

        inventoryReservationService.reserveInventory(order);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> list = notificationRepository.findAll();
            assertFalse(list.isEmpty());
            assertTrue(list.stream().anyMatch(n -> n.getTitle().contains("LOW STOCK WARNING")));
        });
    }
}
