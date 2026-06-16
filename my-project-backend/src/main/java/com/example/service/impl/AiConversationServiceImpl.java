package com.example.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.AiConversation;
import com.example.entity.dto.AiConversationMessage;
import com.example.mapper.AiConversationMapper;
import com.example.mapper.AiConversationMessageMapper;
import com.example.service.AiConversationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class AiConversationServiceImpl
        extends ServiceImpl<AiConversationMapper, AiConversation>
        implements AiConversationService {

    @Resource
    AiConversationMessageMapper messageMapper;

    @Override
    public List<AiConversation> listConversations(int userId) {
        return this.list(new QueryWrapper<AiConversation>()
                .eq("user_id", userId)
                .orderByDesc("updated_time"));
    }

    @Override
    public AiConversation createConversation(int userId, String title) {
        AiConversation conv = new AiConversation();
        conv.setUserId(userId);
        conv.setTitle(title != null && !title.isEmpty() ? title : "新对话");
        conv.setCreatedTime(new Date());
        conv.setUpdatedTime(new Date());
        this.save(conv);
        return conv;
    }

    @Override
    @Transactional
    public void deleteConversation(int userId, int id) {
        AiConversation conv = this.getById(id);
        if (conv == null || !conv.getUserId().equals(userId)) return;
        messageMapper.delete(new QueryWrapper<AiConversationMessage>()
                .eq("conversation_id", id));
        this.removeById(id);
    }

    @Override
    @Transactional
    public void saveMessage(int userId, int conversationId, String role, String content, String messageType) {
        AiConversation conv = this.getById(conversationId);
        if (conv == null || !conv.getUserId().equals(userId))
            throw new IllegalArgumentException("无权访问此对话");

        AiConversationMessage msg = new AiConversationMessage();
        msg.setConversationId(conversationId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setMessageType(messageType != null ? messageType : "text");
        msg.setCreatedTime(new Date());
        messageMapper.insert(msg);

        conv.setUpdatedTime(new Date());
        this.updateById(conv);
    }

    @Override
    public List<JSONObject> loadMessages(int userId, int conversationId) {
        AiConversation conv = this.getById(conversationId);
        if (conv == null || !conv.getUserId().equals(userId))
            throw new IllegalArgumentException("无权访问此对话");

        List<AiConversationMessage> records = messageMapper.selectList(
                new QueryWrapper<AiConversationMessage>()
                        .eq("conversation_id", conversationId)
                        .orderByAsc("created_time"));

        List<JSONObject> result = new ArrayList<>();
        for (AiConversationMessage msg : records) {
            JSONObject obj = new JSONObject();
            obj.put("type", msg.getRole());
            obj.put("text", msg.getContent());
            obj.put("messageType", msg.getMessageType());
            result.add(obj);
        }
        return result;
    }

    @Override
    @Transactional
    public void updateTitle(int userId, int conversationId, String title) {
        AiConversation conv = this.getById(conversationId);
        if (conv == null || !conv.getUserId().equals(userId))
            throw new IllegalArgumentException("无权访问此对话");
        conv.setTitle(title);
        conv.setUpdatedTime(new Date());
        this.updateById(conv);
    }
}
