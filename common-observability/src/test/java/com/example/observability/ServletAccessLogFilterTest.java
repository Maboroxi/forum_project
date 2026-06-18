package com.example.observability;

import com.example.common.constants.GatewayHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ServletAccessLogFilterTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void propagatesRequestAndTraceHeadersAndCleansMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/forum/types");
        request.addHeader(GatewayHeaders.REQUEST_ID, "123");
        request.addHeader(GatewayHeaders.USER_ID, "7");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new ServletAccessLogFilter(() -> "0123456789abcdef").doFilter(request, response, (req, res) -> {
            assertEquals("123", MDC.get(RequestContext.REQUEST_ID));
            assertEquals("7", MDC.get(RequestContext.USER_ID));
        });

        assertEquals("123", response.getHeader(GatewayHeaders.REQUEST_ID));
        assertEquals("0123456789abcdef", response.getHeader(GatewayHeaders.TRACE_ID));
        assertNull(MDC.get(RequestContext.REQUEST_ID));
        assertNull(MDC.get(RequestContext.USER_ID));
    }
}
