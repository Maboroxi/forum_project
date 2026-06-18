package com.example.common.entity;

import org.slf4j.MDC;

import java.util.List;
import java.util.Optional;

public record PageRestBean<T>(long id, int code, PageList<T> data, String message) {

    public record PageList<T>(List<T> list, long total, long page) {
    }

    public static <T> PageRestBean<T> success(List<T> list, long total, long page) {
        return new PageRestBean<>(requestId(), 200, new PageList<>(list, total, page), "请求成功");
    }

    private static long requestId() {
        String requestId = Optional.ofNullable(MDC.get("requestId"))
                .orElseGet(() -> Optional.ofNullable(MDC.get("reqId")).orElse("0"));
        try {
            return Long.parseLong(requestId);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
