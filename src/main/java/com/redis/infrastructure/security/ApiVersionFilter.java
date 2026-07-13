package com.redis.infrastructure.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiVersionFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String path = httpRequest.getRequestURI();
            
            if (path.startsWith("/api/v")) {
                int nextSlash = path.indexOf("/", 6);
                if (nextSlash != -1) {
                    String version = path.substring(5, nextSlash); // "v1", "v2", etc.
                    String newPath = "/api" + path.substring(nextSlash);
                    httpRequest.setAttribute("resolvedApiVersion", version);

                    HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(httpRequest) {
                        @Override
                        public String getRequestURI() {
                            return newPath;
                        }
                        @Override
                        public String getServletPath() {
                            return newPath;
                        }
                        @Override
                        public StringBuffer getRequestURL() {
                            StringBuffer url = new StringBuffer();
                            url.append(httpRequest.getScheme())
                               .append("://")
                               .append(httpRequest.getServerName())
                               .append(":")
                               .append(httpRequest.getServerPort())
                               .append(newPath);
                            return url;
                        }
                    };
                    chain.doFilter(wrapper, response);
                    return;
                }
            }
        }
        
        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}
}
