# 校园 AI 助手 Phase 1 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在论坛侧边栏增加"校园AI助手"独立页面，支持多对话管理、AI 对话（DeepSeek）、联网搜索（Tavily）、论坛帖子搜索

**Architecture:** 后端新增 `ai_conversation` + `ai_conversation_message` 两张表，通过 MyBatis-Plus 实体和 Mapper 管理；新增 `AiConversationService` 处理对话 CRUD，修改 `AiServiceImpl` 集成对话上下文管理和可配置工具；前端新增 `AiAgent.vue` 页面组件实现 ChatGPT 风格两栏布局

**Tech Stack:** Spring Boot 3.5.8 / Spring AI 1.1.2 / MyBatis-Plus 3.5.15 / Vue 3 + Element Plus / markdown-it / Tavily Search API

---

### Task 1: 创建数据库表

**Files:**
- Create: `my-project-backend/src/main/resources/db/migration/V1__create_ai_conversation_tables.sql`

- [ ] **Step 1: 编写建表 SQL**

根据设计文档，在 `study.sql` 末尾追加（或新建 SQL 文件）：

```sql
-- ai_conversation: 对话会话
CREATE TABLE IF NOT EXISTS `ai_conversation` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL COMMENT '所属用户',
    `title` VARCHAR(100) DEFAULT '新对话' COMMENT '对话标题',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话会话';

-- ai_conversation_message: 消息记录
CREATE TABLE IF NOT EXISTS `ai_conversation_message` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `conversation_id` INT NOT NULL COMMENT '所属对话ID',
    `role` VARCHAR(20) NOT NULL COMMENT 'user/assistant/system',
    `content` TEXT NOT NULL COMMENT '消息内容(JSON格式)',
    `message_type` VARCHAR(20) DEFAULT 'text' COMMENT 'text/image/tool_call/tool_result',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_conversation_id` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话消息';
```

### Task 2: 后端实体 + Mapper

**Files:**
- Create: `my-project-backend/src/main/java/com/example/entity/dto/AiConversation.java`
- Create: `my-project-backend/src/main/java/com/example/entity/dto/AiConversationMessage.java`
- Create: `my-project-backend/src/main/java/com/example/mapper/AiConversationMapper.java`
- Create: `my-project-backend/src/main/java/com/example/mapper/AiConversationMessageMapper.java`

- [ ] **Step 1: 创建 AiConversation 实体**

```java
package com.example.entity.dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("ai_conversation")
public class AiConversation {
    @TableId
    Integer id;
    Integer userId;
    String title;
    Date createdTime;
    Date updatedTime;
}
```

- [ ] **Step 2: 创建 AiConversationMessage 实体**

```java
package com.example.entity.dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("ai_conversation_message")
public class AiConversationMessage {
    @TableId
    Integer id;
    Integer conversationId;
    String role;
    String content;
    String messageType;
    Date createdTime;
}
```

- [ ] **Step 3: 创建 Mapper**

```java
package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.dto.AiConversation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiConversationMapper extends BaseMapper<AiConversation> {
}
```

```java
package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.dto.AiConversationMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiConversationMessageMapper extends BaseMapper<AiConversationMessage> {
}
```

- [ ] **Step 4: 验证编译通过**

Run: `cd my-project-backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add my-project-backend/src/main/java/com/example/entity/dto/AiConversation.java my-project-backend/src/main/java/com/example/entity/dto/AiConversationMessage.java my-project-backend/src/main/java/com/example/mapper/AiConversationMapper.java my-project-backend/src/main/java/com/example/mapper/AiConversationMessageMapper.java study.sql
git commit -m "feat: add AI conversation entities and mappers"
```

### Task 3: AiConversationService - 对话 CRUD 后端逻辑

**Files:**
- Create: `my-project-backend/src/main/java/com/example/service/AiConversationService.java`
- Create: `my-project-backend/src/main/java/com/example/service/impl/AiConversationServiceImpl.java`
- Modify: `my-project-backend/src/main/resources/application-dev.yml` (追加 Tavily 配置)

- [ ] **Step 1: 创建 Service 接口**

```java
package com.example.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.dto.AiConversation;

import java.util.List;

public interface AiConversationService extends IService<AiConversation> {
    List<AiConversation> listConversations(int userId);
    AiConversation createConversation(int userId, String title);
    void deleteConversation(int userId, int id);
    void saveMessage(int conversationId, String role, String content, String messageType);
    List<JSONObject> loadMessages(int conversationId);
}
```

- [ ] **Step 2: 创建 ServiceImpl**

```java
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
        // 验证所有权
        AiConversation conv = this.getById(id);
        if (conv == null || conv.getUserId() != userId) return;
        // 删除消息
        messageMapper.delete(new QueryWrapper<AiConversationMessage>()
                .eq("conversation_id", id));
        // 删除对话
        this.removeById(id);
    }

    @Override
    public void saveMessage(int conversationId, String role, String content, String messageType) {
        AiConversationMessage msg = new AiConversationMessage();
        msg.setConversationId(conversationId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setMessageType(messageType != null ? messageType : "text");
        msg.setCreatedTime(new Date());
        messageMapper.insert(msg);

        // 更新对话的更新时间
        AiConversation conv = new AiConversation();
        conv.setId(conversationId);
        conv.setUpdatedTime(new Date());
        this.updateById(conv);
    }

    @Override
    public List<JSONObject> loadMessages(int conversationId) {
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
}
```

- [ ] **Step 3: 修改 application-dev.yml，追加 Tavily 配置**

追加以下内容到 `application-dev.yml` 末尾：

```yaml
  ai:
    tavily:
      api-key: 'your-tavily-api-key'
```

注意：需要用户提供实际的 Tavily API Key。

- [ ] **Step 4: 编译验证**

Run: `cd my-project-backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add my-project-backend/src/main/java/com/example/service/AiConversationService.java my-project-backend/src/main/java/com/example/service/impl/AiConversationServiceImpl.java my-project-backend/src/main/resources/application-dev.yml
git commit -m "feat: add AiConversationService with CRUD and Tavily config"
```

### Task 4: AiConversationController - 对话管理 API

**Files:**
- Create: `my-project-backend/src/main/java/com/example/controller/AiConversationController.java`

- [ ] **Step 1: 创建 Controller**

```java
package com.example.controller;

import com.example.entity.RestBean;
import com.example.entity.dto.AiConversation;
import com.example.service.AiConversationService;
import com.example.utils.Const;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Min;
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
            @RequestBody(required = false) String title) {
        return RestBean.success(service.createConversation(userId, title));
    }

    @DeleteMapping("/conversations/{id}")
    public RestBean<Void> deleteConversation(
            @RequestAttribute(Const.ATTR_USER_ID) int userId,
            @PathVariable @Min(1) int id) {
        service.deleteConversation(userId, id);
        return RestBean.success();
    }

    @GetMapping("/conversations/{id}/messages")
    public RestBean<List<com.alibaba.fastjson2.JSONObject>> getMessages(
            @RequestAttribute(Const.ATTR_USER_ID) int userId,
            @PathVariable @Min(1) int id) {
        // 验证所有权
        AiConversation conv = service.getById(id);
        if (conv == null || conv.getUserId() != userId)
            return RestBean.failure(403, "无权访问此对话");
        return RestBean.success(service.loadMessages(id));
    }
}
```

注意：需要检查 `Const.ATTR_USER_ID` 的值。在 `Const.java` 中确认该常量。

- [ ] **Step 2: 编译验证**

Run: `cd my-project-backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add my-project-backend/src/main/java/com/example/controller/AiConversationController.java
git commit -m "feat: add AiConversationController REST API"
```

### Task 5: 修改 AiServiceImpl - 集成对话上下文和 Tavily 搜索工具

**Files:**
- Modify: `my-project-backend/src/main/java/com/example/service/impl/AiServiceImpl.java`
- Create: `my-project-backend/src/main/java/com/example/service/ForumTools.java` (修改，增加 web_search 工具)
- Modify: `my-project-backend/src/main/java/com/example/service/AiService.java`

- [ ] **Step 1: 修改 AiService 接口**

```java
package com.example.service;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AiService {
    SseEmitter chatWithAi(int conversationId, int userId, String text,
                          List<String> imageUrls, boolean enableWebSearch);
}
```

- [ ] **Step 2: 创建 Tavily 搜索客户端工具类**

```java
package com.example.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import jakarta.annotation.Resource;

@Component
public class WebSearchTools {

    @Value("${spring.ai.tavily.api-key:}")
    String tavilyApiKey;

    @Resource
    RestTemplate restTemplate;

    @Tool(description = "搜索互联网上的最新信息。当用户询问实时信息、新闻或你不确定的内容时使用。")
    String web_search(@ToolParam(description = "搜索关键词") String keyword) {
        if (tavilyApiKey.isEmpty()) {
            return "网络搜索功能未配置，请联系管理员设置 Tavily API Key。";
        }
        try {
            JSONObject request = new JSONObject();
            request.put("api_key", tavilyApiKey);
            request.put("query", keyword);
            request.put("max_results", 5);

            JSONObject response = restTemplate.postForObject(
                    "https://api.tavily.com/search",
                    request,
                    JSONObject.class);

            if (response == null || response.getJSONArray("results") == null) {
                return "未找到相关结果。";
            }

            JSONArray results = response.getJSONArray("results");
            StringBuilder sb = new StringBuilder();
            sb.append("以下是网络搜索结果：\n\n");
            for (int i = 0; i < results.size(); i++) {
                JSONObject item = results.getJSONObject(i);
                sb.append(i + 1).append(". ").append(item.getString("title")).append("\n");
                sb.append("   ").append(item.getString("content")).append("\n");
                sb.append("   来源：").append(item.getString("url")).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "网络搜索时发生错误：" + e.getMessage();
        }
    }
}
```

- [ ] **Step 3: 在 pom.xml 中添加 RestTemplate Bean（如果没有的话）**

确认 `RestTemplate` 是否需要显式配置。检查是否有现成的配置类。如果没有，在启动类或配置类中添加：

```java
// 在某个 @Configuration 类中添加，或检查是否已有
@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

- [ ] **Step 4: 重写 AiServiceImpl.java**

```java
package com.example.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.service.AiConversationService;
import com.example.service.AiService;
import com.example.service.ForumTools;
import com.example.service.WebSearchTools;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.Media;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
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
    AiConversationService conversationService;

    ChatClient chatClient;

    @PostConstruct
    public void init() {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你是一个校园论坛的AI助手，名字叫「校园AI助手」。你友善、专业，能够帮助学生解答问题。" +
                        "你可以在论坛中搜索帖子、获取最新的帖子信息。" +
                        "当需要互联网上的最新信息时，你可以进行网络搜索。" +
                        "请用中文回复。")
                .build();
    }

    @Override
    public SseEmitter chatWithAi(int conversationId, int userId, String text,
                                 List<String> imageUrls, boolean enableWebSearch) {
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        try {
            // 1. 从数据库加载历史消息
            List<JSONObject> history = conversationService.loadMessages(conversationId);
            List<Message> messages = new ArrayList<>();

            // 2. 将历史消息转为 Spring AI Message
            for (JSONObject msg : history) {
                String role = msg.getString("type");
                String content = msg.getString("text");
                if ("user".equals(role)) {
                    messages.add(new UserMessage(content));
                } else if ("assistant".equals(role)) {
                    messages.add(new AssistantMessage(content));
                }
            }

            // 3. 构建当前用户消息（含可能的图片）
            if (imageUrls != null && !imageUrls.isEmpty()) {
                // 有多模态消息（图片理解）
                var userMsgBuilder = UserMessage.builder().text(text);
                for (String url : imageUrls) {
                    userMsgBuilder.media(Media.builder()
                            .mimeType(MimeTypeUtils.IMAGE_PNG)
                            .data(url)
                            .build());
                }
                messages.add(userMsgBuilder.build());
            } else {
                messages.add(new UserMessage(text));
            }

            // 4. 保存用户消息到数据库
            JSONObject userContent = new JSONObject();
            userContent.put("text", text);
            if (imageUrls != null && !imageUrls.isEmpty()) {
                userContent.put("imageUrls", imageUrls);
            }
            conversationService.saveMessage(conversationId, "user",
                    userContent.toJSONString(),
                    (imageUrls != null && !imageUrls.isEmpty()) ? "image" : "text");

            // 5. 构建 ChatClient.PromptRequestSpec
            var promptSpec = chatClient.prompt()
                    .messages(messages);

            // 6. 注册工具
            List<ToolCallback> toolCallbacks = new ArrayList<>();
            toolCallbacks.addAll(List.of(ToolCallbacks.from(forumTools)));
            if (enableWebSearch) {
                toolCallbacks.addAll(List.of(ToolCallbacks.from(webSearchTools)));
            }
            promptSpec.toolCallbacks(toolCallbacks.toArray(new ToolCallback[0]));

            // 7. 流式调用，发送 SSE
            Flux<String> flux = promptSpec.stream().content();
            StringBuilder fullReply = new StringBuilder();

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
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data(error.getMessage() != null ? error.getMessage() : "unknown"));
                        } catch (IOException e) {
                            // ignore
                        }
                        emitter.completeWithError(error);
                    },
                    () -> {
                        // 8. 保存完整 AI 回复到数据库
                        conversationService.saveMessage(conversationId, "assistant",
                                fullReply.toString(), "text");
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

        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }
}
```

- [ ] **Step 5: 修改 AiChatController.java**

```java
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

    /**
     * 新版流式对话接口 - 按会话ID发送消息
     * 请求体: { "conversationId": 1, "text": "你好", "imageUrls": [], "enableWebSearch": false }
     */
    @PostMapping(value = "/chat/{conversationId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatWithAI(
            @PathVariable int conversationId,
            @RequestAttribute(Const.ATTR_USER_ID) int userId,
            @RequestBody JSONObject body) {
        String text = body.getString("text");
        List<String> imageUrls = body.getJSONArray("imageUrls") != null
                ? body.getJSONArray("imageUrls").toList(String.class)
                : List.of();
        boolean enableWebSearch = body.getBooleanValue("enableWebSearch");
        return service.chatWithAi(conversationId, userId, text, imageUrls, enableWebSearch);
    }
}
```

注意：需要确认 `Const.ATTR_USER_ID` 的确切值。在项目代码中搜索该常量。

- [ ] **Step 6: 编译验证**

Run: `cd my-project-backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add my-project-backend/src/main/java/com/example/service/impl/AiServiceImpl.java my-project-backend/src/main/java/com/example/service/AiService.java my-project-backend/src/main/java/com/example/service/WebSearchTools.java my-project-backend/src/main/java/com/example/controller/AiChatController.java
git commit -m "feat: integrate conversation context, Tavily search, and image support into AI chat"
```

### Task 6: 前端 AI Agent 页面 - AiAgent.vue

**Files:**
- Create: `my-project-frontend/src/views/ai/AiAgent.vue`
- Modify: `my-project-frontend/src/router/index.js` (加路由)
- Modify: `my-project-frontend/src/views/IndexView.vue` (加侧边栏菜单项)
- Modify: `my-project-frontend/src/net/api/ai.js` (加 API 封装)

- [ ] **Step 1: 扩展 ai.js API 层**

```javascript
import {post, get, fetchPost} from '@/net'
import {accessHeader} from '@/net/index.js'

// 原有 SSE 聊天函数保留（论坛区仍使用）
export const apiChatWithAI = async (context, onMessage, onError, onComplete) => {
    const response = await fetchPost('/api/ai/chat', context)
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        const text = decoder.decode(value)
            .replaceAll("data:", "")
            .replaceAll("\n", "")
        onMessage(text)
    }
    onComplete()
}

// ===== 对话管理 API =====

// 获取对话列表
export const apiConversationList = (success) =>
    get('/api/ai/conversations', success)

// 创建新对话
export const apiConversationCreate = (title, success) =>
    post('/api/ai/conversations', {title}, success)

// 删除对话
export const apiConversationDelete = (id, success) =>
    fetch(`/api/ai/conversations/${id}`, {
        method: 'DELETE',
        headers: accessHeader()
    }).then(r => r.json()).then(data => success && success(data))

// 获取对话消息历史
export const apiConversationMessages = (id, success) =>
    get(`/api/ai/conversations/${id}/messages`, success)

// SSE 流式对话（按会话ID）
export const apiChatWithConversation = async (conversationId, body, onMessage, onError, onComplete) => {
    try {
        const response = await fetch(`/api/ai/chat/${conversationId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                ...accessHeader()
            },
            body: JSON.stringify(body)
        })
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            buffer += decoder.decode(value, {stream: true});
            const lines = buffer.split('\n');
            buffer = lines.pop() || ''; // 保留不完整的行

            for (const line of lines) {
                if (line.startsWith('event: ')) {
                    const eventType = line.slice(7).trim();
                    if (eventType === 'done') {
                        onComplete();
                        return;
                    }
                } else if (line.startsWith('data: ')) {
                    try {
                        const data = JSON.parse(line.slice(6));
                        if (data.type === 'text') {
                            onMessage(data.content);
                        }
                    } catch (e) {
                        // 非 JSON 数据直接传递
                        onMessage(line.slice(6));
                    }
                }
            }
        }
        onComplete();
    } catch (e) {
        onError(e);
    }
}
```

- [ ] **Step 2: 创建 AiAgent.vue**

这是一个较大的组件，包含左侧历史对话栏 + 右侧聊天区。

```vue
<template>
  <div class="ai-agent-container">
    <!-- 左侧历史对话栏 -->
    <div class="history-sidebar">
      <div class="history-header">
        <el-button type="primary" @click="createNewConversation" :icon="Plus" size="large">
          新对话
        </el-button>
      </div>
      <el-scrollbar class="history-list">
        <div v-for="conv in conversations"
             :key="conv.id"
             class="history-item"
             :class="{ active: activeConversationId === conv.id }"
             @click="switchConversation(conv.id)">
          <div class="history-item-title">
            <el-icon><ChatDotSquare /></el-icon>
            <span class="title-text">{{ conv.title }}</span>
          </div>
          <el-button v-if="activeConversationId === conv.id"
                     text
                     type="danger"
                     size="small"
                     :icon="Delete"
                     @click.stop="deleteConversation(conv.id)" />
        </div>
        <div v-if="conversations.length === 0" class="empty-history">
          <el-empty description="暂无对话" :image-size="60" />
        </div>
      </el-scrollbar>
    </div>

    <!-- 右侧聊天区 -->
    <div class="chat-area">
      <!-- 消息列表 -->
      <div class="messages-container" ref="messagesRef">
        <div v-if="messages.length === 0" class="welcome-placeholder">
          <div class="welcome-icon">🤖</div>
          <h2>校园AI助手</h2>
          <p>你好！我是校园AI助手，可以帮你：</p>
          <div class="suggestions">
            <el-tag @click="sendQuickQuestion('搜索论坛中关于编程的帖子')">搜索论坛帖子</el-tag>
            <el-tag @click="sendQuickQuestion('最近论坛有什么热门帖子？')">最新帖子</el-tag>
            <el-tag @click="sendQuickQuestion('今天有什么科技新闻？')">网络搜索</el-tag>
          </div>
        </div>

        <div v-for="(msg, idx) in messages" :key="idx"
             class="message-wrapper"
             :class="msg.role === 'user' ? 'user-message' : 'assistant-message'">
          <div class="avatar">
            <el-avatar :size="36" v-if="msg.role === 'assistant'" icon="ChatDotSquare" />
            <el-avatar :size="36" v-else :src="store.user.avatar ? store.avatarUrl : undefined">
              {{ store.user.username?.[0]?.toUpperCase() }}
            </el-avatar>
          </div>
          <div class="message-bubble">
            <div class="message-content" v-html="renderMarkdown(msg.content)"></div>
            <div v-if="idx === messages.length - 1 && isLoading" class="typing-indicator">
              <span class="dot"></span><span class="dot"></span><span class="dot"></span>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="input-area">
        <div class="input-toolbar">
          <el-upload
              :show-file-list="false"
              :before-upload="handleImageUpload"
              accept="image/*"
              action="">
            <el-button :icon="Picture" circle text />
          </el-upload>
          <span class="upload-tip">上传图片</span>
        </div>
        <div class="input-row">
          <el-input
              v-model="inputText"
              type="textarea"
              :rows="3"
              placeholder="输入你的问题..."
              @keydown.enter.prevent="sendMessage"
              :disabled="isLoading" />
          <div class="input-actions">
            <el-switch
                v-model="enableWebSearch"
                active-text="🌐 联网搜索"
                inline-prompt
                size="small" />
            <el-button type="primary" @click="sendMessage" :loading="isLoading" :icon="Promotion">
              发送
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useStore } from '@/store'
import { Plus, Delete, ChatDotSquare, Promotion, Picture } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import {
  apiConversationList,
  apiConversationCreate,
  apiConversationDelete,
  apiConversationMessages,
  apiChatWithConversation
} from '@/net/api/ai'
import { ElMessage, ElMessageBox } from 'element-plus'

const md = new MarkdownIt({ html: true })
const store = useStore()

const conversations = ref([])
const activeConversationId = ref(null)
const messages = ref([])
const inputText = ref('')
const isLoading = ref(false)
const enableWebSearch = ref(false)
const messagesRef = ref(null)

// 当前上传的图片（用于图片理解）
const uploadedImages = ref([])

onMounted(() => {
  loadConversations()
})

function renderMarkdown(text) {
  if (!text) return ''
  return md.render(text)
}

function loadConversations() {
  apiConversationList(data => {
    conversations.value = data || []
  })
}

function createNewConversation() {
  apiConversationCreate('新对话', data => {
    conversations.value.unshift(data)
    activeConversationId.value = data.id
    messages.value = []
    inputText.value = ''
  })
}

function switchConversation(id) {
  if (isLoading.value) return
  activeConversationId.value = id
  messages.value = []
  apiConversationMessages(id, data => {
    messages.value = (data || []).map(m => ({
      role: m.type,
      content: parseMessageContent(m.text),
      messageType: m.messageType
    }))
    scrollToBottom()
  })
}

function parseMessageContent(content) {
  try {
    const obj = JSON.parse(content)
    return obj.text || content
  } catch {
    return content
  }
}

function deleteConversation(id) {
  ElMessageBox.confirm('确定删除此对话？', '提示', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    apiConversationDelete(id, () => {
      conversations.value = conversations.value.filter(c => c.id !== id)
      if (activeConversationId.value === id) {
        activeConversationId.value = null
        messages.value = []
      }
      ElMessage.success('已删除')
    })
  }).catch(() => {})
}

function handleImageUpload(file) {
  // 上传图片到现有上传接口
  const formData = new FormData()
  formData.append('file', file)
  
  // 使用现有的图片上传 API
  import('@/net').then(({post}) => {
    post('/api/image/cache', formData, url => {
      uploadedImages.value.push(url)
      ElMessage.success('图片已上传')
    }, () => {
      ElMessage.error('图片上传失败')
    })
  })
  return false // 阻止默认上传行为
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text && uploadedImages.value.length === 0) return
  if (!activeConversationId.value) {
    // 如果没有对话，自动创建
    await new Promise(resolve => {
      apiConversationCreate('新对话', data => {
        conversations.value.unshift(data)
        activeConversationId.value = data.id
        resolve()
      })
    })
  }
  if (!activeConversationId.value) return

  const userMsg = {
    role: 'user',
    content: text,
    messageType: uploadedImages.value.length > 0 ? 'image' : 'text'
  }
  messages.value.push(userMsg)
  const currentText = text
  inputText.value = ''

  // 准备 AI 回复占位
  const assistantMsg = { role: 'assistant', content: '' }
  messages.value.push(assistantMsg)
  isLoading.value = true
  scrollToBottom()

  const body = {
    text: currentText,
    imageUrls: uploadedImages.value,
    enableWebSearch: enableWebSearch.value
  }
  uploadedImages.value = []

  apiChatWithConversation(
      activeConversationId.value,
      body,
      (chunk) => {
        // onMessage: 追加内容
        assistantMsg.content += chunk
        scrollToBottom()
      },
      (error) => {
        // onError
        assistantMsg.content = '生成失败，请重试。'
        isLoading.value = false
      },
      () => {
        // onComplete
        isLoading.value = false
        // 刷新对话列表（更新时间变化）
        loadConversations()
      }
  )
}

function sendQuickQuestion(text) {
  inputText.value = text
  sendMessage()
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}
</script>

<style scoped lang="less">
.ai-agent-container {
  display: flex;
  height: calc(100vh - 105px);
  background: var(--el-bg-color-page);
  border-radius: 8px;
  overflow: hidden;
}

.history-sidebar {
  width: 240px;
  min-width: 240px;
  background: var(--el-bg-color);
  border-right: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;

  .history-header {
    padding: 16px;
    border-bottom: 1px solid var(--el-border-color-light);

    .el-button {
      width: 100%;
    }
  }

  .history-list {
    flex: 1;
    padding: 8px;

    .history-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 10px 12px;
      border-radius: 8px;
      cursor: pointer;
      transition: background 0.2s;
      margin-bottom: 2px;

      &:hover {
        background: var(--el-fill-color-light);
      }

      &.active {
        background: var(--el-color-primary-light-9);
        color: var(--el-color-primary);
      }

      .history-item-title {
        display: flex;
        align-items: center;
        gap: 8px;
        overflow: hidden;

        .title-text {
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          font-size: 14px;
        }
      }
    }

    .empty-history {
      margin-top: 40px;
    }
  }
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color);
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px 48px;
}

.welcome-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  color: var(--el-text-color-secondary);

  .welcome-icon {
    font-size: 64px;
    margin-bottom: 16px;
  }

  h2 {
    margin: 0 0 8px;
    font-size: 24px;
    color: var(--el-text-color-primary);
  }

  p {
    margin: 0 0 24px;
    font-size: 15px;
  }

  .suggestions {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
    justify-content: center;

    .el-tag {
      cursor: pointer;
      padding: 8px 16px;
      font-size: 14px;
      border-radius: 20px;
    }
  }
}

.message-wrapper {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;

  &.user-message {
    flex-direction: row-reverse;

    .message-bubble {
      background: var(--el-color-primary-light-8);
      border-radius: 18px 18px 4px 18px;
    }
  }

  &.assistant-message {
    .message-bubble {
      background: var(--el-fill-color-light);
      border-radius: 18px 18px 18px 4px;
    }
  }

  .message-bubble {
    max-width: 70%;
    padding: 12px 18px;
    line-height: 1.6;
    font-size: 14px;
    word-break: break-word;

    :deep(p) {
      margin: 0 0 8px;
      &:last-child { margin: 0; }
    }

    :deep(pre) {
      background: var(--el-fill-color-darker);
      padding: 12px;
      border-radius: 8px;
      overflow-x: auto;
    }

    :deep(code) {
      font-size: 13px;
    }
  }
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 4px 0;

  .dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--el-text-color-secondary);
    animation: typing 1.4s infinite;

    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

@keyframes typing {
  0%, 60%, 100% { opacity: 0.3; transform: translateY(0); }
  30% { opacity: 1; transform: translateY(-4px); }
}

.input-area {
  padding: 16px 24px;
  border-top: 1px solid var(--el-border-color-light);
  background: var(--el-bg-color);

  .input-toolbar {
    display: flex;
    align-items: center;
    gap: 4px;
    margin-bottom: 8px;

    .upload-tip {
      font-size: 12px;
      color: var(--el-text-color-secondary);
    }
  }

  .input-row {
    .el-textarea {
      :deep(textarea) {
        resize: none;
        border-radius: 12px;
        padding: 12px 16px;
      }
    }

    .input-actions {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 8px;
    }
  }
}
</style>

<style>
.markdown-body p { margin: 0 0 8px; }
.markdown-body p:last-child { margin: 0; }
</style>
```

- [ ] **Step 3: 修改路由配置**

在 `src/router/index.js` 的 `/index` 子路由中追加：

```javascript
{
  path: 'ai-agent',
  name: 'ai-agent',
  component: () => import('@/views/ai/AiAgent.vue'),
  meta: { requiresAuth: true }
}
```

找到 `/index` 的子路由数组（在 `children` 中），在 `privacy-setting` 条目之后添加。

- [ ] **Step 4: 修改 IndexView.vue 侧边栏**

在 `userMenu` 数组中新增一个分组：

```javascript
{
    title: '校园AI助手',
    icon: MagicStick,  // 或使用其他合适图标
    sub: [
        { title: 'AI 对话', icon: ChatLineSquare, index: '/index/ai-agent' }
    ]
}
```

需要导入 `MagicStick` 和 `ChatLineSquare` 图标：在 Element Plus 图标导入中补充。

实际图标选择建议：
- 使用 `MagicStick`（魔法棒）作为组图标
- 使用 `ChatLineSquare`（聊天对话框）作为子项图标

在 `IndexView.vue` 的 `<script setup>` 中，从 `@element-plus/icons-vue` 导入这两个图标。

- [ ] **Step 5: 验证前端构建**

Run: `cd my-project-frontend && npx vite build 2>&1 | head -30`
Expected: BUILD SUCCESS，无报错

- [ ] **Step 6: Commit**

```bash
git add my-project-frontend/src/views/ai/AiAgent.vue my-project-frontend/src/router/index.js my-project-frontend/src/views/IndexView.vue my-project-frontend/src/net/api/ai.js
git commit -m "feat: add AI Agent page with conversation sidebar and chat area"
```

### Task 7: 确认 Const.ATTR_USER_ID 和编译修复

**Files:**
- Read: `my-project-backend/src/main/java/com/example/utils/Const.java`

- [ ] **Step 1: 检查 Const.ATTR_USER_ID 常量**

```bash
grep -n "ATTR_USER_ID" my-project-backend/src/main/java/com/example/utils/Const.java
```

该常量在 Security 的 JwtAuthenticationFilter 中用于在 request attribute 中设置 userId。需要确认其名称与实际一致。

- [ ] **Step 2: 处理 DELETE 方法**

前端 API 使用了 `/api/ai/conversations/{id}` 的 DELETE 方法，但编写的前端代码误用了 `post`。确认前端 `apiConversationDelete` 使用正确的 HTTP 方法：

```javascript
export const apiConversationDelete = (id, success) =>
    fetch(`/api/ai/conversations/${id}`, {
        method: 'DELETE',
        headers: accessHeader()
    }).then(r => r.json()).then(data => success && success(data))
```

或使用 Axios 的 delete：

```javascript
import {delete as del} from '@/net'
export const apiConversationDelete = (id, success) =>
    del(`/api/ai/conversations/${id}`, success)
```

需要在 `src/net/index.js` 中导出 delete 方法。

- [ ] **Step 3: 全局编译验证**

```bash
cd my-project-backend && mvn compile -q
cd my-project-frontend && npx vite build 2>&1 | tail -10
```

Expected: 两者都 BUILD SUCCESS

- [ ] **Step 4: Commit 修复**

```bash
git add -A
git commit -m "fix: correct API methods and imports for AI agent"
```

---

## 自审检查

**设计覆盖检查：**
- ✅ 侧边栏入口（Task 6 Step 4）
- ✅ 独立路由页面（Task 6 Step 3）
- ✅ ChatGPT 风格两栏布局：历史栏 + 聊天区（Task 6 Step 2）
- ✅ 联网搜索开关（Task 6 Step 2，enableWebSearch switch）
- ✅ 对话管理 CRUD（Task 2-4）
- ✅ 消息持久化（Task 5）
- ✅ Tavily 网络搜索（Task 5 Step 2）
- ✅ 保留原有浮动 AiChatWindow（未删除）
- ❌ 图片上传/理解/生成 — Phase 2，未包含

**类型/签名一致性检查：**
- AiService 接口签名与 AiServiceImpl 一致 ✅
- Controller 路径与 Service 参数一致 ✅
- 前端 API 调用与后端 Controller 路径一致 ✅
- SSE 事件格式前后端匹配 ✅
