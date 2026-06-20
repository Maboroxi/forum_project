<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { QuillDeltaToHtmlConverter } from 'quill-delta-to-html'
import { useStore } from '@/store'
import {
  apiForumTopic,
  apiForumComments,
  apiForumInteract,
  apiForumCommentDelete,
} from '@/net/api/forum'
import { showToast, showConfirmDialog } from 'vant'

const route = useRoute()
const router = useRouter()
const store = useStore()

const tid = computed(() => route.params.tid)
const loading = ref(true)
const showCommentEditor = ref(false)
const commentQuote = ref(null)

const topic = reactive({
  data: null,
  like: false,
  collect: false,
  comments: null,
  page: 1
})

function init() {
  loading.value = true
  apiForumTopic(tid.value, data => {
    topic.data = data
    topic.like = data.interact.like
    topic.collect = data.interact.collect
    loading.value = false
    loadComments(1)
  })
}

// 导航到不同帖子时重新加载，但忽略导航离开时的 undefined
watch(tid, (newTid) => {
  if (!newTid) return
  topic.data = null
  topic.comments = null
  init()
})

function convertToHtml(content) {
  try {
    const ops = JSON.parse(content).ops
    return new QuillDeltaToHtmlConverter(ops, { inlineStyles: true }).convert()
  } catch {
    return content || ''
  }
}

function interact(type, message) {
  apiForumInteract(tid.value, type, topic, message)
}

function loadComments(page) {
  topic.comments = null
  topic.page = page
  apiForumComments(tid.value, page - 1, data => topic.comments = data)
}

function onCommentAdd() {
  showCommentEditor.value = false
  commentQuote.value = null
  loadComments(Math.floor(++topic.data.comments / 10) + 1)
}

function deleteComment(id) {
  showConfirmDialog({ title: '提示', message: '确定删除这条评论吗？' }).then(() => {
    apiForumCommentDelete(id, () => {
      showToast('已删除')
      loadComments(topic.page)
    })
  }).catch(() => {})
}

function openComment(item = null) {
  commentQuote.value = item
  showCommentEditor.value = true
}

function openEditor() {
  showToast('编辑功能请在电脑端使用')
}

onMounted(init)
</script>

<template>
  <!-- 单根节点包裹，支持 Transition 动画 -->
  <div class="mobile-topic-detail-wrapper">
    <!-- 加载骨架 -->
    <div class="mobile-loading" v-if="loading">
      <van-skeleton title :row="6" />
    </div>
    <!-- 内容 -->
    <template v-else-if="topic.data">
      <div class="detail-header">
        <div class="header-user">
          <van-image round width="40" height="40" :src="store.avatarUserUrl(topic.data.user.avatar)" />
          <div class="header-user-info">
            <div class="header-username">{{ topic.data.user.username }}</div>
            <div class="header-time">{{ new Date(topic.data.time).toLocaleString() }}</div>
          </div>
          <van-tag v-if="topic.data.locked" type="danger" size="small">已锁定</van-tag>
        </div>
        <div class="header-title">
          <van-tag :color="store.findTypeById(topic.data.type)?.color" size="small" style="margin-right: 6px">
            {{ store.findTypeById(topic.data.type)?.name || '综合' }}
          </van-tag>
          {{ topic.data.title }}
        </div>
      </div>

      <div class="detail-content" v-html="convertToHtml(topic.data.content)"></div>

      <div class="detail-stats">
        <span>发帖时间: {{ new Date(topic.data.time).toLocaleString() }}</span>
      </div>

      <div class="detail-comments" v-if="topic.comments">
        <div class="comments-title">评论 ({{ topic.data.comments || 0 }})</div>
        <div v-for="item in topic.comments" :key="item.id" class="comment-item">
          <van-image round width="32" height="32" :src="store.avatarUserUrl(item.user.avatar)" />
          <div class="comment-body">
            <div class="comment-username">{{ item.user.username }}</div>
            <div v-if="item.quote" class="comment-quote">回复: {{ item.quote }}</div>
            <div class="comment-text" v-html="convertToHtml(item.content)"></div>
            <div class="comment-footer">
              <span class="comment-time">{{ new Date(item.time).toLocaleString() }}</span>
              <div class="comment-actions">
                <van-icon name="chat-o" @click="openComment(item)" />
                <van-icon v-if="item.user.id === store.user.id" name="delete-o" @click="deleteComment(item.id)" />
              </div>
            </div>
          </div>
        </div>
        <div class="comments-pagination" v-if="topic.data.comments > 10">
          <van-pagination v-model="topic.page" :total-items="topic.data.comments" :items-per-page="10"
                          @change="loadComments" mode="simple" />
        </div>
      </div>

      <div class="detail-action-bar">
        <div class="action-btn" :class="{ active: topic.like }" @click="interact('like', '点赞')">
          <van-icon :name="topic.like ? 'good-job' : 'good-job-o'" />
          <span>{{ topic.data.interact.likeCount }}</span>
        </div>
        <div class="action-btn" @click="openComment()">
          <van-icon name="chat-o" />
          <span>{{ topic.data.comments }}</span>
        </div>
        <div class="action-btn" :class="{ active: topic.collect }" @click="interact('collect', '收藏')">
          <van-icon :name="topic.collect ? 'star' : 'star-o'" />
          <span>{{ topic.data.interact.collectCount }}</span>
        </div>
        <div class="action-btn" @click="openEditor()">
          <van-icon name="edit" />
        </div>
      </div>

      <topic-comment-editor :show="showCommentEditor" @close="showCommentEditor = false"
                            :tid="tid" :quote="commentQuote" @comment="onCommentAdd" />
    </template>
  </div>
</template>

<style scoped>
.mobile-topic-detail-wrapper {
  min-height: 100%;
}

.mobile-loading {
  padding: 20px 16px;
  background: #fff;
}

.detail-header {
  background: #fff;
  padding: 14px 12px;
  margin-bottom: 6px;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.header-user-info {
  flex: 1;
}

.header-username {
  font-size: 15px;
  font-weight: 600;
}

.header-time {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}

.header-title {
  font-size: 17px;
  font-weight: 700;
  line-height: 1.4;
  color: #1a1a1a;
}

.detail-content {
  background: #fff;
  padding: 14px 12px;
  margin-bottom: 6px;
  font-size: 15px;
  line-height: 1.7;
  color: #333;
  overflow-wrap: anywhere;
}

.detail-content :deep(img) {
  display: block;
  max-width: 100%;
  height: auto;
  border-radius: 6px;
  margin: 8px 0;
}

.detail-stats {
  background: #fff;
  padding: 8px 12px;
  margin-bottom: 6px;
  font-size: 12px;
  color: #999;
}

.detail-comments {
  background: #fff;
  padding: 12px;
  margin-bottom: 6px;
}

.comments-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.comment-item {
  display: flex;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-username {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 2px;
}

.comment-quote {
  font-size: 12px;
  color: #999;
  background: #f5f5f5;
  padding: 6px 8px;
  border-radius: 4px;
  margin: 4px 0;
}

.comment-text {
  font-size: 14px;
  line-height: 1.5;
}

.comment-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
}

.comment-time {
  font-size: 11px;
  color: #999;
}

.comment-actions {
  display: flex;
  gap: 16px;
  font-size: 16px;
  color: #999;
}

.detail-action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  padding: 8px 0 env(safe-area-inset-bottom, 0);
  z-index: 100;
}

.action-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  font-size: 14px;
  color: #666;
  padding: 8px 0;
  min-height: 44px;
  justify-content: center;
}

.action-btn:active {
  opacity: 0.5;
  background: #f5f5f5;
}

.action-btn .van-icon {
  font-size: 22px;
}

.action-btn.active {
  color: #1989fa;
}
</style>
