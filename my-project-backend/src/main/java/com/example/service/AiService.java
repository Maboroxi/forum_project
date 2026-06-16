package com.example.service;

import com.alibaba.fastjson2.JSONArray;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AiService {
    SseEmitter chatWithAi(JSONArray content);

    SseEmitter chatWithAi(int conversationId, int userId, String text, List<String> imageUrls,
                          boolean enableWebSearch, String fileContent, String fileName);
}
