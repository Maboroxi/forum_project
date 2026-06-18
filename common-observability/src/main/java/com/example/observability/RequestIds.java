package com.example.observability;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public final class RequestIds {

    private static final AtomicLong SEQUENCE = new AtomicLong(
            (System.currentTimeMillis() << 20) | ThreadLocalRandom.current().nextInt(1 << 20));

    private RequestIds() {
    }

    public static String next() {
        return Long.toString(SEQUENCE.incrementAndGet() & Long.MAX_VALUE);
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return next();
        }
        try {
            long parsed = Long.parseLong(value);
            return parsed > 0 ? Long.toString(parsed) : next();
        } catch (NumberFormatException ignored) {
            return next();
        }
    }
}
