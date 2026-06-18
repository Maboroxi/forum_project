# ai-service

AI 会话与工具服务，默认端口 `8083`，注册名 `ai-service`。

## 对外接口

```text
GET     /api/ai/conversations
POST    /api/ai/conversations
DELETE  /api/ai/conversations/{id}
GET     /api/ai/conversations/{id}/messages
POST    /api/ai/chat
POST    /api/ai/chat/{conversationId}
```

所有接口必须经过 Gateway。Gateway 会注入用户身份和内部服务凭证，直接访问
`8083` 会返回 401。

## 依赖

```text
Nacos          服务发现
MySQL          ai_conversation、ai_conversation_message
OSS Service    读取当前用户上传的文本和图片
Monolith       临时提供论坛搜索内部接口
DeepSeek       主聊天模型
Tavily         可选联网搜索
SiliconFlow    可选图片识别和生成
```

## 配置

```text
AI_SERVER_PORT          默认 8083
NACOS_SERVER_ADDR       默认 localhost:8848
MYSQL_URL               默认 jdbc:mysql://localhost:3306/study_main
MYSQL_USERNAME          默认 root
MYSQL_PASSWORD          默认 123456
DEEPSEEK_KEY            DeepSeek API Key
DEEPSEEK_BASE_URL       默认 https://api.deepseek.com
TAVILY_API_KEY          Tavily API Key
SILICONFLOW_API_KEY     SiliconFlow API Key
INTERNAL_SERVICE_TOKEN  必须与 Gateway、单体和 OSS 一致
```

启动：

```bash
mvn spring-boot:run
```

聊天接口使用 SSE。每个会话同一时间只允许一个生成任务，历史上下文最多读取最近
20 条消息和 50,000 字符。
