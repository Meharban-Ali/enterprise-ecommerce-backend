package com.redis.common;

import com.redis.infrastructure.security.CorrelationIdFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CorrelationIdTest {

    @Test
    void testCorrelationIdGeneratedAndPropagated() throws IOException, ServletException {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        String generatedId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertNotNull(generatedId);
        assertFalse(generatedId.trim().isEmpty());
        verify(chain, times(1)).doFilter(any(), any());
    }

    @Test
    void testExistingCorrelationIdIsPreserved() throws IOException, ServletException {
        CorrelationIdFilter filter = new CorrelationIdFilter();

        String existingId = "my-custom-correlation-id-123";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            assertEquals(existingId, MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
        };

        filter.doFilter(request, response, chain);

        assertEquals(existingId, response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER));
    }
}
