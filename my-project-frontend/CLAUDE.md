# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
npm install          # Install dependencies
npm run dev          # Start Vite dev server (default http://localhost:5173)
npm run build        # Production build
npm run preview      # Preview production build
```

The dev server proxies `/api` requests to the backend at `http://localhost:8080` (see `vite.config.js` — auto-configured by Vite's proxy).

## Architecture Overview

**Stack:** Vue 3 (Composition API, `<script setup>`), Vite 4, Vue Router 4, Pinia, Element Plus 2.11, **Vant 4** (mobile), Axios, Quill Editor, markdown-it

### Key directories

```
src/
├── net/                  # HTTP layer
│   ├── index.js          # Axios wrapper, JWT management, login/logout, isUnauthorized/isRoleAdmin
│   └── api/              # API modules: user.js, forum.js, email.js, ai.js
├── router/index.js       # Vue Router config + beforeEach guards
├── store/index.js        # Pinia store (general)
├── components/           # Reusable components
├── layouts/              # Layout components
│   └── MobileLayout.vue  # Mobile shell (NavBar + TabBar)
├── utils/
│   └── device.js         # isMobile ref (768px breakpoint)
├── views/                # Page components
│   ├── welcome/          # Login, Register, ForgetPassword (responsive)
│   ├── forum/            # TopicList, TopicDetail (responsive via mobile/ subdir)
│   ├── mobile/           # Mobile-optimized page variants
│   │   ├── MobileTopicList.vue
│   │   └── MobileTopicDetail.vue
│   ├── settings/         # UserSetting, PrivacySetting, ForumSetting
│   ├── ai/               # AiAgent (responsive)
│   └── admin/            # Admin dashboard + sub-sections (responsive)
└── App.vue               # Root component — detects isMobile, wraps MobileLayout
```

### Responsive design pattern

The app uses **template-level branching** with `isMobile` (from `src/utils/device.js`):

```
App.vue
  ├─ MobileLayout (v-if="isMobile")  → Vant NavBar + TabBar + router-view
  └─ router-view (v-else)            → Desktop layout untouched
```

Pages check `isMobile` at template root:
- `v-if="isMobile"` → render Vant mobile variant
- `v-else` → render desktop Element Plus variant

Key rules:
- **TabBar** hides on detail pages (`/topic-detail/`)
- **NavBar** shows back arrow only on sub-pages (path depth > 2)
- **Welcome** page hides NavBar and TabBar entirely

### HTTP layer (`src/net/index.js`)

All HTTP requests go through Axios with automatic JWT attachment:

- `post(url, data, success, failure)` — POST with Bearer token
- `get(url, success, failure)` — GET with Bearer token
- `fetchPost(url, data)` — raw Fetch API (used by AI chat for SSE streaming, returns ReadableStream)
- `login(username, password, remember, success, failure)` — form-login, stores JWT
- `logout(success, failure)` — calls logout API, clears stored token

Token storage: `localStorage` when "remember me" is checked, `sessionStorage` otherwise. Token expiry is checked client-side; 401 responses trigger automatic redirect to login.

### Router guard logic

```
welcome/*  + authenticated  → redirect to /index
/index/*   + unauthenticated → redirect to /
/admin/*   + not admin      → redirect to /index
```

### JWT flow in frontend

1. `login()` → POST `/api/auth/login` (form-encoded) → stores `{token, expire, role}` in localStorage/sessionStorage
2. Every subsequent request: `accessHeader()` reads stored token, attaches `Authorization: Bearer <token>`
3. `takeAccessToken()` checks expiry before each use; expired tokens trigger `deleteAccessToken()` + warning
4. `logout()` → GET `/api/auth/logout` → clears storage → redirects

### Component overview

| Component | Purpose |
|-----------|---------|
| `AiChatWindow.vue` | Floating AI chat (bottom-right), SSE streaming, markdown-it rendering |
| `Card.vue` | Generic content card wrapper |
| `ColorDot.vue` | Colored dot for topic type indicators |
| `InteractButton.vue` | Like/collect toggle button |
| `LightCard.vue` | Light-themed card variant |
| `TopicCollectList.vue` | User's collected topics |
| `TopicCommentEditor.vue` | Quill editor for posting comments |
| `TopicEditor.vue` | Quill editor for creating/editing topics |
| `TopicTag.vue` | Topic type tag with color |
| `UserEditor.vue` | User profile editor |
| `UserInfo.vue` | User info display card |
| `Weather.vue` | Weather display widget |

### Pinia store (`src/store/index.js`)

The `general` store holds:
- `user` — current user info (id, username, role, avatar)
- `forum.types` — cached topic type list (loaded once, reused across pages)

### Quill rich text

Both topic content and comments use Quill Delta JSON format. The editor components (`TopicEditor.vue`, `TopicCommentEditor.vue`) embed Quill with image resize and super-solution plugins. Content is stored/transmitted as Delta JSON, rendered back with `quill-delta-to-html` library.

### AI chat implementation

`AiChatWindow.vue` sends full conversation context (array of `{type, text}`) to `POST /api/ai/chat` via `fetchPost()`. The response is an SSE stream consumed through a `ReadableStream` reader — each chunk is appended to the assistant's message text in real-time. Key states: loading (pulsing dots), error ("生成失败，请重试"), empty (welcome message).

The standalone `AiAgent.vue` (at `/index/ai-agent`) uses a dedicated conversation API with history sidebar, multi-turn chat, image upload, web search toggle, and file upload. On mobile, the sidebar is replaced with an ActionSheet triggered by a "历史" button.

### Admin views

Admin pages in `views/admin/` use a common layout (`AdminView.vue`) with sidebar navigation. Sub-sections:
- `UserAdmin.vue` — user list with search, edit dialog, ban/unban, password reset
- `EmailAdmin.vue` — email send records, resend capability
- `ForumAdmin.vue` — tabs for topic management (`ForumTopicAdmin`), type management (`ForumTopicTypeAdmin`), and prohibited words (`ForumTopicProhibitedAdmin`)

On mobile, AdminView renders a simplified layout with NavBar + no sidebar, keeping the admin sub-pages functional.
