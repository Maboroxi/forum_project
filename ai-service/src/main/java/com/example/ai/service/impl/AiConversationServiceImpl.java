package com.example.ai.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.ai.entity.AiConversation;
import com.example.ai.entity.AiConversationMessage;
import com.example.ai.mapper.AiConversationMapper;
import com.example.ai.mapper.AiConversationMessageMapper;
import com.example.ai.service.AiConversationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class AiConversationServiceImpl
        extends ServiceImpl<AiConversationMapper, AiConversation>
        implements AiConversationService {

    @Resource
    private AiConversationMessageMapper messageMapper;

    @Override
    public List<AiConversation> listConversations(int userId) {
        return list(new LambdaQueryWrapper<AiConversation>()
                .eq(AiConversation::getUserId, userId)
                .orderByDesc(AiConversation::getUpdatedTime));
    }

    @Override
    public AiConversation createConversation(int userId, String title) {
        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setTitle(title == null || title.isBlank() ? "新对话" : title.trim());
        conversation.setCreatedTime(new Date());
        conversation.setUpdatedTime(new Date());
        save(conversation);
        return conversation;
    }

    @Override
    @Transactional
    public boolean deleteConversation(int userId, int id) {
        if (!ownsConversation(userId, id)) {
            return false;
        }
        messageMapper.delete(new LambdaQueryWrapper<AiConversationMessage>()
                .eq(AiConversationMessage::getConversationId, id));
        return removeById(id);
    }

    @Override
    @Transactional
    public void saveMessage(int userId, int conversationId, String role,
                            String content, String messageType) {
        AiConversation conversation = requireOwnedConversation(userId, conversationId);
        AiConversationMessage message = new AiConversationMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setMessageType(messageType == null ? "text" : messageType);
        message.setCreatedTime(new Date());
        messageMapper.insert(message);

        conversation.setUpdatedTime(new Date());
        updateById(conversation);
    }

    @Override
    public List<JSONObject> loadMessages(int userId, int conversationId) {
        requireOwnedConversation(userId, conversationId);
        return toResponse(messageMapper.selectList(new LambdaQueryWrapper<AiConversationMessage>()
                .eq(AiConversationMessage::getConversationId, conversationId)
                .orderByAsc(AiConversationMessage::getCreatedTime)
                .orderByAsc(AiConversationMessage::getId)));
    }

    @Override
    public List<JSONObject> loadRecentMessages(int userId, int conversationId, int limit) {
        requireOwnedConversation(userId, conversationId);
        List<AiConversationMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<AiConversationMessage>()
                        .eq(AiConversationMessage::getConversationId, conversationId)
                        .orderByDesc(AiConversationMessage::getCreatedTime)
                        .orderByDesc(AiConversationMessage::getId)
                        .last("LIMIT " + Math.max(1, Math.min(limit, 50))));
        Collections.reverse(messages);
        return toResponse(messages);
    }

    @Override
    public void updateTitle(int userId, int conversationId, String title) {
        AiConversation conversation = requireOwnedConversation(userId, conversationId);
        conversation.setTitle(title);
        conversation.setUpdatedTime(new Date());
        updateById(conversation);
    }

    @Override
    public boolean ownsConversation(int userId, int conversationId) {
        AiConversation conversation = getById(conversationId);
        return conversation != null && conversation.getUserId().equals(userId);
    }

    private AiConversation requireOwnedConversation(int userId, int conversationId) {
        AiConversation conversation = getById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权访问此对话");
        }
        return conversation;
    }

    private List<JSONObject> toResponse(List<AiConversationMessage> messages) {
        List<JSONObject> result = new ArrayList<>(messages.size());
        for (AiConversationMessage message : messages) {
            JSONObject item = new JSONObject();
            item.put("type", message.getRole());
            item.put("text", message.getContent());
            item.put("messageType", message.getMessageType());
            result.add(item);
        }
        return result;
    }
}
