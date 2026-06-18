package com.example.gateway.filter;

import com.example.common.constants.GatewayHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ObservabilityGlobalFilterTest {

    @Test
    void replacesClientRequestIdAndReturnsTraceHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/forum/types")
                        .header(GatewayHeaders.REQUEST_ID, "999")
                        .build());
        ObservabilityGlobalFilter filter = new ObservabilityGlobalFilter(() -> "abcdef0123456789");

        filter.filter(exchange, current -> {
            String requestId = current.getRequest().getHeaders().getFirst(GatewayHeaders.REQUEST_ID);
            assertNotNull(requestId);
            assertNotEquals("999", requestId);
            return Mono.empty();
        }).block();

        HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
        assertNotNull(responseHeaders.getFirst(GatewayHeaders.REQUEST_ID));
        assertEquals("abcdef0123456789", responseHeaders.getFirst(GatewayHeaders.TRACE_ID));
    }
}
