package com.example.gateway.filter;

import com.example.common.constants.GatewayHeaders;
import com.example.observability.RequestIds;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

@Component
public class ObservabilityGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityGlobalFilter.class);

    private final Supplier<String> traceIdSupplier;

    @Autowired
    public ObservabilityGlobalFilter(Tracer tracer) {
        this(() -> {
            Span span = tracer.currentSpan();
            return span == null ? null : span.context().traceId();
        });
    }

    ObservabilityGlobalFilter(Supplier<String> traceIdSupplier) {
        this.traceIdSupplier = traceIdSupplier;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = RequestIds.next();
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(GatewayHeaders.REQUEST_ID);
                    headers.remove(GatewayHeaders.TRACE_ID);
                    headers.set(GatewayHeaders.REQUEST_ID, requestId);
                })
                .build();
        exchange.getResponse().getHeaders().set(GatewayHeaders.REQUEST_ID, requestId);
        ServerWebExchange mutated = exchange.mutate().request(request).build();

        return Mono.defer(() -> {
            String traceId = traceIdSupplier.get();
            long started = System.nanoTime();
            if (traceId != null) {
                exchange.getResponse().getHeaders().set(GatewayHeaders.TRACE_ID, traceId);
            }

            return chain.filter(mutated).doFinally(signal -> {
                long durationMs = (System.nanoTime() - started) / 1_000_000;
                int status = exchange.getResponse().getStatusCode() == null
                        ? 0 : exchange.getResponse().getStatusCode().value();
                log.atInfo()
                        .addKeyValue("eventType", "http.access")
                        .addKeyValue("requestId", requestId)
                        .addKeyValue("method", request.getMethod().name())
                        .addKeyValue("path", request.getURI().getPath())
                        .addKeyValue("status", status)
                        .addKeyValue("durationMs", durationMs)
                        .log("Gateway request completed");
            });
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
