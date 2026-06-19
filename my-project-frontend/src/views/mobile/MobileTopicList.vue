<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from '@/store'
import { apiForumTopicList, apiForumTopTopics } from '@/net/api/forum'
import { showToast } from 'vant'

const router = useRouter()
const store = useStore()

const state = reactive({
  list: [],
  page: 0,
  type: 0,
  end: false,
  top: [],
  loading: false,
  refreshing: false
})

const types = ref([])

// 加载帖子列表
function loadList(append = false) {
  if (state.end || state.loading) return
  state.loading = true

  apiForumTopicList(state.page, state.type, data => {
    if (data && data.length) {
      if (append) {
        state.list.push(...data)
      } else {
        state.list = data
      }
      state.page++
    }
    if (!data || data.length < 10) {
      state.end = true
    }
    state.loading = false
    state.refreshing = false
  })
}

// 下拉刷新
function onRefresh() {
  state.refreshing = true
  state.page = 0
  state.end = false
  state.loading = false
  loadList(false)
}

// 切换分类
function switchType(typeId) {
  state.type = typeId
  state.page = 0
  state.end = false
  state.loading = false
  state.list = []
  loadList(false)
}

function toDetail(id) {
  router.push(`/index/topic-detail/${id}`)
}

function formatTime(time) {
  const d = new Date(time)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 172800000) return '昨天 ' + d.toLocaleTimeString().slice(0, 5)
  return d.toLocaleDateString()
}

function getTypeName(typeId) {
  return store.findTypeById(typeId)?.name || '综合'
}

function getTypeColor(typeId) {
  return store.findTypeById(typeId)?.color || '#999'
}

onMounted(() => {
  // 加载置顶
  apiForumTopTopics(data => { state.top = data || [] })
  // 加载类型（store 中已有"全部"选项，直接使用）
  types.value = store.forum.types || []
  // 加载列表
  loadList(false)
})
</script>

<template>
  <div class="mobile-topic-list">
    <!-- 置顶帖子 -->
    <div v-if="state.top.length" class="sticky-topics">
      <div class="sticky-item" v-for="item in state.top" :key="'top-' + item.id"
           @click="toDetail(item.id)">
        <van-tag type="info" size="small">置顶</van-tag>
        <span class="sticky-title">{{ item.title }}</span>
      </div>
    </div>

    <!-- 分类标签（可横向滚动） -->
    <div class="type-tabs">
      <div
        v-for="t in types"
        :key="t.id"
        :class="['type-chip', { active: state.type === t.id }]"
        @click="switchType(t.id)"
      >
        {{ t.name }}
      </div>
    </div>

    <!-- 帖子列表（下拉刷新 + 无限滚动） -->
    <van-pull-refresh v-model="state.refreshing" @refresh="onRefresh" :head-height="60">
      <van-list
        v-model:loading="state.loading"
        :finished="state.end"
        finished-text="— 没有更多了 —"
        @load="loadList(true)"
        :immediate-check="false"
      >
        <div
          v-for="item in state.list"
          :key="item.id"
          class="topic-card"
          @click="toDetail(item.id)"
        >
          <div class="card-header">
            <van-image
              round
              width="36"
              height="36"
              :src="store.avatarUserUrl(item.avatar)"
              @click.stop
            />
            <div class="card-user">
              <div class="card-username">{{ item.username }}</div>
              <div class="card-time">{{ formatTime(item.time) }}</div>
            </div>
            <van-tag :color="getTypeColor(item.type)" size="small" class="type-tag">
              {{ getTypeName(item.type) }}
            </van-tag>
          </div>
          <div class="card-title">
            <van-tag v-if="item.locked" type="danger" size="small" style="margin-right: 4px">已锁定</van-tag>
            {{ item.title }}
          </div>
          <div class="card-excerpt">{{ item.text }}</div>
          <!-- 图片网格 -->
          <div v-if="item.images && item.images.length" class="card-images" :class="'img-count-' + Math.min(item.images.length, 3)">
            <van-image
              v-for="(img, i) in item.images.slice(0, 3)"
              :key="i"
              :src="img"
              fit="cover"
              @click.stop
            />
          </div>
          <!-- 底部统计 -->
          <div class="card-footer">
            <span><van-icon name="good-job-o" /> {{ item.like || 0 }}</span>
            <span><van-icon name="star-o" /> {{ item.collect || 0 }}</span>
          </div>
        </div>
        <van-empty v-if="!state.list.length && !state.loading && state.end" description="暂无帖子" />
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<style scoped>
.mobile-topic-list {
  padding-bottom: 60px;
}

/* 置顶区域 */
.sticky-topics {
  background: #fff;
  padding: 8px 12px;
  margin-bottom: 6px;
}

.sticky-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 0;
  font-size: 13px;
  overflow: hidden;
}

.sticky-title {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #333;
}

/* 分类标签 */
.type-tabs {
  display: flex;
  gap: 8px;
  padding: 10px 12px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  background: #fff;
  margin-bottom: 6px;
}

.type-tabs::-webkit-scrollbar {
  display: none;
}

.type-chip {
  flex-shrink: 0;
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 13px;
  background: #f5f5f5;
  color: #666;
  transition: all .2s;
}

.type-chip.active {
  background: #1989fa;
  color: #fff;
  font-weight: bold;
}

/* 帖子卡片 */
.topic-card {
  background: #fff;
  padding: 14px 12px;
  margin-bottom: 6px;
  transition: background .2s;
}

.topic-card:active {
  background: #f5f5f5;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.card-user {
  flex: 1;
  min-width: 0;
}

.card-username {
  font-size: 14px;
  font-weight: 600;
}

.card-time {
  font-size: 11px;
  color: #999;
  margin-top: 1px;
}

.type-tag {
  flex-shrink: 0;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.4;
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.card-excerpt {
  font-size: 13px;
  color: #888;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  margin-bottom: 8px;
}

/* 图片网格 */
.card-images {
  display: grid;
  gap: 4px;
  margin-bottom: 8px;
  border-radius: 6px;
  overflow: hidden;
}

.card-images.img-count-1 {
  grid-template-columns: 1fr;
  max-width: 50%;
}

.card-images.img-count-2 {
  grid-template-columns: 1fr 1fr;
}

.card-images.img-count-3 {
  grid-template-columns: 1fr 1fr 1fr;
}

.card-images :deep(.van-image) {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 4px;
  overflow: hidden;
}

/* 底部 */
.card-footer {
  display: flex;
  gap: 20px;
  font-size: 12px;
  color: #999;
}

.card-footer span {
  display: flex;
  align-items: center;
  gap: 3px;
}
</style>
