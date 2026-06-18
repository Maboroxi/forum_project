package com.example.observability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestIdsTest {

    @Test
    void acceptsPositiveNumericRequestId() {
        assertEquals("123", RequestIds.normalize("123"));
    }

    @Test
    void replacesInvalidRequestId() {
        String first = RequestIds.normalize("invalid");
        String second = RequestIds.normalize(null);
        assertTrue(Long.parseLong(first) > 0);
        assertTrue(Long.parseLong(second) > 0);
        assertNotEquals(first, second);
    }
}
