<script setup>
import {isMobile} from "@/utils/device";
</script>

<template>
    <div>
        <router-view v-slot="{ Component }">
            <keep-alive :include="['TopicList']">
                <component :is="Component"/>
            </keep-alive>
        </router-view>
        <!-- 桌面端回到顶部 -->
        <el-backtop v-if="!isMobile" target=".main-content-page .el-scrollbar__wrap" :right="20" :bottom="70"/>
    </div>
</template>
