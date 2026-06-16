package com.example.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.dto.AiConversation;

import java.util.List;

public interface AiConversationService extends IService<AiConversation> {
    List<AiConversation> listConversations(int userId);
    AiConversation createConversation(int userId, String title);
    void deleteConversation(int userId, int id);
    void saveMessage(int userId, int conversationId, String role, String content, String messageType);
    List<JSONObject> loadMessages(int userId, int conversationId);
    void updateTitle(int userId, int conversationId, String title);
}
