package org.jeduardo.entries.filter;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Retrieve a request ID from the incoming request and make it available
 * for both the logging system and the request response.
 */
@Component
public class RequestIdFilter implements Filter {
    private static final String X_REQUEST_ID = "X-Request-Id";
    private static final Logger LOGGER = LogManager.getLogger(RequestIdFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            String requestId = httpRequest.getHeader(X_REQUEST_ID);
            if (StringUtils.isEmpty(requestId)) {
                LOGGER.debug("Request ID not presented, creating it on the fly");
                requestId = UUID.randomUUID().toString();
            }
            MDC.put("requestId", requestId);
            httpResponse.addHeader(X_REQUEST_ID, requestId);
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    @Override
    public void destroy() {

    }
}