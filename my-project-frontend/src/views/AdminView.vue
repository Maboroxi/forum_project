<script setup>

import {
    Bell,
    ChatDotSquare,
    Location, Message,
    User
} from "@element-plus/icons-vue";
import UserInfo from "@/components/UserInfo.vue";
import {inject, onMounted, ref} from "vue";
import { isMobile } from "@/utils/device";
import logoSvg from "@/assets/logo.svg";
import router from "@/router";
import {useRoute} from "vue-router";

const adminMenu = [
    {
        title: '校园论坛管理', icon: Location, sub: [
            { title: '用户管理', icon: User, index: '/admin/user' },
            { title: '邮件管理', icon: Message, index: '/admin/email' },
            { title: '帖子广场管理', icon: ChatDotSquare, index: '/admin/forum' },
            { title: '公告管理', icon: Bell, index: '/admin/announcement' }
        ]
    }
]

const route = useRoute()
const loading = inject('userLoading')
const pageTabs = ref([])

function handleTabClick({ props }) {
    router.push(props.name)
}

function handleTabClose(name) {
    const index = pageTabs.value.findIndex(tab => tab.name === name)
    const isCurrent = name === route.fullPath
    pageTabs.value.splice(index, 1)
    if(pageTabs.value.length > 0) {
        //删除后，标签列表中还有剩余的Tab且关闭的是当前的，则自动进行切换，默认切换到上一个，如果没有上一个，则切换到下一个
        if(isCurrent) {
            router.push(pageTabs.value[Math.max(0, index - 1)].name)
        }
    } else {
        router.push('/admin')
    }
}

function addAdminTab(menu) {
    if(!menu.index) return
    if(pageTabs.value.findIndex(tab => tab.name === menu.index) < 0) {
        pageTabs.value.push({
            title: menu.title,
            name: menu.index
        })
    }
}

onMounted(() => {
    const initPage = adminMenu
        .flatMap(menu => menu.sub)
        .find(sub => sub.index === route.fullPath)
    if(initPage) {
        addAdminTab(initPage)
    }
})
</script>

<template>
    <!-- 移动端：简化管理视图 -->
    <div v-if="isMobile" class="mobile-admin">
        <van-nav-bar title="管理后台" left-text="返回" left-arrow @click-left="$router.push('/index')" fixed placeholder/>
        <div class="mobile-admin-content">
            <router-view v-slot="{ Component }">
                <keep-alive>
                    <component :is="Component"/>
                </keep-alive>
            </router-view>
        </div>
    </div>
    <!-- 桌面端：完整管理布局 -->
    <div v-else class="admin-content" v-loading="loading" element-loading-text="正在进入，请稍后...">
        <el-container style="height: 100%">
            <el-aside width="230px" class="admin-content-aside">
                <div class="logo-box">
                    <el-image class="logo" :src="logoSvg"/>
                    <span class="site-title">北梨论坛</span>
                </div>
                <el-scrollbar style="height: calc(100vh - 57px)">
                    <el-menu
                        router
                        :default-active="$route.path"
                        :default-openeds="['1']"
                        style="min-height: calc(100vh - 57px);border: none">
                        <el-sub-menu :index="(index + 1).toString()"
                                     v-for="(menu, index) in adminMenu">
                            <template #title>
                                <el-icon>
                                    <component :is="menu.icon"/>
                                </el-icon>
                                <span><b>{{ menu.title }}</b></span>
                            </template>
                            <el-menu-item :index="subMenu.index"
                                          @click="addAdminTab(subMenu)"
                                          v-for="subMenu in menu.sub">
                                <template #title>
                                    <el-icon>
                                        <component :is="subMenu.icon"/>
                                    </el-icon>
                                    {{ subMenu.title }}
                                </template>
                            </el-menu-item>
                        </el-sub-menu>
                    </el-menu>
                </el-scrollbar>
            </el-aside>
            <el-container>
                <el-header class="admin-content-header">
                    <div style="flex: 1">
                        <el-tabs type="card"
                                 :model-value="route.fullPath"
                                 closable
                                 @tab-remove="handleTabClose"
                                 @tab-click="handleTabClick">
                            <el-tab-pane v-for="tab in pageTabs"
                                         :label="tab.title"
                                         :name="tab.name"
                                         :key="tab.name"/>
                        </el-tabs>
                    </div>
                    <user-info/>
                </el-header>
                <el-main>
                    <router-view v-slot="{ Component }">
                        <keep-alive>
                            <component :is="Component" />
                        </keep-alive>
                    </router-view>
                </el-main>
            </el-container>
        </el-container>
    </div>
</template>

<style scoped>
.admin-content {
    height: 100vh;
    width: 100vw;

    .admin-content-aside {
        border-right: solid 1px var(--el-border-color);

        .logo-box {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            padding: 15px 0 10px;
            height: 32px;

            .logo {
                height: 32px;
            }

            .site-title {
                font-size: 18px;
                font-weight: 700;
                color: var(--el-text-color-primary);
                white-space: nowrap;
            }
        }
    }

    .admin-content-header {
        border-bottom: solid 1px var(--el-border-color);
        height: 55px;
        display: flex;
        align-items: center;
        box-sizing: border-box;

        :deep(.el-tabs__header) {
            height: 32px;
            margin-bottom: 0;
            border-bottom: none;
        }

        :deep(.el-tabs__nav) {
            gap: 10px;
            border: none;
        }

        :deep(.el-tabs__item) {
            height: 32px;
            padding: 0 15px;
            border-radius: 6px;
            border: solid 1px var(--el-border-color);
        }
    }
}
</style>
