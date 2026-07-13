package com.redis.common.service;

import com.redis.analytics.service.AnalyticsServiceImpl;

import com.redis.product.repository.ProductRepository;
import com.redis.product.entity.Product;
import com.redis.order.entity.Order;
import com.redis.cart.service.CartService;
import com.redis.user.entity.Role;
import com.redis.cart.dto.response.CartResponse;
import com.redis.inventory.service.InventoryReservationService;
import com.redis.cart.repository.CartRepository;
import com.redis.cart.service.WishlistServiceImpl;
import com.redis.order.dto.response.OrderResponse;
import com.redis.category.entity.Category;
import com.redis.user.repository.UserRepository;
import com.redis.category.repository.CategoryRepository;
import com.redis.cart.dto.response.WishlistResponse;
import com.redis.cart.entity.Wishlist;
import com.redis.cart.repository.WishlistRepository;
import com.redis.cart.service.CartServiceImpl;
import com.redis.cart.entity.CartItem;
import com.redis.cart.entity.Cart;
import com.redis.order.repository.OrderRepository;
import com.redis.category.service.CategoryServiceImpl;
import com.redis.order.service.OrderServiceImpl;
import com.redis.user.entity.User;
import com.redis.category.dto.response.CategoryResponse;

import com.redis.product.exception.ProductNotFoundException;
import com.redis.cart.dto.request.CartItemRequest;
import com.redis.category.dto.request.CategoryRequest;
import com.redis.order.dto.request.OrderRequest;
import com.redis.order.dto.request.OrderStatusUpdateRequest;
import com.redis.order.entity.OrderStatus;
import com.redis.product.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ECommerceFeatureExpansionTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private CartRepository cartRepository;
    @Mock private WishlistRepository wishlistRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductMapper productMapper;
    @Mock private CartService cartService;
    @Mock private InventoryReservationService inventoryReservationService;

    @InjectMocks private CategoryServiceImpl categoryService;
    @InjectMocks private CartServiceImpl cartServiceImplementation;
    @InjectMocks private WishlistServiceImpl wishlistService;
    @InjectMocks private OrderServiceImpl orderService;
    @InjectMocks private AnalyticsServiceImpl analyticsService;

    private User testUser;
    private Product testProduct;
    private Category testCategory;
    private Cart testCart;
    private Wishlist testWishlist;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("testuser@ecommerce.local")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();

        testCategory = Category.builder()
                .id(10L)
                .name("Electronics")
                .description("Electronic gadgets")
                .products(new ArrayList<>())
                .build();

        testProduct = Product.builder()
                .id(100L)
                .name("Smartphone")
                .price(new BigDecimal("9999.00"))
                .rating(new BigDecimal("4.5"))
                .stockQuantity(50)
                .category(testCategory)
                .build();

        testCategory.getProducts().add(testProduct);

        testCart = Cart.builder()
                .id(1000L)
                .user(testUser)
                .items(new ArrayList<>())
                .build();

        testWishlist = Wishlist.builder()
                .id(2000L)
                .user(testUser)
                .products(new ArrayList<>())
                .build();
    }

    // ─── Category Service Tests ──────────────────────────────────────────────────
    @Test
    void createCategory_Success() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Home Appliances")
                .description("Devices for home")
                .build();

        when(categoryRepository.findByNameIgnoreCase(any())).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(11L);
            return c;
        });

        CategoryResponse response = categoryService.createCategory(request);

        assertNotNull(response);
        assertEquals(11L, response.getId());
        assertEquals("Home Appliances", response.getName());
        verify(categoryRepository, times(1)).save(any());
    }

    @Test
    void createCategory_DuplicateName_ThrowsException() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Electronics")
                .build();

        when(categoryRepository.findByNameIgnoreCase("Electronics")).thenReturn(Optional.of(testCategory));

        assertThrows(IllegalArgumentException.class, () -> categoryService.createCategory(request));
        verify(categoryRepository, never()).save(any());
    }

    // ─── Cart Service Tests ──────────────────────────────────────────────────────
    @Test
    void getCart_Success() {
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));

        CartResponse response = cartServiceImplementation.getCart(1L);

        assertNotNull(response);
        assertEquals(1000L, response.getCartId());
        assertEquals(1L, response.getUserId());
        assertTrue(response.getItems().isEmpty());
    }

    @Test
    void addItem_NewProduct_Success() {
        CartItemRequest request = CartItemRequest.builder()
                .productId(100L)
                .quantity(2)
                .build();

        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        CartResponse response = cartServiceImplementation.addItem(1L, request);

        assertNotNull(response);
        assertEquals(1, testCart.getItems().size());
        assertEquals(2, testCart.getItems().get(0).getQuantity());
        verify(cartRepository, times(1)).save(any());
    }

    @Test
    void addItem_ExistingProduct_MergeSuccess() {
        CartItem existingItem = CartItem.builder()
                .cart(testCart)
                .product(testProduct)
                .quantity(2)
                .build();
        testCart.getItems().add(existingItem);

        CartItemRequest request = CartItemRequest.builder()
                .productId(100L)
                .quantity(3)
                .build();

        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        CartResponse response = cartServiceImplementation.addItem(1L, request);

        assertNotNull(response);
        assertEquals(1, testCart.getItems().size());
        assertEquals(5, testCart.getItems().get(0).getQuantity());
        verify(cartRepository, times(1)).save(any());
    }

    @Test
    void addItem_MergeExceedingStock_ThrowsException() {
        CartItem existingItem = CartItem.builder()
                .cart(testCart)
                .product(testProduct)
                .quantity(48)
                .build();
        testCart.getItems().add(existingItem);

        CartItemRequest request = CartItemRequest.builder()
                .productId(100L)
                .quantity(5) // total 53, exceeds stock 50
                .build();

        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

        assertThrows(RuntimeException.class, () -> cartServiceImplementation.addItem(1L, request));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addItem_InvalidQuantity_ThrowsException() {
        CartItemRequest request = CartItemRequest.builder()
                .productId(100L)
                .quantity(0)
                .build();

        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

        assertThrows(RuntimeException.class, () -> cartServiceImplementation.addItem(1L, request));
        verify(cartRepository, never()).save(any());
    }

    // ─── Wishlist Service Tests ──────────────────────────────────────────────────
    @Test
    void getWishlist_Success() {
        when(wishlistRepository.findByUserIdWithProducts(1L)).thenReturn(Optional.of(testWishlist));

        WishlistResponse response = wishlistService.getWishlist(1L);

        assertNotNull(response);
        assertEquals(2000L, response.getWishlistId());
        assertEquals(1L, response.getUserId());
    }

    @Test
    void addProductToWishlist_Success() {
        when(wishlistRepository.findByUserIdWithProducts(1L)).thenReturn(Optional.of(testWishlist));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(wishlistRepository.save(any(Wishlist.class))).thenReturn(testWishlist);

        WishlistResponse response = wishlistService.addProduct(1L, 100L);

        assertNotNull(response);
        verify(wishlistRepository, times(1)).save(any());
    }

    // ─── Order Service Tests ─────────────────────────────────────────────────────
    @Test
    void placeOrder_Success() {
        // Prepare cart item
        CartItem cartItem = CartItem.builder()
                .id(500L)
                .cart(testCart)
                .product(testProduct)
                .quantity(2)
                .build();
        testCart.getItems().add(cartItem);

        OrderRequest request = OrderRequest.builder()
                .shippingAddress("123 Main St, New Delhi, India")
                .notes("Deliver in evening")
                .build();

        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(99L);
            return o;
        });

        OrderResponse response = orderService.placeOrder(1L, request);

        assertNotNull(response);
        assertEquals(99L, response.getOrderId());
        assertEquals(1L, response.getUserId());
        assertEquals("PENDING_PAYMENT", response.getStatus());
        verify(inventoryReservationService, times(1)).reserveInventory(any(Order.class));
    }

    @Test
    void placeOrder_OutOfStock_ThrowsException() {
        CartItem cartItem = CartItem.builder()
                .id(500L)
                .cart(testCart)
                .product(testProduct)
                .quantity(100) // exceeds 50 available stock
                .build();
        testCart.getItems().add(cartItem);

        OrderRequest request = OrderRequest.builder()
                .shippingAddress("123 Main St, New Delhi, India")
                .build();

        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new IllegalArgumentException("Insufficient stock"))
                .when(inventoryReservationService).reserveInventory(any(Order.class));

        assertThrows(IllegalArgumentException.class, () -> orderService.placeOrder(1L, request));
    }
}