package com.redis.payment.entity;

import com.redis.notification.entity.Notification;
import com.redis.order.entity.Order;
import com.redis.user.entity.Role;
import com.redis.cart.repository.CartRepository;
import com.redis.payment.repository.PaymentRepository;
import com.redis.user.repository.UserRepository;
import com.redis.order.entity.OrderStatus;
import com.redis.notification.repository.NotificationRepository;
import com.redis.notification.entity.MailClient;
import com.redis.payment.service.PaymentService;
import com.redis.order.repository.OrderRepository;
import com.redis.user.entity.User;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.payment.service.gateway.PaymentGateway;
import com.redis.payment.entity.PaymentMethod;
import com.redis.payment.entity.PaymentStatus;
import org.junit.jupiter.api.AfterEach;
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
public class PaymentNotificationIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private MailClient mailClient;

    private User testUser;
    private Order order;
    private Payment payment;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        cartRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("payuser")
                .email("pay@example.com")
                .password("Password123!")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
        testUser = userRepository.save(testUser);

        order = Order.builder()
                .user(testUser)
                .shippingAddress("Addr")
                .totalAmount(new BigDecimal("50.00"))
                .orderDate(LocalDateTime.now())
                .status(com.redis.order.entity.OrderStatus.PENDING_PAYMENT)
                .items(new ArrayList<>())
                .build();
        order = orderRepository.save(order);

        payment = Payment.builder()
                .order(order)
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .status(PaymentStatus.SUCCESS)
                .paymentMethod(PaymentMethod.CARD)
                .paymentGateway(com.redis.payment.entity.PaymentGateway.STRIPE)
                .transactions(new ArrayList<>())
                .refunds(new ArrayList<>())
                .build();
        payment = paymentRepository.save(payment);
    }

    @Test
    void testRefundPaymentTriggersNotification() {
        paymentService.refundPayment(testUser.getId(), payment.getId(), new BigDecimal("10.00"), "Reason", false);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> list = notificationRepository.findAll();
            assertFalse(list.isEmpty());
            assertTrue(list.stream().anyMatch(n -> n.getTitle().contains("Refund")));
        });
    }

    @AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        cartRepository.deleteAll();
        userRepository.deleteAll();
    }
}
