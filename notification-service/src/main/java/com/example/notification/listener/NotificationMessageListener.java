package com.example.notification.listener;

import com.example.common.entity.RestBean;
import com.example.notification.service.NotificationService;
import com.example.observability.RabbitRequestContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RabbitListener(queues = "notification")
public class NotificationMessageListener {

    @Resource
    NotificationService notificationService;

    @RabbitHandler
    public void handleNotification(Map<String, Object> payload, Message message) {
        try (var ignored = RabbitRequestContext.open(message)) {
            int uid = (int) payload.get("recipientUid");
            String title = (String) payload.get("title");
            String content = (String) payload.get("content");
            String type = payload.getOrDefault("type", "success").toString();
            String url = payload.getOrDefault("url", "").toString();
            notificationService.addNotification(uid, title, content, type, url);
            log.atInfo()
                    .addKeyValue("eventType", "message.consume")
                    .addKeyValue("messageId", message.getMessageProperties().getMessageId())
                    .addKeyValue("queue", "notification")
                    .addKeyValue("recipientUid", uid)
                    .log("Notification created");
        } catch (Exception e) {
            log.atError()
                    .addKeyValue("eventType", "application.error")
                    .addKeyValue("messageId", message.getMessageProperties().getMessageId())
                    .setCause(e)
                    .log("Failed to consume notification message");
            throw e; // trigger RabbitMQ retry
        }
    }
}
