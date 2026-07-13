package com.redis.infrastructure.governance;

import com.redis.infrastructure.config.ApiSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

public class ApiDeprecationTest {

    @Test
    void testDeprecationHeadersInjected() throws Exception {
        ApiSecurityProperties props = new ApiSecurityProperties();
        props.setApiVersioningEnabled(true);

        ApiVersionResolver resolver = new ApiVersionResolver(props);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("resolvedApiVersion", "v1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = resolver.preHandle(request, response, new Object());

        assertTrue(result);
        assertNotNull(response.getHeader("Deprecation"));
        assertNotNull(response.getHeader("Sunset"));
        assertNotNull(response.getHeader("Link"));
        assertEquals("v1", response.getHeader("X-API-Version"));
    }
}
