package com.example.observability;

import org.slf4j.MDC;

import java.util.Map;

public final class RequestContext {

    public static final String REQUEST_ID = "requestId";
    public static final String USER_ID = "userId";

    private RequestContext() {
    }

    public static String requestId() {
        return MDC.get(REQUEST_ID);
    }

    public static Scope open(String requestId) {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        MDC.put(REQUEST_ID, RequestIds.normalize(requestId));
        return () -> {
            MDC.clear();
            if (previous != null) {
                MDC.setContextMap(previous);
            }
        };
    }

    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
