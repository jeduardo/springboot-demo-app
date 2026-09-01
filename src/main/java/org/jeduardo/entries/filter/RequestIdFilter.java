package org.jeduardo.entries.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Retrieve a request ID from the incoming request and make it available
 * for both the logging system and the request response.
 */
@Component
public class RequestIdFilter implements Filter {
    private static final String X_REQUEST_ID = "X-Request-Id";
    private static final String REQUEST_ID = "requestId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var http = (HttpServletRequest) request;
        String id = Optional.ofNullable(http.getHeader(X_REQUEST_ID))
                .filter(s -> !s.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setHeader(X_REQUEST_ID, id);
        try (MDC.MDCCloseable ignored = MDC.putCloseable(REQUEST_ID, id)) {
            chain.doFilter(request, response);
        }
    }
}
