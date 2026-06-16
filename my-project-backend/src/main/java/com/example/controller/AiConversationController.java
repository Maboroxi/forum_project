package com.example.controller;

import com.alibaba.fastjson2.JSONObject;
import com.example.entity.RestBean;
import com.example.entity.dto.AiConversation;
import com.example.service.AiConversationService;
import com.example.utils.Const;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiConversationController {

    @Resource
    AiConversationService service;

    @GetMapping("/conversations")
    public RestBean<List<AiConversation>> listConversations(
            @RequestAttribute(Const.ATTR_USER_ID) int userId) {
        return RestBean.success(service.listConversations(userId));
    }

    @PostMapping("/conversations")
    public RestBean<AiConversation> createConversation(
            @RequestAttribute(Const.ATTR_USER_ID) int userId,
            @RequestBody(required = false) JSONObject body) {
        String title = body != null ? body.getString("title") : null;
        return RestBean.success(service.createConversation(userId, title));
    }

    @DeleteMapping("/conversations/{id}")
    public RestBean<Void> deleteConversation(
            @RequestAttribute(Const.ATTR_USER_ID) int userId,
            @PathVariable int id) {
        service.deleteConversation(userId, id);
        return RestBean.success();
    }

    @GetMapping("/conversations/{id}/messages")
    public RestBean<List<JSONObject>> getMessages(
            @RequestAttribute(Const.ATTR_USER_ID) int userId,
            @PathVariable int id) {
        try {
            return RestBean.success(service.loadMessages(userId, id));
        } catch (IllegalArgumentException e) {
            return RestBean.forbidden(e.getMessage());
        }
    }
}
