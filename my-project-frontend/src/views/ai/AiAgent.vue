<template>
  <div style="height: 100%;">
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
      <div class="messages-container" ref="messagesRef" v-loading="switchLoading" element-loading-text="切换中...">
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
             :class="msg.role === 'user' ? 'user-message' : msg.role === 'tool' ? 'tool-message' : 'assistant-message'">
          <div class="avatar" v-if="msg.role !== 'tool'">
            <el-avatar :size="36" v-if="msg.role === 'assistant'" style="background: #e8f5e9; color: #2e7d32;">🤖</el-avatar>
            <el-avatar :size="36" v-else :src="store.user.avatar ? store.avatarUrl : undefined">
              {{ store.user.username?.[0]?.toUpperCase() }}
            </el-avatar>
          </div>
          <div class="message-bubble" :class="{ 'tool-bubble': msg.role === 'tool' }">
            <div v-if="msg.fileName" class="file-attach-badge">📄 {{ msg.fileName }}</div>
            <div v-if="msg.imageUrls && msg.imageUrls.length" class="image-preview">
              <img v-for="(url, i) in msg.imageUrls" :key="i" :src="url"
                   class="chat-image" @click="previewImage(url)" />
            </div>
            <div class="message-content" v-html="renderMarkdown(msg.content)"></div>
            <div v-if="idx === messages.length - 1 && isLoading" class="typing-indicator">
              <span class="dot"></span><span class="dot"></span><span class="dot"></span>
            </div>
          </div>
        </div>
      </div>

      <div class="input-area">
        <div class="input-toolbar">
          <div class="toolbar-buttons">
            <el-upload
                :show-file-list="false"
                :before-upload="handleImageUpload"
                accept="image/*"
                action="">
              <el-button :icon="Picture" circle text />
            </el-upload>
            <el-button :icon="Document" circle text @click="triggerFileUpload" />
            <input ref="fileInputRef" type="file" hidden
                   accept=".txt,.md,.csv,.json,.xml,.yaml,.yml,.log,.py,.java,.js,.ts,.vue,.html,.css,.sh,.sql,.rs,.go,.kt"
                   @change="handleTextFileUpload" />
          </div>
          <div class="upload-previews" v-if="uploadedImages.length > 0 || uploadedFileContent">
            <span v-for="(url, i) in uploadedImages" :key="i" class="img-preview-item">
              <img :src="url" class="img-thumb" @click="previewImage(url)" />
              <el-button class="img-remove" :icon="Close" size="small" circle
                         @click="uploadedImages.splice(i, 1)" />
            </span>
            <el-tag v-if="uploadedFileContent" closable
                    @close="uploadedFileContent = ''; uploadedFileName = ''"
                    type="warning" size="small">
              📄 {{ uploadedFileName }}
            </el-tag>
          </div>
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
                inactive-text=""
                size="small" />
            <el-button type="primary" @click="sendMessage" :loading="isLoading" :icon="Promotion">
              发送
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 图片查看器 -->
  <el-dialog v-model="imagePreviewVisible" :show-close="true"
             width="auto" align-center center
             class="image-preview-dialog">
    <img :src="previewImageUrl" style="max-width: 80vw; max-height: 80vh; border-radius: 4px;" />
  </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useStore } from '@/store'
import { Plus, Delete, ChatDotSquare, Promotion, Picture, Document, Close } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import {
  apiConversationList,
  apiConversationCreate,
  apiConversationDelete,
  apiConversationMessages,
  apiChatWithConversation,
  apiUploadTextFile
} from '@/net/api/ai'
import { takeAccessToken } from '@/net'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const md = new MarkdownIt({ html: true })
const store = useStore()

const conversations = ref([])
const activeConversationId = ref(null)
const messages = ref([])
const inputText = ref('')
const isLoading = ref(false)
const enableWebSearch = ref(false)
const uploadedImages = ref([])
const uploadedFileContent = ref('')
const uploadedFileName = ref('')
const fileInputRef = ref(null)
const messagesRef = ref(null)
const switchLoading = ref(false)
const imagePreviewVisible = ref(false)
const previewImageUrl = ref('')

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
    uploadedFileContent.value = ''
    uploadedFileName.value = ''
  })
}

function switchConversation(id) {
  if (isLoading.value) return
  activeConversationId.value = id
  switchLoading.value = true
  // 清除临时上传的附件
  uploadedImages.value = []
  uploadedFileContent.value = ''
  uploadedFileName.value = ''
  apiConversationMessages(id, data => {
    messages.value = (data || []).map(m => {
      let text = m.text
      let fileName = null
      let fileContent = null
      let imageUrls = null
      try {
        const obj = JSON.parse(m.text)
        text = obj.text || text
        fileName = obj.fileName || null
        fileContent = obj.fileContent || null
        imageUrls = obj.imageUrls || null  // 图片 URL
      } catch { /* 纯文本消息，直接使用 */ }
      // 工具结果转成历史提示显示
      if (m.messageType === 'tool_result') {
        let toolLabel = '🛠️ 已调用工具'
        if (text.includes('recognize_image')) toolLabel = '🖼️ 已识别图片内容'
        else if (text.includes('generate_image')) toolLabel = '🎨 已生成图片'
        return { role: 'tool', content: toolLabel, messageType: 'tool_result' }
      }
      return {
        role: m.type,
        content: text,
        messageType: m.messageType,
        fileName: fileName,
        fileContent: fileContent,
        imageUrls: imageUrls
      }
    })
    switchLoading.value = false
    scrollToBottom()
  })
}

function previewImage(url) {
  previewImageUrl.value = url
  imagePreviewVisible.value = true
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

async function handleImageUpload(file) {
  const formData = new FormData()
  formData.append('file', file)
  try {
    const {data} = await axios.post('/api/image/cache', formData, {
      headers: {
        'Authorization': `Bearer ${takeAccessToken()?.token}`
      }
    })
    if (data.code === 200) {
      // 后端返回的是 MinIO 路径（如 /cache/20240616/uuid）
      // 需要拼成完整 URL：http://localhost:8080/images/cache/20240616/uuid
      const fullUrl = axios.defaults.baseURL + '/images' + data.data
      uploadedImages.value.push(fullUrl)
      ElMessage.success('图片已上传')
    } else {
      ElMessage.error(data.message || '图片上传失败')
    }
  } catch (err) {
    const msg = err.response?.data?.message || '图片上传失败'
    ElMessage.error(msg)
    console.error('图片上传出错:', err)
  }
  return false
}

function triggerFileUpload() {
  fileInputRef.value?.click()
}

function handleTextFileUpload(event) {
  const file = event.target.files?.[0]
  if (!file) return

  const formData = new FormData()
  formData.append('file', file)
  apiUploadTextFile(formData, data => {
    uploadedFileContent.value = data.content
    uploadedFileName.value = data.filename
    ElMessage.success(`已读取「${data.filename}」(共 ${data.size} 字符)`)
  }, () => {
    ElMessage.error('文件上传失败')
  })
  event.target.value = ''
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text && uploadedImages.value.length === 0) return
  if (!activeConversationId.value) {
    await new Promise(resolve => {
      apiConversationCreate('新对话', data => {
        conversations.value.unshift(data)
        activeConversationId.value = data.id
        resolve()
      })
    })
  }
  if (!activeConversationId.value) return

  const currentFileContent = uploadedFileContent.value
  const currentFileName = uploadedFileName.value
  const currentImages = [...uploadedImages.value]
  const currentText = text

  const userMsg = {
    role: 'user',
    content: currentText || (currentImages.length > 0 ? '(图片)' : ''),
    messageType: currentImages.length > 0 ? 'image' : 'text',
    fileName: currentFileContent ? currentFileName : null,
    imageUrls: currentImages.length > 0 ? currentImages : null
  }
  messages.value.push(userMsg)
  inputText.value = ''
  uploadedImages.value = []

  const assistantMsg = { role: 'assistant', content: '' }
  messages.value.push(assistantMsg)
  isLoading.value = true
  scrollToBottom()

  const body = {
    text: currentText || '请分析这张图片',
    imageUrls: currentImages,
    enableWebSearch: enableWebSearch.value
  }
  if (currentFileContent) {
    body.fileContent = currentFileContent
    body.fileName = currentFileName
    uploadedFileContent.value = ''
    uploadedFileName.value = ''
  }

  apiChatWithConversation(
      activeConversationId.value,
      body,
      (chunk) => {
        assistantMsg.content += chunk
        scrollToBottom()
      },
      (error) => {
        assistantMsg.content = error || '生成失败，请重试。'
        isLoading.value = false
      },
      () => {
        isLoading.value = false
        loadConversations()
      },
      (toolData) => {
        // 显示工具调用提示
        const toolName = toolData.tool || ''
        const input = toolData.input?.keyword || toolData.input?.text || ''
        let label = ''
        if (toolName.includes('web_search') || toolName.includes('WebSearch')) {
          label = input ? `🔍 正在搜索「${input}」...` : '🔍 正在搜索互联网...'
        } else if (toolName.includes('search')) {
          label = input ? `📝 正在搜索论坛「${input}」...` : '📝 正在搜索论坛帖子...'
        } else if (toolName.includes('recognize_image')) {
          label = '🖼️ 正在识别图片内容...'
        } else if (toolName.includes('generate_image')) {
          const prompt = toolData.input?.prompt || ''
          label = prompt ? `🎨 正在生成图片「${prompt}」...` : '🎨 正在生成图片...'
        } else {
          label = `🛠️ 正在调用工具: ${toolName}`
        }
        messages.value.splice(messages.value.length - 1, 0, {
          role: 'tool',
          content: label
        })
        scrollToBottom()
      },
      (newTitle) => {
        // 收到自动生成的对话标题，更新侧边栏
        const conv = conversations.value.find(c => c.id === activeConversationId.value)
        if (conv) conv.title = newTitle
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
  height: 100%;
  min-height: calc(100vh - 105px);
  max-height: calc(100vh - 105px);
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
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color);
}

.messages-container {
  flex: 1;
  min-height: 0;
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

  &.tool-message {
    justify-content: center;
    margin-bottom: 4px;

    .message-bubble {
      background: transparent;
      padding: 4px 12px;
      max-width: 100%;

      .message-content {
        font-size: 12px;
        color: var(--el-text-color-secondary);
        text-align: center;
      }
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

    :deep(img) {
      max-width: 100%;
      max-height: 360px;
      border-radius: 8px;
      object-fit: contain;
    }

    .image-preview {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
      margin-bottom: 8px;
    }

    .chat-image {
      max-width: 200px;
      max-height: 200px;
      border-radius: 8px;
      cursor: pointer;
      object-fit: cover;
      border: 1px solid var(--el-border-color-light);
      transition: transform 0.2s;

      &:hover {
        transform: scale(1.05);
      }
    }



    .file-attach-badge {
      display: inline-block;
      font-size: 12px;
      background: var(--el-color-warning-light-9);
      color: var(--el-color-warning-dark-2);
      padding: 2px 10px;
      border-radius: 4px;
      margin-bottom: 8px;
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
    gap: 8px;
    margin-bottom: 8px;

    .toolbar-buttons {
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .upload-previews {
      display: flex;
      align-items: center;
      gap: 6px;
      flex-wrap: wrap;
    }

    .img-preview-item {
      position: relative;
      display: inline-flex;

      .img-thumb {
        width: 36px;
        height: 36px;
        border-radius: 4px;
        object-fit: cover;
        cursor: pointer;
        border: 1px solid var(--el-border-color-light);
      }

      .img-remove {
        position: absolute;
        top: -8px;
        right: -8px;
        width: 18px;
        height: 18px;
        background: var(--el-color-danger);
        color: #fff;
        border: none;
        --el-button-size: 18px;

        &:hover {
          background: var(--el-color-danger-light-3);
        }
      }
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
.image-preview-dialog .el-dialog__body { padding: 0; }
</style>
