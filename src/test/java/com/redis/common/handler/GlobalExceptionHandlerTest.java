package com.redis.common.handler;

import com.redis.product.entity.Product;
import com.redis.product.exception.ProductNotFoundException;
import com.redis.common.handler.GlobalExceptionHandler;
import com.redis.product.exception.ProductDuplicateException;
import com.redis.auth.entity.CustomAuthenticationEntryPoint;
import com.redis.auth.service.JwtService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.common.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.hamcrest.CoreMatchers.containsString;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.DummyController.class)
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@WithMockUser
@DisplayName("GlobalExceptionHandler Advice Tests")
class GlobalExceptionHandlerTest {

    @MockBean
    private com.redis.auth.service.JwtService jwtService;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @MockBean
    private com.redis.auth.entity.CustomAuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ═══════════════════════════════════════════════════════════
    // DUMMY TEST CONTROLLER & DTOs
    // ═══════════════════════════════════════════════════════════
    @RestController
    @RequestMapping("/test-exceptions")
    static class DummyController {

        @GetMapping("/not-found")
        public void throwNotFound() {
            throw new ProductNotFoundException(42L);
        }

        @GetMapping("/duplicate")
        public void throwDuplicate() {
            throw new ProductDuplicateException("Pixel 8");
        }

        @GetMapping("/illegal-argument")
        public void throwIllegalArgument() {
            throw new IllegalArgumentException("Invalid configuration parameter");
        }

        @GetMapping("/generic")
        public void throwGeneric() {
            throw new RuntimeException("Something unexpected broke inside the database");
        }

        @GetMapping("/no-resource")
        public void throwNoResource() throws Exception {
            throw new NoResourceFoundException(HttpMethod.GET, "static/main.css");
        }

        @PostMapping("/validation")
        public void throwValidation(@Valid @RequestBody DummyDto dto) {
        }
    }

    static class DummyDto {
        @NotBlank(message = "Field cannot be blank")
        private String name;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    // ═══════════════════════════════════════════════════════════
    // TEST CASES
    // ═══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Product Exceptions Handling")
    class ProductExceptions {

        @Test
        @DisplayName("✅ 404: ProductNotFoundException should map to NOT_FOUND")
        void handleNotFound() throws Exception {
            mockMvc.perform(get("/test-exceptions/not-found"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Product not found with id: 42"));
        }

        @Test
        @DisplayName("✅ 409: ProductDuplicateException should map to CONFLICT")
        void handleDuplicate() throws Exception {
            mockMvc.perform(get("/test-exceptions/duplicate"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("PRODUCT_ALREADY_EXISTS"))
                    .andExpect(jsonPath("$.message").value("Product already exists with name: Pixel 8"));
        }
    }

    @Nested
    @DisplayName("Validation Exceptions Handling")
    class ValidationExceptions {

        @Test
        @DisplayName("✅ 400: MethodArgumentNotValidException should return field validation details")
        void handleValidationFailure() throws Exception {
            DummyDto invalidDto = new DummyDto();
            invalidDto.setName(""); // triggers @NotBlank

            mockMvc.perform(post("/test-exceptions/validation")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[0].field").value("name"))
                    .andExpect(jsonPath("$.errors[0].message").value("Field cannot be blank"));
        }

        @Test
        @DisplayName("✅ 400: HttpMessageNotReadableException should map to BAD_REQUEST on malformed JSON")
        void handleMalformedJson() throws Exception {
            mockMvc.perform(post("/test-exceptions/validation")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid-json-body}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_JSON"))
                    .andExpect(jsonPath("$.message").value(containsString("Malformed JSON")));
        }
    }

    @Nested
    @DisplayName("HTTP & Standard Exceptions Handling")
    class HttpAndStandardExceptions {

        @Test
        @DisplayName("✅ 405: HttpRequestMethodNotSupportedException should map to METHOD_NOT_ALLOWED")
        void handleMethodNotSupported() throws Exception {
            // POST to a GET endpoint
            mockMvc.perform(post("/test-exceptions/not-found").with(csrf()))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"))
                    .andExpect(jsonPath("$.message").value(containsString("method is not supported")));
        }

        @Test
        @DisplayName("✅ 404: NoResourceFoundException should map to NOT_FOUND")
        void handleNoResourceFound() throws Exception {
            mockMvc.perform(get("/test-exceptions/no-resource"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("ENDPOINT_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Requested endpoint does not exist"));
        }

        @Test
        @DisplayName("✅ 400: IllegalArgumentException should map to BAD_REQUEST")
        void handleIllegalArgument() throws Exception {
            mockMvc.perform(get("/test-exceptions/illegal-argument"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"))
                    .andExpect(jsonPath("$.message").value("Invalid configuration parameter"));
        }
    }

    @Nested
    @DisplayName("Fallback Handler Exception")
    class FallbackExceptions {

        @Test
        @DisplayName("✅ 500: Generic unexpected Exception should hide details and return INTERNAL_SERVER_ERROR")
        void handleGeneric() throws Exception {
            mockMvc.perform(get("/test-exceptions/generic"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
                    .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."));
        }
    }
}
