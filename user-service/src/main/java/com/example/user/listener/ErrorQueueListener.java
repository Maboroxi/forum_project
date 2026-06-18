package com.example.user.listener;

import com.example.user.entity.dto.EmailRecord;
import com.example.user.mapper.EmailRecordMapper;
import com.example.user.utils.Const;
import com.example.observability.RabbitRequestContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RabbitListener(queues = Const.MQ_ERROR)
public class ErrorQueueListener {

    @Resource
    EmailRecordMapper recordMapper;

    @RabbitHandler
    public void saveErrorToDatabase(EmailRecord record, Message message) {
        try (var ignored = RabbitRequestContext.open(message)) {
            log.atError()
                    .addKeyValue("eventType", "message.consume")
                    .addKeyValue("messageId", message.getMessageProperties().getMessageId())
                    .addKeyValue("queue", Const.MQ_ERROR)
                    .addKeyValue("emailRecordId", record.getId())
                    .log("Mail message moved to dead letter queue");
            record.setStatus(2);
            recordMapper.updateById(record);
        }
    }
}
