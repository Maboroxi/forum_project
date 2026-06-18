package com.example.notification.listener;

import com.example.common.entity.RestBean;
import com.example.notification.service.NotificationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RabbitListener(queues = "notification")
public class NotificationMessageListener {

    @Resource
    NotificationService notificationService;

    @RabbitHandler
    public void handleNotification(Map<String, Object> message) {
        try {
            int uid = (int) message.get("recipientUid");
            String title = (String) message.get("title");
            String content = (String) message.get("content");
            String type = (String) message.getOrDefault("type", "success").toString();
            String url = (String) message.getOrDefault("url", "").toString();
            notificationService.addNotification(uid, title, content, type, url);
            log.info("通知已创建: uid={}, title={}", uid, title);
        } catch (Exception e) {
            log.error("处理通知消息失败: {}", e.getMessage(), e);
            throw e; // trigger RabbitMQ retry
        }
    }
}
