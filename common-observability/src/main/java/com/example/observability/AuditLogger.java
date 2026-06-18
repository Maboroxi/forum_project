package com.example.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger("audit.admin");

    private AuditLogger() {
    }

    public static void success(String action, String targetType, Object targetId) {
        log.atInfo()
                .addKeyValue("eventType", "audit.admin")
                .addKeyValue("action", action)
                .addKeyValue("targetType", targetType)
                .addKeyValue("targetId", targetId)
                .addKeyValue("result", "success")
                .log("Administrator operation completed");
    }
}
