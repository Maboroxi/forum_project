<script setup>
import { computed, inject, onMounted, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { isUnauthorized } from '@/net'
import { apiNotificationList, apiNotificationDelete, apiNotificationDeleteAll } from '@/net/api/user'
import { showConfirmDialog, showToast } from 'vant'
import TopicEditor from '@/components/TopicEditor.vue'

const router = useRouter()
const route = useRoute()
const loading = inject('userLoading')

const showEditor = ref(false)
const showNotification = ref(false)
const notification = ref([])

const tabs = [
  { name: '首页', icon: 'home-o', route: '/index' },
  { name: '公告', icon: 'info-o', route: '/index/announcements' },
  { name: '发帖', icon: 'plus', route: '', isAction: true },
  { name: 'AI', icon: 'chat-o', route: '/index/ai-agent' },
  { name: '我的', icon: 'user-o', route: '/index/user-setting' },
]

// 当前激活的 tab 索引 — 用 watch 同步路由变化
const activeTab = ref(0)

watch(() => route.path, (path) => {
  // 精确匹配：长的优先，避免 `/index` 抢先匹配 `/index/xxx`
  let matched = -1
  for (let i = 0; i < tabs.length; i++) {
    const t = tabs[i]
    if (t.route && t.route !== '/index' && path.startsWith(t.route)) {
      matched = i
    }
  }
  activeTab.value = matched >= 0 ? matched : 0
}, { immediate: true })

function onTopicCreated() {
  showEditor.value = false
  // 发帖成功后刷新列表：强制重新加载页面
  router.push('/index')
}

function onTabChange(index) {
  const tab = tabs[index]
  if (tab.isAction) {
    showEditor.value = true
    activeTab.value = 0
    return
  }
  if (tab.route) router.push(tab.route)
}

// 是否为首页相关路由
const isIndexRoute = computed(() => {
  return route.path === '/index' || route.path.startsWith('/index/')
})

// 是否显示导航栏（所有内容页都显示）
const showNavbar = computed(() => isIndexRoute.value || route.path.startsWith('/admin'))

// 是否显示底部 TabBar（只在列表/首页显示，详情页隐藏）
const showTabbar = computed(() => {
  if (!isIndexRoute.value) return false
  return !route.path.includes('/topic-detail/')
})

const loadNotification = () => {
  apiNotificationList(data => {
    notification.value = data
  })
}

onMounted(() => {
  if (!isUnauthorized()) {
    loadNotification()
  }
})

function onNotification(item) {
  apiNotificationDelete(item.id, () => {
    loadNotification()
    if (item.url) {
      const match = item.url.match(/\/index\/(topic-detail\/\d+)/)
      if (match) {
        showNotification.value = false
        router.push(`/index/${match[1]}`)
      } else {
        window.open(item.url)
      }
    }
  })
}

function clearAllNotifications() {
  showConfirmDialog({ title: '提示', message: '确定清除全部未读消息吗？' })
    .then(() => {
      apiNotificationDeleteAll(loadNotification)
      showToast('已清除')
    })
    .catch(() => {})
}
</script>

<template>
  <div class="mobile-layout">
    <!-- 顶部导航栏 -->
    <van-nav-bar
      v-if="showNavbar"
      :title="route.meta?.title || '北梨论坛'"
      :safe-area-inset-top="true"
      :left-arrow="route.path.split('/').filter(Boolean).length > 2"
      @click-left="router.back()"
      fixed
      placeholder
    >
      <template #right>
        <van-icon name="bell" size="20" @click="showNotification = true"
                  :badge="notification.length || undefined"/>
      </template>
    </van-nav-bar>

    <!-- 内容区 -->
    <div class="mobile-content">
      <router-view />
    </div>

    <!-- 底部 TabBar（只在列表/首页显示） -->
    <van-tabbar v-if="showTabbar" v-model="activeTab" @change="onTabChange" placeholder>
      <van-tabbar-item
        v-for="tab in tabs"
        :key="tab.name"
        :icon="tab.isAction ? undefined : tab.icon"
      >
        <template #icon v-if="tab.isAction">
          <div class="mobile-fab-btn">
            <van-icon name="plus" size="22" color="#fff"/>
          </div>
        </template>
        <span v-if="!tab.isAction">{{ tab.name }}</span>
      </van-tabbar-item>
    </van-tabbar>

    <!-- 帖子编辑器 -->
    <topic-editor :show="showEditor" @close="showEditor = false" @success="onTopicCreated"/>

    <!-- 通知面板 -->

    <!-- 通知面板 -->
    <van-action-sheet v-model:show="showNotification" title="消息通知">
      <div class="notif-body">
        <van-empty v-if="!notification.length" description="暂无未读消息" />
        <div v-else class="notif-list">
          <div class="notif-item" v-for="item in notification" :key="item.id"
               @click="onNotification(item)">
            <div class="notif-header">
              <van-tag plain type="primary" size="small">{{ item.title }}</van-tag>
            </div>
            <div class="notif-desc">{{ item.content }}</div>
          </div>
        </div>
        <div class="notif-footer" v-if="notification.length">
          <van-button block size="small" plain type="primary" @click="clearAllNotifications">
            清除全部
          </van-button>
        </div>
      </div>
    </van-action-sheet>
  </div>
</template>

<style scoped>
.mobile-layout {
  min-height: 100vh;
  background: #f7f8fa;
}

.mobile-content {
  min-height: calc(100vh - 100px);
}

.mobile-fab-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1989fa, #07c160);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
  box-shadow: 0 4px 12px rgba(25, 137, 250, 0.4);
}

.notif-body {
  padding: 12px 16px;
  max-height: 50vh;
  overflow-y: auto;
}

.notif-item {
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.notif-header {
  margin-bottom: 4px;
}

.notif-desc {
  font-size: 13px;
  color: #666;
  line-height: 1.4;
  margin-top: 4px;
}

.notif-footer {
  padding-top: 12px;
}
</style>
