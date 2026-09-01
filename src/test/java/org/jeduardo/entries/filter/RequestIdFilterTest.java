package org.jeduardo.entries.filter;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestIdFilterTest {

    private static final String X_REQUEST_ID = "X-Request-Id";
    private static final String REQUEST_ID = "requestId";
    private final RequestIdFilter filter = new RequestIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void preservesNonblankIncomingRequestIdInMdcAndResponse() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(X_REQUEST_ID, "incoming-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertThat(MDC.get(REQUEST_ID)).isEqualTo("incoming-id"));

        assertThat(response.getHeader(X_REQUEST_ID)).isEqualTo("incoming-id");
        assertThat(MDC.get(REQUEST_ID)).isNull();
    }

    @Test
    void generatesRequestIdForBlankIncomingHeaderAndClearsMdc() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(X_REQUEST_ID, "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdDuringChain = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                requestIdDuringChain.set(MDC.get(REQUEST_ID)));

        assertThat(response.getHeader(X_REQUEST_ID)).isEqualTo(requestIdDuringChain.get());
        assertThat(UUID.fromString(requestIdDuringChain.get())).isNotNull();
        assertThat(MDC.get(REQUEST_ID)).isNull();
    }

    @Test
    void generatesRequestIdForAbsentIncomingHeaderAndClearsMdc() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdDuringChain = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                requestIdDuringChain.set(MDC.get(REQUEST_ID)));

        assertThat(response.getHeader(X_REQUEST_ID)).isEqualTo(requestIdDuringChain.get());
        assertThat(UUID.fromString(requestIdDuringChain.get())).isNotNull();
        assertThat(MDC.get(REQUEST_ID)).isNull();
    }

    @Test
    void clearsMdcWhenChainThrowsServletException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(X_REQUEST_ID, "exception-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ServletException exception = new ServletException("chain failed");

        assertThatThrownBy(() -> filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertThat(MDC.get(REQUEST_ID)).isEqualTo("exception-id");
            throw exception;
        })).isSameAs(exception);

        assertThat(response.getHeader(X_REQUEST_ID)).isEqualTo("exception-id");
        assertThat(MDC.get(REQUEST_ID)).isNull();
    }
}
