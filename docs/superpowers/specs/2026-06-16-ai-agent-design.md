# 校园 AI 助手 — 设计文档

## 概述

在 IT 百马论坛中增加一个 AI Agent 功能页面"校园AI助手"，提供智能对话、联网搜索、帖子查询、图片生成与理解等能力。参考 `spring-ai-demo/` 中的 Spring AI 集成模式实现。

## 架构

```
Vue 3 AI Agent Page (独立路由 /index/ai-agent)
  │
  ├── SSE POST /api/ai/chat/{conversationId}  (流式对话)
  ├── REST API:  /api/ai/conversations/**     (对话CRUD)
  └── REST API:  /api/common/upload            (图片上传)
                    │
                    ▼
            Spring Boot Backend
              ├── AiServiceImpl     (ChatModel 流式 + 工具注册)
              ├── AiConversationService (对话管理)
              ├── ForumTools        (工具集: search_forum/web_search...)
              ├── TavilyClient      (网络搜索 HTTP 客户端)
              └── DB: ai_conversation + ai_conversation_message
```

## 前端设计

### 布局 (IndexView.vue 内)

```
<el-main>
  <div class="ai-agent-container">
    <div class="chat-history-sidebar">  ← 220px, 可折叠
      [+ 新对话]
      ─────────────
      📝 对话标题 1
      📝 对话标题 2
      ...
    </div>
    <div class="chat-main-area">       ← flex:1
      ┌─ 消息列表 ──────────────────┐
      │  用户消息 (右对齐)            │
      │  AI 回复 + Markdown 渲染     │
      │    ├ 工具调用指示器(内联)     │
      │    └ 图片/搜索结果展示        │
      └────────────────────────────┘
      ┌─ 输入区 ────────────────────┐
      │ [📎上传] [输入消息...] [发送]│
      │        [🌐 联网搜索]         │
      └────────────────────────────┘
    </div>
  </div>
</el-main>
```

### 组件结构

- `AiAgent.vue` — 主页面组件，承载历史栏和聊天区
- 不引入额外 UI 库，复用 Element Plus + markdown-it

### 路由

```js
{
  path: '/index/ai-agent',
  name: 'ai-agent',
  component: () => import('@/views/ai/AiAgent.vue'),
  meta: { requiresAuth: true }
}
```

### 侧边栏修改

在 `IndexView.vue` 的 `userMenu` 中增加一项：

```js
{
  title: '校园AI助手',
  icon: MagicStick,  // Element Plus 图标
  sub: [
    { title: 'AI 对话', icon: ChatLineSquare, index: '/index/ai-agent' }
  ]
}
```

### 对话历史管理

- `conversations` 数组 + 当前选中 `activeConversationId`
- 左侧栏点击切换 → 加载对应消息历史
- 新建对话 → `POST /api/ai/conversations` → 添加到列表并选中
- 删除对话 → Confirm 弹窗确认后删除

### 消息渲染

- 用户消息：纯文本或含图片（上传后显示缩略图 inline）
- AI 回复：markdown-it 渲染 + 工具调用指示器（"🔍 正在搜索帖子…"、"🌐 正在联网搜索…" 等轻量状态）
- 新建对话时自动滚动到底部

## 后端设计

### 数据库

```sql
CREATE TABLE ai_conversation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(100) DEFAULT '新对话',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
);

CREATE TABLE ai_conversation_message (
    id INT AUTO_INCREMENT PRIMARY KEY,
    conversation_id INT NOT NULL,
    role VARCHAR(20) NOT NULL COMMENT 'user/assistant/system',
    content TEXT NOT NULL COMMENT '消息内容(JSON)',
    message_type VARCHAR(20) DEFAULT 'text' COMMENT 'text/image/tool_call/tool_result',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation_id (conversation_id)
);
```

### API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ai/conversations` | 获取用户对话列表 |
| POST | `/api/ai/conversations` | 创建新对话 (body: `{title?}`) |
| DELETE | `/api/ai/conversations/{id}` | 删除对话及其消息 |
| GET | `/api/ai/conversations/{id}/messages` | 获取对话消息历史 |
| POST | `/api/ai/chat/{conversationId}` | 发送消息流式回复 (SSE) |

### 消息 API (POST /api/ai/chat/{conversationId})

请求体：
```json
{
  "text": "用户消息文本",
  "imageUrls": ["https://...", "https://..."],
  "enableWebSearch": true
}
```

SSE 事件流：
```
data:{"type":"text","content":"正在"}

data:{"type":"text","content":"思考..."}

data:{"type":"tool_call","tool":"web_search","status":"started"}
data:{"type":"tool_call","tool":"web_search","status":"completed","result":"..."}

data:{"type":"text","content":"完整的AI回复"}

data:{"type":"done"}
```

### 工具系统

后端工具基于 Spring AI `@Tool` 注解，参考 `spring-ai-demo/S20-manual-tool-execute`：

| 工具名 | 功能 | 实现方式 | 阶段 |
|--------|------|---------|------|
| `search_forum_posts` | 搜索论坛帖子 | 已有，ES TopicRepository | Phase 1 |
| `get_recent_posts` | 获取最近帖子 | 已有 | Phase 1 |
| `web_search` | 联网搜索 | 新增 Tavily HTTP 调用 | Phase 1 |
| `generate_image` | 文生图 | 新增，硅基流动 API | Phase 2 |
| `analyze_image` | 图片理解 | 新增，硅基流动 | Phase 2 |

工具注册方式：在 `AiServiceImpl` 中 `ChatClient` 构建时通过 `.defaultTools(...)` 注册。联网搜索工具根据请求中的 `enableWebSearch` 字段决定是否加入当前调用。

### 图片上传

复用现有的图片上传接口（或新增 `/api/common/upload`），上传后返回图片 URL，前端在请求中传入 `imageUrls`，后端构建包含 `Media` 的 `UserMessage`。

## 分阶段实施

### Phase 1 (当前实施)
- [ ] 数据库表创建 + 实体类 + Mapper
- [ ] AiConversationService（对话 CRUD）
- [ ] AiConversationController（对话管理 API）
- [ ] 修改 AiServiceImpl 集成对话上下文与可配置工具
- [ ] 新增 TavilyClient + web_search 工具
- [ ] 前端 AiAgent.vue 页面（历史栏 + 聊天区 + 输入区）
- [ ] 前端路由 + 侧边栏入口
- [ ] 前端 ai.js API 封装
- [ ] 保留原有 AiChatWindow.vue（论坛区浮动窗口）

### Phase 2 (后续配置)
- [ ] 配置硅基流动 API
- [ ] generate_image 工具
- [ ] analyze_image 工具
- [ ] 前端图片上传/预览/展示

## 未涉及的范围
- AI 回复的流式渲染已由现有 markdown-it 处理
- 鉴权复用现有 JWT 机制
- 对话标题自动生成暂不实现（默认"新对话"）
