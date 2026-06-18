package com.example.observability;

import com.example.common.constants.GatewayHeaders;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.function.Supplier;

@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ServletAccessLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ServletAccessLogFilter.class);
    private static final Set<String> QUIET_PATHS = Set.of("/actuator/health", "/actuator/prometheus");

    private final Supplier<String> traceIdSupplier;

    public ServletAccessLogFilter(Tracer tracer) {
        this(() -> {
            Span span = tracer.currentSpan();
            return span == null ? null : span.context().traceId();
        });
    }

    ServletAccessLogFilter(Supplier<String> traceIdSupplier) {
        this.traceIdSupplier = traceIdSupplier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = RequestIds.normalize(request.getHeader(GatewayHeaders.REQUEST_ID));
        String previousRequestId = MDC.get(RequestContext.REQUEST_ID);
        String previousUserId = MDC.get(RequestContext.USER_ID);
        MDC.put(RequestContext.REQUEST_ID, requestId);
        putIfPresent(RequestContext.USER_ID, request.getHeader(GatewayHeaders.USER_ID));
        response.setHeader(GatewayHeaders.REQUEST_ID, requestId);
        putTraceHeader(response);

        long started = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - started) / 1_000_000;
            if (!QUIET_PATHS.contains(request.getRequestURI())) {
                log.atInfo()
                        .addKeyValue("eventType", "http.access")
                        .addKeyValue("method", request.getMethod())
                        .addKeyValue("path", request.getRequestURI())
                        .addKeyValue("status", response.getStatus())
                        .addKeyValue("durationMs", durationMs)
                        .addKeyValue("remoteAddress", request.getRemoteAddr())
                        .log("HTTP request completed");
            }
            restore(RequestContext.REQUEST_ID, previousRequestId);
            restore(RequestContext.USER_ID, previousUserId);
        }
    }

    private void putTraceHeader(HttpServletResponse response) {
        String traceId = traceIdSupplier.get();
        if (traceId != null && !traceId.isBlank()) {
            response.setHeader(GatewayHeaders.TRACE_ID, traceId);
        }
    }

    private void putIfPresent(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }

    private void restore(String key, String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }
}
