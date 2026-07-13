package com.redis.order.entity;

import com.redis.product.repository.ProductRepository;
import com.redis.product.entity.Product;
import com.redis.notification.entity.Notification;
import com.redis.user.entity.Role;
import com.redis.cart.repository.CartRepository;
import com.redis.user.repository.UserRepository;
import com.redis.notification.repository.NotificationRepository;
import com.redis.notification.entity.MailClient;
import com.redis.cart.entity.CartItem;
import com.redis.cart.entity.Cart;
import com.redis.order.service.OrderService;
import com.redis.user.entity.User;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.order.dto.request.OrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class OrderNotificationIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private MailClient mailClient;

    private User testUser;
    private Product product;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        cartRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("orderuser")
                .email("order@example.com")
                .password("Password123!")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
        testUser = userRepository.save(testUser);

        product = Product.builder()
                .name("Test Product")
                .price(new BigDecimal("10.00"))
                .rating(new BigDecimal("4.5"))
                .stockQuantity(100)
                .build();
        product = productRepository.save(product);

        Cart cart = Cart.builder()
                .user(testUser)
                .items(new ArrayList<>())
                .build();
        CartItem item = CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(2)
                .build();
        cart.getItems().add(item);
        cartRepository.save(cart);
    }

    @Test
    void testPlaceOrderTriggersNotification() {
        OrderRequest request = OrderRequest.builder()
                .shippingAddress("123 Main St")
                .notes("Deliver to front desk")
                .build();

        orderService.placeOrder(testUser.getId(), request);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> list = notificationRepository.findAll();
            assertFalse(list.isEmpty());
            assertTrue(list.stream().anyMatch(n -> n.getTitle().contains("Order Placed Successfully")));
        });
    }
}
