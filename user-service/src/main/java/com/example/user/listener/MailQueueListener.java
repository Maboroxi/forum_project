package com.example.user.listener;

import com.example.user.entity.dto.EmailRecord;
import com.example.user.mapper.EmailRecordMapper;
import com.example.observability.RabbitRequestContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RabbitListener(queues = "mail")
public class MailQueueListener {

    @Resource
    JavaMailSender sender;

    @Resource
    EmailRecordMapper recordMapper;

    @Value("${spring.mail.username}")
    String username;

    @RabbitHandler
    public void sendMailMessage(EmailRecord email, Message message) {
        try (var ignored = RabbitRequestContext.open(message)) {
            sender.send(createMessage(email));
            email.setStatus(1);
            recordMapper.updateById(email);
            log.atInfo()
                    .addKeyValue("eventType", "message.consume")
                    .addKeyValue("messageId", message.getMessageProperties().getMessageId())
                    .addKeyValue("queue", "mail")
                    .addKeyValue("emailRecordId", email.getId())
                    .log("Mail message sent");
        } catch (Exception e) {
            log.atError()
                    .addKeyValue("eventType", "application.error")
                    .addKeyValue("messageId", message.getMessageProperties().getMessageId())
                    .addKeyValue("emailRecordId", email.getId())
                    .setCause(e)
                    .log("Failed to send mail message");
            throw e;
        }
    }

    private SimpleMailMessage createMessage(EmailRecord email) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject(email.getTitle());
        message.setText(email.getContent());
        message.setTo(email.getEmail());
        message.setFrom(username);
        return message;
    }
}
