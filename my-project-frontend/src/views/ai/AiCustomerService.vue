<template>
  <div class="customer-service-container">
    <div v-if="loading" class="cs-loading">
      <el-icon class="loading-icon" :size="32"><Loading /></el-icon>
      <span>正在加载智能客服...</span>
    </div>
    <iframe :src="embedUrl" class="cs-iframe" :class="{ hidden: loading }"
            allow="microphone *" allowfullscreen @load="onLoad" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Loading } from '@element-plus/icons-vue'

const embedUrl = 'https://udify.app/chatbot/WTGhz6Ba8remaf6l'
const loading = ref(true)

function onLoad() {
  loading.value = false
}
</script>

<style scoped lang="less">
.customer-service-container {
  position: relative;
  height: 100%;
  min-height: calc(100vh - 105px);
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-bg-color);

  .cs-loading {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 16px;
    color: var(--el-text-color-secondary);
    font-size: 15px;
    z-index: 1;
    background: var(--el-bg-color);

    .loading-icon {
      animation: cs-spin 1.5s linear infinite;
    }
  }

  .cs-iframe {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    border: none;
    z-index: 2;

    &.hidden {
      visibility: hidden;
    }
  }
}

@keyframes cs-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
