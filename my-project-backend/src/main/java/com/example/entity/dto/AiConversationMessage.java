package com.example.entity.dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("ai_conversation_message")
public class AiConversationMessage {
    @TableId
    Integer id;
    Integer conversationId;
    String role;
    String content;
    String messageType;
    Date createdTime;
}
