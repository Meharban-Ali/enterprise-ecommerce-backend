package com.redis.notification.entity;

import com.redis.cart.entity.Cart;
import com.redis.cart.entity.CartItem;
import com.redis.cart.repository.CartRepository;
import com.redis.common.entity.OutboxStatus;
import com.redis.infrastructure.config.NotificationProperties;
import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.notification.repository.NotificationOutboxRepository;
import com.redis.notification.repository.NotificationRepository;
import com.redis.notification.service.EmailNotificationService;
import com.redis.order.dto.request.OrderRequest;
import com.redis.order.dto.response.OrderResponse;
import com.redis.order.entity.Order;
import com.redis.order.repository.OrderRepository;
import com.redis.order.service.OrderService;
import com.redis.payment.entity.Payment;
import com.redis.payment.entity.PaymentGateway;
import com.redis.payment.entity.PaymentMethod;
import com.redis.payment.entity.PaymentStatus;
import com.redis.payment.entity.Refund;
import com.redis.payment.entity.RefundStatus;
import com.redis.payment.repository.PaymentRepository;
import com.redis.payment.repository.RefundRepository;
import com.redis.product.entity.Product;
import com.redis.product.repository.ProductRepository;
import com.redis.user.entity.Role;
import com.redis.user.entity.User;
import com.redis.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationEdgeCasesIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationOutboxRepository outboxRepository;

    @Autowired
    private EmailNotificationService emailNotificationService;

    @Autowired
    private NotificationProperties notificationProperties;

    @MockBean
    private MailClient mailClient;

    private User testUser;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        notificationProperties.setTemplateManagementEnabled(false);
        Mockito.reset(mailClient);
        Mockito.doNothing().when(mailClient).sendEmail(anyString(), anyString(), anyString(), anyBoolean());

        outboxRepository.deleteAll();
        notificationRepository.deleteAll();
        refundRepository.deleteAll();
        paymentRepository.deleteAll();
        cartRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("edgeuser")
                .email("edge@example.com")
                .password("Password123!")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
        testUser = userRepository.save(testUser);

        product1 = Product.builder()
                .name("Pro Laptop")
                .price(new BigDecimal("1000.00"))
                .rating(new BigDecimal("4.8"))
                .stockQuantity(10)
                .build();
        product1 = productRepository.save(product1);

        product2 = Product.builder()
                .name("Wireless Mouse")
                .price(new BigDecimal("50.00"))
                .rating(new BigDecimal("4.5"))
                .stockQuantity(20)
                .build();
        product2 = productRepository.save(product2);
    }

    @Test
    @Transactional
    void testMultipleLineItemsOrderNotification() {
        Cart cart = Cart.builder()
                .user(testUser)
                .items(new ArrayList<>())
                .build();
        cart.getItems().add(CartItem.builder().cart(cart).product(product1).quantity(1).build());
        cart.getItems().add(CartItem.builder().cart(cart).product(product2).quantity(2).build());
        cartRepository.save(cart);

        OrderResponse response = orderService.placeOrder(testUser.getId(), OrderRequest.builder().shippingAddress("123 Main St").build());
        assertNotNull(response);

        Order order = orderRepository.findById(response.getOrderId()).orElseThrow();
        assertEquals(2, order.getItems().size());
        assertEquals(new BigDecimal("1100.00"), order.getTotalAmount());

        Notification notification = Notification.builder()
                .user(testUser)
                .title("Order Placed")
                .message("Your order has been placed")
                .type(NotificationType.ORDER)
                .channel(NotificationChannel.EMAIL)
                .priority(NotificationPriority.MEDIUM)
                .status(NotificationStatus.PENDING)
                .referenceEntityId(order.getId())
                .referenceEntityType("ORDER")
                .build();
        notification = notificationRepository.save(notification);

        emailNotificationService.send(notification);
        assertEquals(NotificationStatus.SENT, notification.getStatus());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(mailClient).sendEmail(anyString(), anyString(), bodyCaptor.capture(), anyBoolean());
        String body = bodyCaptor.getValue();
        assertTrue(body.contains("Pro Laptop"), "Email body must contain product name 'Pro Laptop'");
        assertTrue(body.contains("Wireless Mouse"), "Email body must contain product name 'Wireless Mouse'");
        assertTrue(body.contains("1100.00"), "Email body must contain correct total amount '1100.00'");
        assertTrue(body.contains("edgeuser"), "Email body must contain actual customer username 'edgeuser'");
    }

    @Test
    @Transactional
    void testPartialRefundNotification() {
        Cart cart = Cart.builder()
                .user(testUser)
                .items(new ArrayList<>())
                .build();
        cart.getItems().add(CartItem.builder().cart(cart).product(product1).quantity(1).build());
        cartRepository.save(cart);

        OrderResponse response = orderService.placeOrder(testUser.getId(), OrderRequest.builder().shippingAddress("123 Main St").build());
        Order order = orderRepository.findById(response.getOrderId()).orElseThrow();

        Payment payment = Payment.builder()
                .order(order)
                .amount(new BigDecimal("1000.00"))
                .currency("USD")
                .paymentMethod(PaymentMethod.CARD)
                .paymentGateway(PaymentGateway.STRIPE)
                .status(PaymentStatus.SUCCESS)
                .build();
        payment = paymentRepository.save(payment);

        Refund refund = Refund.builder()
                .payment(payment)
                .amount(new BigDecimal("200.00"))
                .status(RefundStatus.SUCCESS)
                .reason("Customer request - partial refund")
                .build();
        refund = refundRepository.save(refund);

        Notification notification = Notification.builder()
                .user(testUser)
                .title("Refund Successful")
                .message("Partial refund of $200.00 processed.")
                .type(NotificationType.PAYMENT)
                .channel(NotificationChannel.EMAIL)
                .priority(NotificationPriority.HIGH)
                .status(NotificationStatus.PENDING)
                .referenceEntityId(refund.getId())
                .referenceEntityType("REFUND")
                .build();
        notification = notificationRepository.save(notification);

        emailNotificationService.send(notification);
        assertEquals(NotificationStatus.SENT, notification.getStatus());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(mailClient).sendEmail(anyString(), anyString(), bodyCaptor.capture(), anyBoolean());
        String body = bodyCaptor.getValue();
        assertTrue(body.contains("200.00"), "Email body must contain actual partial refund amount '200.00' from Refund record, NOT original payment total '1000.00'");
        assertTrue(body.contains("Refund"), "Email body must reference refund");
        assertTrue(body.contains("edgeuser"), "Email body must contain actual customer username 'edgeuser'");
    }

    @Test
    @Transactional
    void testNotificationForDeletedOrNonExistentOrder() {
        Notification notification = Notification.builder()
                .user(testUser)
                .title("Order Placed")
                .message("Your order #999999 has been placed")
                .type(NotificationType.ORDER)
                .channel(NotificationChannel.EMAIL)
                .priority(NotificationPriority.MEDIUM)
                .status(NotificationStatus.PENDING)
                .referenceEntityId(999999L)
                .referenceEntityType("ORDER")
                .build();
        notification = notificationRepository.save(notification);

        Notification finalNotification = notification;
        assertDoesNotThrow(() -> {
            emailNotificationService.send(finalNotification);
            assertEquals(NotificationStatus.SENT, finalNotification.getStatus());
        });

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(mailClient).sendEmail(anyString(), anyString(), bodyCaptor.capture(), anyBoolean());
        String body = bodyCaptor.getValue();
        assertTrue(body.contains("Processing"), "Email body must contain fallback processing message when order is missing");
        assertFalse(body.contains("$0.00"), "Email body must NOT contain fabricated $0.00 total amount");
    }

    @Test
    @Transactional
    void testPasswordResetNotificationWithStructuredActionUrl() {
        String expectedResetUrl = "https://ecommerce.com/reset?token=secure-token-98765";
        Notification notification = Notification.builder()
                .user(testUser)
                .title("Password Reset Requested")
                .message("Please click the link to reset your password.")
                .type(NotificationType.SECURITY)
                .channel(NotificationChannel.EMAIL)
                .priority(NotificationPriority.HIGH)
                .status(NotificationStatus.PENDING)
                .actionUrl(expectedResetUrl)
                .referenceEntityType("SECURITY")
                .build();
        notification = notificationRepository.save(notification);

        emailNotificationService.send(notification);
        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertEquals(expectedResetUrl, notification.getActionUrl());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(mailClient).sendEmail(anyString(), anyString(), bodyCaptor.capture(), anyBoolean());
        String body = bodyCaptor.getValue();
        assertTrue(body.contains(expectedResetUrl), "Email body must contain exact structured reset URL: " + expectedResetUrl);
        assertTrue(body.contains("edgeuser"), "Email body must contain actual customer username 'edgeuser'");
    }

    @Test
    void testConcurrentOutboxClaimRaceCondition() throws Exception {
        NotificationOutbox outbox = NotificationOutbox.builder()
                .eventId(UUID.randomUUID())
                .aggregateType("Notification")
                .aggregateId("100")
                .eventType("OrderNotificationEvent")
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .build();
        outbox = outboxRepository.save(outbox);

        final Long outboxId = outbox.getId();
        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    int claimed = outboxRepository.claimEvent(outboxId);
                    if (claimed > 0) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception ignored) {}
            });
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        assertEquals(1, successCount.get(), "Exactly ONE thread should successfully claim the atomic outbox event!");
    }

    @Test
    @Transactional
    void testPasswordChangedEmail_RendersCorrectContentAndSupportContact() {
        Notification notification = Notification.builder()
                .user(testUser)
                .title("Password Changed Successfully")
                .message("Your account password has been updated. If you did not make this change, contact support immediately.")
                .type(NotificationType.SECURITY)
                .channel(NotificationChannel.EMAIL)
                .priority(NotificationPriority.HIGH)
                .status(NotificationStatus.PENDING)
                .build();
        notification = notificationRepository.save(notification);

        Mockito.reset(mailClient);
        Mockito.doNothing().when(mailClient).sendEmail(anyString(), anyString(), anyString(), anyBoolean());

        emailNotificationService.send(notification);

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(mailClient).sendEmail(emailCaptor.capture(), subjectCaptor.capture(), bodyCaptor.capture(), anyBoolean());

        assertEquals("edge@example.com", emailCaptor.getValue());
        assertEquals("Password Changed Successfully", subjectCaptor.getValue());

        String renderedHtml = bodyCaptor.getValue();
        assertNotNull(renderedHtml);
        assertTrue(renderedHtml.contains("edgeuser"), "Email body must contain customer username");
        assertTrue(renderedHtml.contains("Password Changed Successfully"), "Email body must contain title");
        assertTrue(renderedHtml.contains("Security Notice:"), "Email body must contain Security Notice header");
        assertTrue(renderedHtml.contains("If you did not make this change, please contact support immediately"), "Email body must contain support notice text");
        assertTrue(renderedHtml.contains("supportecommerces@gmail.com"), "Email body must contain correct supportContact email");
    }

    @AfterEach
    void tearDown() {
        outboxRepository.deleteAll();
        notificationRepository.deleteAll();
        refundRepository.deleteAll();
        paymentRepository.deleteAll();
        cartRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }
}
