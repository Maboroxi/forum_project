package com.example.ai.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.ai.entity.AiConversation;

import java.util.List;

public interface AiConversationService extends IService<AiConversation> {
    List<AiConversation> listConversations(int userId);
    AiConversation createConversation(int userId, String title);
    boolean deleteConversation(int userId, int id);
    void saveMessage(int userId, int conversationId, String role, String content, String messageType);
    List<JSONObject> loadMessages(int userId, int conversationId);
    List<JSONObject> loadRecentMessages(int userId, int conversationId, int limit);
    void updateTitle(int userId, int conversationId, String title);
    boolean ownsConversation(int userId, int conversationId);
}
