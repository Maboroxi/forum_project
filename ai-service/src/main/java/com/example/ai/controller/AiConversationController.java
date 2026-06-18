package com.example.ai.controller;

import com.alibaba.fastjson2.JSONObject;
import com.example.ai.entity.AiConversation;
import com.example.ai.service.AiConversationService;
import com.example.common.constants.RequestAttributes;
import com.example.common.entity.RestBean;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiConversationController {

    @Resource
    private AiConversationService service;

    @GetMapping("/conversations")
    public RestBean<List<AiConversation>> list(
            @RequestAttribute(RequestAttributes.USER_ID) int userId) {
        return RestBean.success(service.listConversations(userId));
    }

    @PostMapping("/conversations")
    public RestBean<AiConversation> create(
            @RequestAttribute(RequestAttributes.USER_ID) int userId,
            @RequestBody(required = false) JSONObject body) {
        return RestBean.success(service.createConversation(
                userId, body == null ? null : body.getString("title")));
    }

    @DeleteMapping("/conversations/{id}")
    public RestBean<Void> delete(
            @RequestAttribute(RequestAttributes.USER_ID) int userId,
            @PathVariable int id) {
        return service.deleteConversation(userId, id)
                ? RestBean.success()
                : RestBean.failure(404, "对话不存在");
    }

    @GetMapping("/conversations/{id}/messages")
    public RestBean<List<JSONObject>> messages(
            @RequestAttribute(RequestAttributes.USER_ID) int userId,
            @PathVariable int id) {
        try {
            return RestBean.success(service.loadMessages(userId, id));
        } catch (IllegalArgumentException e) {
            return RestBean.failure(404, "对话不存在");
        }
    }
}
