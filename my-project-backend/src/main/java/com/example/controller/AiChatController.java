package com.example.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.service.AiService;
import com.example.utils.Const;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    @Resource
    AiService service;

    // 旧接口 - 保留给旧版浮动 AiChatWindow 使用
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatWithAI(@RequestBody JSONArray content) {
        return service.chatWithAi(content);
    }

    // 新接口 - 按会话ID发送消息
    @PostMapping(value = "/chat/{conversationId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatWithConversation(
            @PathVariable int conversationId,
            @RequestAttribute(Const.ATTR_USER_ID) int userId,
            @RequestBody JSONObject body) {
        String text = body.getString("text");
        List<String> imageUrls = body.getJSONArray("imageUrls") != null
                ? body.getJSONArray("imageUrls").toList(String.class)
                : List.of();
        boolean enableWebSearch = body.getBooleanValue("enableWebSearch");
        String fileContent = body.getString("fileContent");
        String fileName = body.getString("fileName");
        return service.chatWithAi(conversationId, userId, text, imageUrls, enableWebSearch, fileContent, fileName);
    }
}
