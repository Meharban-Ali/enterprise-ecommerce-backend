package com.redis.product;

import com.redis.product.entity.Product;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.product.dto.request.ProductRequest;
import com.redis.common.dto.ApiResponse;
import com.redis.product.dto.response.ProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@DisplayName("E-Commerce Product API Integration Tests")
class ProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("✅ Success: Verify complete CRUD flow of a Product")
    void performProductCRUDLifecycle() throws Exception {

        // ─── STEP 1: CREATE PRODUCT ──────────────────────────────────────────
        ProductRequest createRequest = ProductRequest.builder()
                .name("PlayStation 5")
                .price(new BigDecimal("49999.00"))
                .rating(new BigDecimal("4.9"))
                .stockQuantity(15)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("PlayStation 5"))
                .andReturn();

        // Extract ID from created response using JsonPath
        String responseBody = createResult.getResponse().getContentAsString();
        Number idNumber = com.jayway.jsonpath.JsonPath.read(responseBody, "$.data.id");
        Long productId = idNumber.longValue();
        assertThat(productId).isNotNull();

        // ─── STEP 2: GET PRODUCT BY ID ───────────────────────────────────────
        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(productId))
                .andExpect(jsonPath("$.data.name").value("PlayStation 5"))
                .andExpect(jsonPath("$.data.price").value(49999.00));

        // ─── STEP 3: UPDATE PRODUCT ──────────────────────────────────────────
        ProductRequest updateRequest = ProductRequest.builder()
                .name("PlayStation 5 Slim")
                .price(new BigDecimal("44999.00")) // Updated price
                .rating(new BigDecimal("4.9"))
                .stockQuantity(20) // Updated stock
                .build();

        mockMvc.perform(put("/api/products/" + productId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("PlayStation 5 Slim"))
                .andExpect(jsonPath("$.data.price").value(44999.00))
                .andExpect(jsonPath("$.data.stockQuantity").value(20));

        // ─── STEP 4: SEARCH PRODUCT BY NAME ──────────────────────────────────
        mockMvc.perform(get("/api/products/search?name=PlayStation&page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(productId))
                .andExpect(jsonPath("$.data.content[0].name").value("PlayStation 5 Slim"));

        // ─── STEP 5: DELETE PRODUCT ──────────────────────────────────────────
        mockMvc.perform(delete("/api/products/" + productId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product deleted and cache evicted"));

        // ─── STEP 6: VERIFY DELETION (GET RETURNS 404) ────────────────────────
        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));
    }
}
