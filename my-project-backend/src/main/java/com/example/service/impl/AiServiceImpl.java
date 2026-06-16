package com.example.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.entity.dto.AiConversation;
import com.example.service.AiConversationService;
import com.example.service.AiService;
import com.example.service.ForumTools;
import com.example.service.ImageTools;
import com.example.service.WebSearchTools;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AiServiceImpl implements AiService {

    @Resource
    ChatModel chatModel;

    @Resource
    ForumTools forumTools;

    @Resource
    WebSearchTools webSearchTools;

    @Resource
    ImageTools imageTools;

    @Resource
    AiConversationService conversationService;

    ChatClient chatClient;

    @PostConstruct
    public void init() {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你是校园论坛的AI助手，名字叫「校园AI助手」。你友善、专业，能够帮助学生解答问题。" +
                        "你可以在论坛中搜索帖子、获取最新的帖子信息。" +
                        "当需要互联网上的最新信息时，你可以进行网络搜索。" +
                        "你还具备图片识别和图片生成能力：当用户上传图片并询问图片内容时，调用 recognize_image 工具来识别；" +
                        "当用户要求画图、生成图片、创作图像时，调用 generate_image 工具来生成。" +
                        "请用中文回复。")
                .build();
    }

    @Override
    public SseEmitter chatWithAi(JSONArray context) {
        SseEmitter emitter = new SseEmitter(30000L);
        List<Message> messages = new ArrayList<>();
        for (Object item : context) {
            JSONObject obj = JSONObject.from(item);
            Message msg = switch (obj.getString("type")) {
                case "user" -> new UserMessage(obj.getString("text"));
                case "assistant" -> new AssistantMessage(obj.getString("text"));
                default -> throw new RuntimeException("Unknown message type: " + obj.getString("type"));
            };
            messages.add(msg);
        }

        Flux<String> flux = this.chatClient.prompt()
                .messages(messages)
                .stream()
                .content();

        flux.subscribe(text -> {
            try {
                emitter.send(text);
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }, emitter::completeWithError, emitter::complete);

        return emitter;
    }

    @Override
    public SseEmitter chatWithAi(int conversationId, int userId, String text,
                                 List<String> imageUrls, boolean enableWebSearch,
                                 String fileContent, String fileName) {
        SseEmitter emitter = new SseEmitter(300000L);

        try {
            // 1. 从数据库加载历史消息
            List<JSONObject> history = conversationService.loadMessages(userId, conversationId);
            List<Message> messages = new ArrayList<>();

            // 2. 将历史消息转为 Spring AI Message（含文件内容重建）
            for (JSONObject msg : history) {
                String role = msg.getString("type");
                String rawContent = msg.getString("text");
                if ("user".equals(role)) {
                    // 解析存储的 JSON，若有 fileContent 则重建完整上下文
                    try {
                        JSONObject contentObj = JSONObject.parseObject(rawContent);
                        String userText = contentObj.getString("text");
                        String fn = contentObj.getString("fileName");
                        String fc = contentObj.getString("fileContent");
                        if (fc != null && !fc.isEmpty()) {
                            String restored = "用户上传了文件「" + fn + "」，以下是文件内容：\n\n"
                                    + fc + "\n\n"
                                    + "用户的问题：" + (userText != null ? userText : "");
                            messages.add(new UserMessage(restored));
                        } else {
                            messages.add(new UserMessage(userText != null ? userText : ""));
                        }
                    } catch (Exception e) {
                        // 兼容旧格式：直接作为纯文本
                        messages.add(new UserMessage(rawContent));
                    }
                } else if ("assistant".equals(role)) {
                    messages.add(new AssistantMessage(rawContent));
                } else if ("system".equals(role)) {
                    // 工具调用结果等系统上下文，供 AI 参考
                    messages.add(new SystemMessage(rawContent));
                }
            }

            // 3. 将文件内容拼入用户消息文本
            String finalText = text;
            if (fileContent != null && !fileContent.isEmpty()) {
                String fileLabel = (fileName != null && !fileName.isEmpty()) ? fileName : "上传的文件";
                finalText = "用户上传了文件「" + fileLabel + "」，以下是文件内容：\n\n"
                        + fileContent + "\n\n"
                        + "用户的问题：" + text;
            }

            // 4. 构建当前用户消息（DeepSeek 不支持多模态，改为文本提示图片 URL 让 AI 调用工具识别）
            if (imageUrls != null && !imageUrls.isEmpty()) {
                StringBuilder extendedText = new StringBuilder(finalText);
                extendedText.append("\n\n[用户上传了以下图片，可调用图片识别工具 recognize_image 来分析图片内容：\n");
                for (int i = 0; i < imageUrls.size(); i++) {
                    extendedText.append(i + 1).append(". ").append(imageUrls.get(i)).append("\n");
                }
                extendedText.append("]");
                messages.add(new UserMessage(extendedText.toString()));
            } else {
                messages.add(new UserMessage(finalText));
            }

            // 5. 保存用户消息到数据库（原文与文件内容分离存储）
            JSONObject userContent = new JSONObject();
            userContent.put("text", text != null ? text : "");
            if (imageUrls != null && !imageUrls.isEmpty()) {
                userContent.put("imageUrls", imageUrls);
            }
            if (fileContent != null && !fileContent.isEmpty()) {
                userContent.put("fileName", fileName);
                userContent.put("fileContent", fileContent);
            }
            conversationService.saveMessage(userId, conversationId, "user",
                    userContent.toJSONString(),
                    (imageUrls != null && !imageUrls.isEmpty()) ? "image" : "text");

            // 5. 注册工具
            List<ToolCallback> toolCallbacks = new ArrayList<>();
            Collections.addAll(toolCallbacks, ToolCallbacks.from(forumTools));
            Collections.addAll(toolCallbacks, ToolCallbacks.from(imageTools));
            if (enableWebSearch) {
                Collections.addAll(toolCallbacks, ToolCallbacks.from(webSearchTools));
            }

            // 6. 第一轮：非流式调用，检测工具调用
            ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                    .internalToolExecutionEnabled(false)
                    .build();
            Prompt prompt = new Prompt(messages, options);
            ChatResponse firstResponse = chatClient.prompt(prompt)
                    .toolCallbacks(toolCallbacks.toArray(new ToolCallback[0]))
                    .call()
                    .chatResponse();

            AssistantMessage assistantMessage = firstResponse.getResult().getOutput();
            StringBuilder fullReply = new StringBuilder();

            if (!CollectionUtils.isEmpty(assistantMessage.getToolCalls())) {
                // 7. 手动执行工具，并推送 tool_call SSE 事件
                List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();

                for (var toolCall : assistantMessage.getToolCalls()) {
                    // 推工具调用事件到前端
                    JSONObject toolEvent = new JSONObject();
                    toolEvent.put("type", "tool_call");
                    toolEvent.put("tool", toolCall.name());
                    try {
                        JSONObject argObj = JSONObject.parseObject(toolCall.arguments());
                        toolEvent.put("input", argObj);
                    } catch (Exception e) {
                        toolEvent.put("input", toolCall.arguments());
                    }
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(toolEvent.toJSONString()));

                    // 执行工具并保存结果到对话历史
                    for (ToolCallback callback : toolCallbacks) {
                        if (callback.getToolDefinition().name().equals(toolCall.name())) {
                            String result = callback.call(toolCall.arguments(), new ToolContext(java.util.Map.of()));
                            toolResponses.add(new ToolResponseMessage.ToolResponse(
                                    toolCall.id(), toolCall.name(), result));
                            // 保存工具调用结果到历史，供后续对话恢复上下文
                            conversationService.saveMessage(userId, conversationId, "system",
                                    "调用工具「" + toolCall.name() + "」的结果：\n" + result,
                                    "tool_result");
                            break;
                        }
                    }
                }

                // 8. 第二轮：流式调用，带工具结果
                ToolResponseMessage toolMsg = ToolResponseMessage.builder()
                        .responses(toolResponses).build();
                List<Message> round2Messages = new ArrayList<>(messages);
                round2Messages.add(assistantMessage);
                round2Messages.add(toolMsg);

                Flux<String> flux = chatClient.prompt(new Prompt(round2Messages))
                        .stream().content();

                flux.subscribe(
                        chunk -> {
                            fullReply.append(chunk);
                            try {
                                JSONObject json = new JSONObject();
                                json.put("type", "text");
                                json.put("content", chunk);
                                emitter.send(SseEmitter.event()
                                        .name("message")
                                        .data(json.toJSONString()));
                            } catch (IOException e) {
                                // ignore
                            }
                        },
                        error -> {
                            try {
                                JSONObject errJson = new JSONObject();
                                errJson.put("type", "error");
                                errJson.put("content", error.getMessage() != null ? error.getMessage() : "unknown");
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data(errJson.toJSONString()));
                            } catch (IOException e) {
                                // ignore
                            }
                            emitter.completeWithError(error);
                        },
                        () -> {
                            // 保存完整 AI 回复
                            if (fullReply.length() > 0) {
                                conversationService.saveMessage(userId, conversationId, "assistant",
                                        fullReply.toString(), "text");
                            }
                            // 自动为首次对话生成标题
                            autoGenerateTitleIfNeeded(conversationId, userId, messages, emitter);
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("done")
                                        .data(""));
                            } catch (IOException e) {
                                // ignore
                            }
                            emitter.complete();
                        }
                );
            } else {
                // 9. 没有工具调用，直接返回文本
                String replyText = assistantMessage.getText();
                if (replyText != null) {
                    fullReply.append(replyText);
                    conversationService.saveMessage(userId, conversationId, "assistant",
                            replyText, "text");

                    JSONObject json = new JSONObject();
                    json.put("type", "text");
                    json.put("content", replyText);
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(json.toJSONString()));
                }
                // 自动为首次对话生成标题
                autoGenerateTitleIfNeeded(conversationId, userId, messages, emitter);
                emitter.send(SseEmitter.event().name("done").data(""));
                emitter.complete();
            }

        } catch (IllegalArgumentException e) {
            try {
                JSONObject errJson = new JSONObject();
                errJson.put("type", "error");
                errJson.put("content", e.getMessage());
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(errJson.toJSONString()));
            } catch (IOException ex) {
                // ignore
            }
            emitter.complete();
        } catch (Exception e) {
            try {
                JSONObject errJson = new JSONObject();
                errJson.put("type", "error");
                errJson.put("content", e.getMessage() != null ? e.getMessage() : "服务器内部错误");
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(errJson.toJSONString()));
            } catch (IOException ex) {
                // ignore
            }
            emitter.complete();
        }

        return emitter;
    }

    /**
     * 当对话标题为默认"新对话"时，根据用户首条消息自动生成标题
     */
    private void autoGenerateTitleIfNeeded(int conversationId, int userId,
                                           List<Message> messages, SseEmitter emitter) {
        try {
            AiConversation conv = conversationService.getById(conversationId);
            if (conv == null || !"新对话".equals(conv.getTitle())) return;

            // 提取第一条用户消息的文本内容
            String firstUserText = null;
            for (Message msg : messages) {
                if (msg instanceof UserMessage) {
                    String content = msg.getText();
                    try {
                        JSONObject obj = JSONObject.parseObject(content);
                        firstUserText = obj.getString("text");
                    } catch (Exception e) {
                        firstUserText = content;
                    }
                    if (firstUserText != null && !firstUserText.isEmpty()) break;
                }
            }

            if (firstUserText == null || firstUserText.isEmpty()) return;

            // 调用 AI 生成简短标题（3-10字）
            String generated = chatClient.prompt()
                    .system("你是一个对话标题生成器。根据用户的第一条消息，生成一个简洁的对话标题（3-10个字）。" +
                            "直接输出标题，不要解释，不要引号，不要多余内容。")
                    .user(firstUserText)
                    .call()
                    .content();

            if (generated != null && !generated.isEmpty()) {
                // 清理可能的引号和多余空白
                generated = generated.replaceAll("[\"\"'']", "").trim();
                if (generated.length() > 20) {
                    generated = generated.substring(0, 20);
                }
                conversationService.updateTitle(userId, conversationId, generated);

                // 通过 SSE 推送新标题到前端
                JSONObject titleJson = new JSONObject();
                titleJson.put("type", "title");
                titleJson.put("content", generated);
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(titleJson.toJSONString()));
            }
        } catch (Exception e) {
            // 标题生成失败不影响主流程，仅打印日志
            System.err.println("自动生成对话标题失败: " + e.getMessage());
        }
    }
}
