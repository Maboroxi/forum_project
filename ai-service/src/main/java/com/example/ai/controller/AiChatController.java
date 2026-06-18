package com.example.ai.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.ai.service.AiChatService;
import com.example.common.constants.RequestAttributes;
import com.example.common.entity.RestBean;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    @Resource
    private AiChatService service;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter legacyChat(@RequestBody JSONArray content) {
        return service.chat(content);
    }

    @PostMapping(value = "/chat/{conversationId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(
            @PathVariable int conversationId,
            @RequestAttribute(RequestAttributes.USER_ID) int userId,
            @RequestBody JSONObject body) {
        return service.chat(conversationId, userId, body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public RestBean<Void> notFound(IllegalArgumentException exception) {
        return RestBean.failure(404, exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public RestBean<Void> conflict(IllegalStateException exception) {
        return RestBean.failure(409, exception.getMessage());
    }
}
