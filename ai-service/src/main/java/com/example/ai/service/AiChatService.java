package com.example.ai.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiChatService {
    SseEmitter chat(JSONArray context);
    SseEmitter chat(int conversationId, int userId, JSONObject body);
}
