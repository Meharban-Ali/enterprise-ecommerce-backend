package com.redis.inventory.service;

import com.redis.order.entity.Order;
import com.redis.order.entity.OrderItem;
import com.redis.product.entity.Product;
import com.redis.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InventoryReservationServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private InventoryReservationServiceImpl inventoryReservationService;

    private Product testProduct;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(new BigDecimal("10.00"))
                .stockQuantity(10)
                .build();

        OrderItem orderItem = OrderItem.builder()
                .product(testProduct)
                .quantity(3)
                .build();

        testOrder = Order.builder()
                .id(100L)
                .items(new ArrayList<>(Collections.singletonList(orderItem)))
                .build();
    }

    @Test
    void testReserveInventorySuccess() {
        inventoryReservationService.reserveInventory(testOrder);
        assertEquals(7, testProduct.getStockQuantity());
        verify(productRepository, times(1)).save(testProduct);
    }

    @Test
    void testReserveInventoryInsufficientStock() {
        testOrder.getItems().get(0).setQuantity(15);
        assertThrows(IllegalArgumentException.class, () -> inventoryReservationService.reserveInventory(testOrder));
        assertEquals(10, testProduct.getStockQuantity());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testReleaseReservationSuccess() {
        inventoryReservationService.releaseReservation(testOrder);
        assertEquals(13, testProduct.getStockQuantity());
        verify(productRepository, times(1)).save(testProduct);
    }
}
