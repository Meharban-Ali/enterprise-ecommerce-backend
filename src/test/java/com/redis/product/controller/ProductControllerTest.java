package com.redis.product.controller;

import com.redis.product.entity.Product;
import com.redis.infrastructure.config.CorsProperties;
import com.redis.infrastructure.security.CustomAccessDeniedHandler;
import com.redis.auth.entity.CustomAuthenticationEntryPoint;
import com.redis.auth.service.JwtService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.product.exception.ProductNotFoundException;
import com.redis.product.dto.request.ProductRequest;
import com.redis.product.dto.response.ProductResponse;
import com.redis.product.service.ProductService;
import com.redis.infrastructure.cache.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@ActiveProfiles("test")
@DisplayName("ProductController Slice Tests")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private RedisUtil redisUtil;

    @MockBean
    private com.redis.auth.service.JwtService jwtService;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @MockBean
    private com.redis.auth.entity.CustomAuthenticationEntryPoint authenticationEntryPoint;

    @MockBean
    private com.redis.infrastructure.security.CustomAccessDeniedHandler accessDeniedHandler;

    @MockBean
    private com.redis.infrastructure.config.CorsProperties corsProperties;

    private ProductRequest validRequest;
    private ProductResponse testResponse;

    @BeforeEach
    void setUp() {
        validRequest = ProductRequest.builder()
                .name("Xbox Series X")
                .price(new BigDecimal("49999.00"))
                .rating(new BigDecimal("4.8"))
                .stockQuantity(25)
                .build();

        testResponse = ProductResponse.builder()
                .id(1L)
                .name("Xbox Series X")
                .price(new BigDecimal("49999.00"))
                .rating(new BigDecimal("4.8"))
                .stockQuantity(25)
                .stockStatus("IN_STOCK")
                .priceFormatted("₹ 49,999.00")
                .isAvailable(true)
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // CREATE ENDPOINT TESTS
    // ═══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/products Tests")
    @WithMockUser(roles = "ADMIN")
    class CreateProductEndpoints {

        @Test
        @DisplayName("✅ Success: Should return 201 Created and response body on valid payload")
        void createProduct_Success() throws Exception {
            when(productService.createProduct(any(ProductRequest.class)))
                    .thenReturn(testResponse);

            mockMvc.perform(post("/api/products")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Product created successfully"))
                    .andExpect(jsonPath("$.data.name").value("Xbox Series X"))
                    .andExpect(jsonPath("$.data.price").value(49999.00));
        }

        @Test
        @DisplayName("❌ Failure: Should return 400 Bad Request on invalid validations")
        void createProduct_ValidationFailure() throws Exception {
            ProductRequest invalidRequest = ProductRequest.builder()
                    .name("") // Blank name - invalid
                    .price(new BigDecimal("-10.00")) // Negative price - invalid
                    .rating(new BigDecimal("6.0")) // Greater than 5.0 - invalid
                    .stockQuantity(-5) // Negative stock - invalid
                    .build();

            mockMvc.perform(post("/api/products")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors").isArray());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // READ BY ID ENDPOINT TESTS
    // ═══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("GET /api/products/{id} Tests")
    @WithMockUser(roles = "USER")
    class GetProductByIdEndpoints {

        @Test
        @DisplayName("✅ Success: Cache Miss - Should return 200 OK from DB when not in Redis cache")
        void getProductById_CacheMiss() throws Exception {
            when(redisUtil.exists("product::1")).thenReturn(false);
            when(productService.getProductById(1L)).thenReturn(testResponse);

            mockMvc.perform(get("/api/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Product fetched from database"))
                    .andExpect(jsonPath("$.fromCache").value(false))
                    .andExpect(jsonPath("$.data.name").value("Xbox Series X"));
        }

        @Test
        @DisplayName("✅ Success: Cache Hit - Should return 200 OK from Redis when cached")
        void getProductById_CacheHit() throws Exception {
            when(redisUtil.exists("product::1")).thenReturn(true);
            when(productService.getProductById(1L)).thenReturn(testResponse);

            mockMvc.perform(get("/api/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Product fetched from Redis cache"))
                    .andExpect(jsonPath("$.fromCache").value(true))
                    .andExpect(jsonPath("$.data.name").value("Xbox Series X"));
        }

        @Test
        @DisplayName("❌ Failure: Should return 404 Not Found when product does not exist")
        void getProductById_NotFound() throws Exception {
            when(productService.getProductById(999L))
                    .thenThrow(new ProductNotFoundException(999L));

            mockMvc.perform(get("/api/products/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // READ ALL ENDPOINT TESTS
    // ═══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("GET /api/products Tests")
    @WithMockUser(roles = "USER")
    class GetAllProductsEndpoints {

        @Test
        @DisplayName("✅ Success: Should return paginated products list")
        void getAllProducts_Success() throws Exception {
            PageImpl<ProductResponse> page = new PageImpl<>(List.of(testResponse), PageRequest.of(0, 10), 1);
            when(productService.getAllProducts(any(Pageable.class))).thenReturn(page);
            when(redisUtil.exists("products::page_0_size_10")).thenReturn(false);

            mockMvc.perform(get("/api/products?page=0&size=10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].name").value("Xbox Series X"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // UPDATE ENDPOINT TESTS
    // ═══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("PUT /api/products/{id} Tests")
    @WithMockUser(roles = "ADMIN")
    class UpdateProductEndpoints {

        @Test
        @DisplayName("✅ Success: Should return 200 OK on valid payload update")
        void updateProduct_Success() throws Exception {
            when(productService.updateProduct(eq(1L), any(ProductRequest.class)))
                    .thenReturn(testResponse);

            mockMvc.perform(put("/api/products/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Product updated and cache refreshed"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // DELETE ENDPOINT TESTS
    // ═══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("DELETE /api/products/{id} Tests")
    @WithMockUser(roles = "ADMIN")
    class DeleteProductEndpoints {

        @Test
        @DisplayName("✅ Success: Should return 200 OK on successful deletion")
        void deleteProduct_Success() throws Exception {
            mockMvc.perform(delete("/api/products/1").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Product deleted and cache evicted"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // SEARCH & PRICE RANGE ENDPOINT TESTS
    // ═══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Search & Filters Tests")
    @WithMockUser(roles = "USER")
    class SearchAndFiltersEndpoints {

        @Test
        @DisplayName("✅ Success: Should search products by name")
        void searchProducts_Success() throws Exception {
            PageImpl<ProductResponse> page = new PageImpl<>(List.of(testResponse), PageRequest.of(0, 10), 1);
            when(productService.searchProductsByName(eq("Xbox"), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/products/search?name=Xbox"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].name").value("Xbox Series X"));
        }

        @Test
        @DisplayName("✅ Success: Should fetch products by price range")
        void getByPriceRange_Success() throws Exception {
            PageImpl<ProductResponse> page = new PageImpl<>(List.of(testResponse), PageRequest.of(0, 10), 1);
            when(productService.getProductsByPriceRange(any(BigDecimal.class), any(BigDecimal.class), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/products/price-range?minPrice=1000&maxPrice=100000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // STOCK & RATING ENDPOINT TESTS
    // ═══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Stock & Rating Endpoints")
    @WithMockUser(roles = "USER")
    class StockAndRatingEndpoints {

        @Test
        @DisplayName("✅ Success: Should return products with min rating")
        void getByMinRating_Success() throws Exception {
            when(productService.getProductsByMinRating(any(BigDecimal.class)))
                    .thenReturn(List.of(testResponse));

            mockMvc.perform(get("/api/products/rating?minRating=4.5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].name").value("Xbox Series X"));
        }

        @Test
        @DisplayName("✅ Success: Should return low stock products")
        void getLowStock_Success() throws Exception {
            when(productService.getLowStockProducts(10)).thenReturn(List.of(testResponse));

            mockMvc.perform(get("/api/products/low-stock?threshold=10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("✅ Success: Should return out of stock products")
        void getOutOfStock_Success() throws Exception {
            when(productService.getOutOfStockProducts()).thenReturn(List.of(testResponse));

            mockMvc.perform(get("/api/products/out-of-stock"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CACHE MANAGEMENT ENDPOINT TESTS
    // ═══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("DELETE /api/products/cache Tests")
    @WithMockUser(roles = "ADMIN")
    class CacheClearEndpoints {

        @Test
        @DisplayName("✅ Success: Should clear cache and return 200 OK")
        void clearCache_Success() throws Exception {
            when(productService.clearProductCache()).thenReturn(true);

            mockMvc.perform(delete("/api/products/cache").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("All product caches cleared"));
        }

        @Test
        @DisplayName("❌ Failure: Should return 500 when clear fails")
        void clearCache_Failure() throws Exception {
            when(productService.clearProductCache()).thenReturn(false);

            mockMvc.perform(delete("/api/products/cache").with(csrf()))
                    .andExpect(status().is5xxServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("CACHE_CLEAR_FAILED"));
        }
    }
}
