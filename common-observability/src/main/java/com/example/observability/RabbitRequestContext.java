package com.example.observability;

import com.example.common.constants.GatewayHeaders;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;

import java.util.UUID;

public final class RabbitRequestContext {

    private RabbitRequestContext() {
    }

    public static MessagePostProcessor outbound() {
        return message -> {
            String requestId = RequestContext.requestId();
            if (requestId != null && !requestId.isBlank()) {
                message.getMessageProperties().setHeader(GatewayHeaders.REQUEST_ID, requestId);
            }
            if (message.getMessageProperties().getMessageId() == null) {
                message.getMessageProperties().setMessageId(UUID.randomUUID().toString());
            }
            if (message.getMessageProperties().getCorrelationId() == null) {
                message.getMessageProperties().setCorrelationId(
                        requestId == null ? message.getMessageProperties().getMessageId() : requestId);
            }
            return message;
        };
    }

    public static RequestContext.Scope open(Message message) {
        Object value = message.getMessageProperties().getHeaders().get(GatewayHeaders.REQUEST_ID);
        return RequestContext.open(value == null ? null : value.toString());
    }
}
