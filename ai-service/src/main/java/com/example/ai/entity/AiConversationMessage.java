package com.example.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("ai_conversation_message")
public class AiConversationMessage {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer conversationId;
    private String role;
    private String content;
    private String messageType;
    private Date createdTime;
}
