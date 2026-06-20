<script setup>
import { useDark } from '@vueuse/core'
import { onMounted, provide, ref } from "vue";
import { isUnauthorized } from "@/net";
import { apiUserInfo } from "@/net/api/user";
import zhCn from "element-plus/es/locale/lang/zh-cn";
import { isMobile } from "@/utils/device";
import MobileLayout from "@/layouts/MobileLayout.vue";

useDark({
  selector: 'html',
  attribute: 'class',
  valueDark: 'dark',
  valueLight: 'light'
})

const loading = ref(false)
provide('userLoading', loading)

onMounted(() => {
    if(!isUnauthorized()) {
        apiUserInfo(loading)
    }
})
</script>

<template>
  <el-config-provider :locale="zhCn">
      <div class="wrapper">
        <!-- 移动端 -->
        <MobileLayout v-if="isMobile">
          <router-view/>
        </MobileLayout>
        <!-- 桌面端 -->
        <router-view v-else/>
      </div>
  </el-config-provider>
</template>

<style scoped>
.wrapper {
  line-height: 1.5;
}
</style>
