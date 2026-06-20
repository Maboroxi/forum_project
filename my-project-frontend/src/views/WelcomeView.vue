<template>
    <!-- 移动端：简洁全屏登录 -->
    <div v-if="isMobile" class="mobile-welcome">
        <div class="mobile-welcome-header">
            <div class="mobile-welcome-title">北梨论坛</div>
            <div class="mobile-welcome-desc">与你分享校园生活的每一刻</div>
        </div>
        <div class="mobile-welcome-form">
            <router-view v-slot="{ Component }">
                <transition name="el-fade-in-linear" mode="out-in">
                    <component :is="Component"/>
                </transition>
            </router-view>
        </div>
    </div>
    <!-- 桌面端：双栏布局 -->
    <div v-else class="desktop-welcome" style="width: 100vw;height: 100vh;overflow: hidden;display: flex">
        <div style="flex: 1">
            <el-image style="width: 100%;height: 100%" fit="cover"
                      src="https://img0.baidu.com/it/u=3217812679,2585737758&fm=253&fmt=auto&app=138&f=JPEG?w=1422&h=800"/>
        </div>
        <div class="welcome-title">
            <div style="font-size: 30px;font-weight: bold">欢迎来到校园论坛</div>
            <div style="margin-top: 10px">在这里你可以与同学们密切交流。</div>
        </div>
        <div class="right-card">
            <router-view v-slot="{ Component }">
                <transition name="el-fade-in-linear" mode="out-in">
                    <component :is="Component" style="height: 100%"/>
                </transition>
            </router-view>
        </div>
    </div>
</template>

<script setup>
import { isMobile } from "@/utils/device";
</script>

<style scoped>
.desktop-welcome .right-card {
  width: 400px;
  z-index: 1;
  background-color: var(--el-bg-color);
}

.desktop-welcome .welcome-title {
    position: absolute;
    bottom: 30px;
    left: 30px;
    color: white;
    text-shadow: 0 0 10px black;
}

/* 移动端欢迎页 */
.mobile-welcome {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    background: linear-gradient(180deg, #1989fa 0%, #07c160 100%);
}

.mobile-welcome-header {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    color: #fff;
    padding: 60px 20px 40px;
}

.mobile-welcome-title {
    font-size: 32px;
    font-weight: bold;
    margin-bottom: 12px;
}

.mobile-welcome-desc {
    font-size: 15px;
    opacity: 0.9;
}

.mobile-welcome-form {
    background: #fff;
    border-radius: 16px 16px 0 0;
    padding: 24px 20px 40px;
}
</style>
