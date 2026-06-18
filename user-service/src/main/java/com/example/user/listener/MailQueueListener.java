package com.example.user.listener;

import com.example.user.entity.dto.EmailRecord;
import com.example.user.mapper.EmailRecordMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
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
    public void sendMailMessage(EmailRecord email) {
        try {
            sender.send(createMessage(email));
            email.setStatus(1);
            recordMapper.updateById(email);
            log.info("邮件发送成功，邮件记录ID：{}，邮件接收人: {}", email.getId(), email.getEmail());
        } catch (Exception e) {
            log.error("邮件发送失败，邮件记录ID：{}，邮件接收人: {}, 错误信息: {}", email.getId(), email.getEmail(), e.getMessage());
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
