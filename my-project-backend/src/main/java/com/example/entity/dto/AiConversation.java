package com.example.entity.dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("ai_conversation")
public class AiConversation {
    @TableId
    Integer id;
    Integer userId;
    String title;
    Date createdTime;
    Date updatedTime;
}
