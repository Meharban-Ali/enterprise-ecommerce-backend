package com.redis.infrastructure.governance;

import com.redis.infrastructure.security.ApiVersionFilter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

public class ApiVersioningTest {

    @Test
    void testVersionResolverRewriting() throws Exception {
        ApiVersionFilter filter = new ApiVersionFilter();
        
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals("v1", request.getAttribute("resolvedApiVersion"));
        assertEquals("/api/products", ((jakarta.servlet.http.HttpServletRequest) chain.getRequest()).getRequestURI());
    }
}
