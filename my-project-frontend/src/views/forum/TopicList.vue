<script setup>
import LightCard from "@/components/LightCard.vue";
import {
    Calendar,
    Clock,
    CollectionTag,
    Compass,
    Delete,
    Document,
    Edit,
    EditPen,
    Files,
    Link,
    Picture,
    Microphone, CircleCheck, Star, FolderOpened, ArrowRightBold, Lock
} from "@element-plus/icons-vue";
import Weather from "@/components/Weather.vue";
import {computed, onMounted, reactive, ref, watch} from "vue";
import {ElMessage, ElMessageBox} from "element-plus";
import TopicEditor from "@/components/TopicEditor.vue";
import {useStore} from "@/store";
import ColorDot from "@/components/ColorDot.vue";
import router from "@/router";
import TopicTag from "@/components/TopicTag.vue";
import TopicCollectList from "@/components/TopicCollectList.vue";
import {apiForumTopicList, apiForumTopTopics, apiForumWeather} from "@/net/api/forum";
import {apiAnnouncementLatest} from "@/net/api/announcement";
import {deleteTopicDraft, listTopicDrafts, topicDraftSummary} from "@/utils/topicDraft";

const store = useStore()

const weather = reactive({
    location: {},
    now: {},
    hourly: [],
    success: false
})
const editor = ref(false)
const topics = reactive({
    list: [],
    type: 0,
    page: 0,
    end: false,
    top: []
})
const announcements = ref([])
const collects = ref(false)
const drafts = reactive({
    show: false,
    list: []
})
const draftEditor = reactive({
    id: null,
    title: '',
    type: null,
    content: ''
})

watch(() => topics.type, () => resetList(), {immediate: true})

const today = computed(() => {
    const date = new Date()
    return `${date.getFullYear()} 年 ${date.getMonth() + 1} 月 ${date.getDate()} 日`
})

function updateList(){
    if(topics.end) return
    apiForumTopicList(topics.page, topics.type, data => {
        if(data) {
            data.forEach(d => topics.list.push(d))
            topics.page++
        }
        if(!data || data.length < 10)
            topics.end = true
    })
}

function onTopicCreate() {
    editor.value = false
    clearDraftEditor()
    reloadDrafts()
    resetList()
}

function resetList() {
    topics.page = 0
    topics.end = false
    topics.list = []
    updateList()
}

function clearDraftEditor() {
    draftEditor.id = null
    draftEditor.title = ''
    draftEditor.type = null
    draftEditor.content = ''
}

function openTopicEditor() {
    clearDraftEditor()
    editor.value = true
}

function reloadDrafts() {
    drafts.list = listTopicDrafts(store.user.id)
}

function openDraft(draft) {
    draftEditor.id = draft.id
    draftEditor.title = draft.title || ''
    draftEditor.type = draft.type || null
    draftEditor.content = draft.content ? JSON.stringify(draft.content) : ''
    drafts.show = false
    editor.value = true
}

function onDraftSave(draft) {
    draftEditor.id = draft.id
    draftEditor.title = draft.title
    draftEditor.type = draft.type
    draftEditor.content = draft.content ? JSON.stringify(draft.content) : ''
    reloadDrafts()
}

function removeDraft(draft) {
    ElMessageBox.confirm('删除后无法恢复，确定删除这条草稿吗？', '删除草稿', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        deleteTopicDraft(store.user.id, draft.id)
        reloadDrafts()
        ElMessage.success('草稿已删除')
    }).catch(() => {})
}

function formatDraftTime(time) {
    return new Date(time).toLocaleString()
}

navigator.geolocation.getCurrentPosition(position => {
    const longitude = position.coords.longitude
    const latitude = position.coords.latitude
    apiForumWeather(longitude, latitude, data => {
        Object.assign(weather, data)
        weather.success = true
    })
}, error => {
    console.info(error)
    ElMessage.warning('位置信息获取超时，请检测网络设置')
    apiForumWeather(116.40529, 39.90499, data => {
        Object.assign(weather, data)
        weather.success = true
    })
}, {
    timeout: 3000,
    enableHighAccuracy: true
})

onMounted(() => {
    apiForumTopTopics(data => topics.top = data)
    apiAnnouncementLatest(3, data => announcements.value = data)
    reloadDrafts()
})
</script>

<template>
    <div style="display: flex;margin: 20px auto;gap: 20px;max-width: 900px;padding: 0 20px">
        <div style="flex: 1">
            <light-card>
                <div class="create-topic" @click="openTopicEditor">
                    <el-icon><EditPen/></el-icon> 点击发表主题...
                </div>
                <div style="margin-top: 10px;display: flex;justify-content: space-between;align-items: center">
                    <div style="display: flex;gap: 13px;font-size: 18px;color: grey">
                        <el-icon><Edit /></el-icon>
                        <el-icon><Document /></el-icon>
                        <el-icon><Compass /></el-icon>
                        <el-icon><Picture /></el-icon>
                        <el-icon><Microphone /></el-icon>
                    </div>
                    <el-button :icon="Files" size="small" @click="drafts.show = true;reloadDrafts()" plain>
                        草稿箱
                        <span v-if="drafts.list.length">({{drafts.list.length}})</span>
                    </el-button>
                </div>
            </light-card>
            <light-card style="margin-top: 10px;display: flex;flex-direction: column;gap: 10px">
                <div v-for="item in topics.top" class="top-topic" @click="router.push(`/index/topic-detail/${item.id}`)">
                    <el-tag type="info" size="small">置顶</el-tag>
                    <div>{{item.title}}</div>
                    <div>{{new Date(item.time).toLocaleDateString()}}</div>
                </div>
            </light-card>
            <light-card style="margin-top: 10px;display: flex;gap: 7px">
                <div :class="`type-select-card ${topics.type === item.id ? 'active' : ''}`"
                     v-for="item in store.forum.types"
                     @click="topics.type = item.id">
                    <color-dot :color="item.color"/>
                    <span style="margin-left: 5px">{{item.name}}</span>
                </div>
            </light-card>
            <transition name="el-fade-in" mode="out-in">
                <div v-if="topics.list.length">
                    <div style="margin-top: 10px;display: flex;flex-direction: column;gap: 10px"
                         v-infinite-scroll="updateList">
                        <light-card v-for="item in topics.list" class="topic-card"
                                    @click="router.push('/index/topic-detail/'+item.id)">
                            <div style="display: flex">
                                <div>
                                    <el-avatar :size="30" :src="store.avatarUserUrl(item.avatar)"/>
                                </div>
                                <div style="margin-left: 7px;transform: translateY(-2px)">
                                    <div style="font-size: 13px;font-weight: bold">{{item.username}}</div>
                                    <div style="font-size: 12px;color: grey">
                                        <el-icon><Clock/></el-icon>
                                        <div style="margin-left: 2px;display: inline-block;transform: translateY(-2px)">
                                            {{new Date(item.time).toLocaleString()}}
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div style="margin-top: 5px">
                                <el-tag size="small" effect="dark" type="warning" disable-transitions
                                        style="margin-right: 10px;" v-if="item.locked">
                                    <el-icon>
                                        <Lock/>
                                    </el-icon>
                                    已锁定
                                </el-tag>
                                <topic-tag :type="item.type"/>
                                <span style="font-weight: bold;margin-left: 7px">{{item.title}}</span>
                            </div>
                            <div class="topic-content">{{item.text}}</div>
                            <div style="display: grid;grid-template-columns: repeat(3, 1fr);grid-gap: 10px">
                                <el-image class="topic-image" v-for="img in item.images" :src="img" fit="cover"></el-image>
                            </div>
                            <div style="display: flex;gap: 20px;font-size: 13px;margin-top: 10px;opacity: 0.8">
                                <div>
                                    <el-icon style="vertical-align: middle"><CircleCheck/></el-icon> {{item.like}}点赞
                                </div>
                                <div>
                                    <el-icon style="vertical-align: middle"><Star/></el-icon> {{item.collect}}收藏
                                </div>
                            </div>
                        </light-card>
                    </div>
                </div>
            </transition>
        </div>
        <div style="width: 280px">
            <div style="position: sticky;top: 20px">
                <light-card>
                    <div class="collect-list-button" @click="collects = true">
                        <span><el-icon><FolderOpened /></el-icon> 查看我的收藏</span>
                        <el-icon style="transform: translateY(3px)"><ArrowRightBold/></el-icon>
                    </div>
                </light-card>
                <light-card style="margin-top: 10px">
                    <div style="font-weight: bold">
                        <el-icon><CollectionTag/></el-icon>
                        校园公告
                    </div>
                    <el-divider style="margin: 10px 0"/>
                    <el-empty v-if="!announcements.length" :image-size="60" description="暂无公告"/>
                    <div v-else class="announcement-preview"
                         v-for="item in announcements"
                         @click="router.push(`/index/announcement/${item.id}`)">
                        <div>
                            <el-tag v-if="item.top" size="small" type="danger">置顶</el-tag>
                            <span class="announcement-title">{{ item.title }}</span>
                        </div>
                        <div class="announcement-summary">{{ item.summary || '暂无摘要' }}</div>
                        <div class="announcement-time">{{ new Date(item.publishTime || item.createTime).toLocaleDateString() }}</div>
                    </div>
                </light-card>
                <light-card style="margin-top: 10px">
                    <div style="font-weight: bold">
                        <el-icon><Calendar/></el-icon>
                        天气信息
                    </div>
                    <el-divider style="margin: 10px 0"/>
                    <weather :data="weather"/>
                </light-card>
                <light-card style="margin-top: 10px">
                    <div class="info-text">
                        <div>当前日期</div>
                        <div>{{today}}</div>
                    </div>
                    <div class="info-text">
                        <div>当期IP地址</div>
                        <div>127.0.0.1</div>
                    </div>
                </light-card>
                <div style="font-size: 14px;margin-top: 10px;color: grey">
                    <el-icon><Link/></el-icon>
                    友情链接
                    <el-divider style="margin: 10px 0"/>
                </div>
                <div style="display: grid;grid-template-columns: repeat(2, 1fr);grid-gap: 10px;margin-top: 10px">
                    <div class="friend-link">
                        <el-image style="height: 100%" src="https://element-plus.org/images/js-design-banner.jpg"/>
                    </div>
                    <div class="friend-link">
                        <el-image style="height: 100%" src="https://element-plus.org/images/vform-banner.png"/>
                    </div>
                </div>
            </div>
        </div>
        <topic-editor :show="editor" :draft-id="draftEditor.id"
                      :default-title="draftEditor.title"
                      :default-type="draftEditor.type"
                      :default-text="draftEditor.content"
                      @success="onTopicCreate"
                      @draft-save="onDraftSave"
                      @close="editor = false"/>
        <topic-collect-list :show="collects" @close="collects = false"/>
        <el-drawer v-model="drafts.show" title="帖子草稿箱" direction="rtl" size="420px">
            <el-empty v-if="!drafts.list.length" description="暂无草稿"/>
            <div v-else class="draft-list">
                <div class="draft-item" v-for="item in drafts.list" :key="item.id">
                    <div class="draft-item-header">
                        <div class="draft-title">{{item.title || '未命名草稿'}}</div>
                        <el-tag v-if="item.type" size="small" type="info">
                            {{store.findTypeById(item.type)?.name || '未知分类'}}
                        </el-tag>
                    </div>
                    <div class="draft-summary">{{topicDraftSummary(item.content) || '暂无正文内容'}}</div>
                    <div class="draft-footer">
                        <span>{{formatDraftTime(item.updateTime)}}</span>
                        <div>
                            <el-button :icon="EditPen" size="small" type="primary" @click="openDraft(item)" plain>
                                继续编辑
                            </el-button>
                            <el-button :icon="Delete" size="small" type="danger" @click="removeDraft(item)" plain>
                                删除
                            </el-button>
                        </div>
                    </div>
                </div>
            </div>
        </el-drawer>
    </div>
</template>

<style lang="less" scoped>
.collect-list-button {
    font-size: 14px;
    display: flex;
    justify-content: space-between;
    transition: .3s;

    &:hover {
        cursor: pointer;
        opacity: 0.6;
    }
}

.top-topic {
    display: flex;

    div:first-of-type {
        font-size: 14px;
        margin-left: 10px;
        font-weight: bold;
        opacity: 0.8;
        transition: color .3s;

        &:hover {
            color: grey;
        }
    }

    div:nth-of-type(2) {
        flex: 1;
        color: grey;
        font-size: 13px;
        text-align: right;
    }

    &:hover {
        cursor: pointer;
    }
}

.announcement-preview {
    border-bottom: solid 1px var(--el-border-color);
    padding: 8px 0;
    transition: .3s;

    &:last-child {
        border-bottom: none;
    }

    &:hover {
        cursor: pointer;
        opacity: 0.7;
    }

    .announcement-title {
        font-size: 14px;
        font-weight: bold;
        margin-left: 6px;
    }

    .announcement-summary {
        color: grey;
        display: -webkit-box;
        font-size: 13px;
        line-height: 1.5;
        margin-top: 5px;
        overflow: hidden;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 2;
    }

    .announcement-time {
        color: grey;
        font-size: 12px;
        margin-top: 4px;
        text-align: right;
    }
}

.type-select-card {
    background-color: #f5f5f5;
    padding: 2px 7px;
    font-size: 14px;
    border-radius: 3px;
    box-sizing: border-box;
    transition: background-color .3s;

    &.active {
        border: solid 1px #ead4c4;
    }

    &:hover {
        cursor: pointer;
        background-color: #dadada;
    }
}

.topic-card {
    padding: 15px;
    transition: scale .3s;

    &:hover {
        scale: 1.015;
        cursor: pointer;
    }

    .topic-content {
        font-size: 13px;
        color: grey;
        margin: 5px 0;
        display: -webkit-box;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 3;
        overflow: hidden;
        text-overflow: ellipsis;
    }

    .topic-image {
        width: 100%;
        height: 100%;
        max-height: 110px;
        border-radius: 5px;
    }
}

.info-text {
    display: flex;
    justify-content: space-between;
    color: grey;
    font-size: 14px;
}

.friend-link {
    border-radius: 5px;
    overflow: hidden;
}

.draft-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.draft-item {
    border: solid 1px var(--el-border-color);
    border-radius: 6px;
    padding: 12px;
}

.draft-item-header {
    display: flex;
    gap: 10px;
    justify-content: space-between;
}

.draft-title {
    font-size: 14px;
    font-weight: bold;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.draft-summary {
    color: grey;
    display: -webkit-box;
    font-size: 13px;
    line-height: 1.5;
    margin-top: 8px;
    overflow: hidden;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
}

.draft-footer {
    align-items: center;
    color: grey;
    display: flex;
    font-size: 12px;
    justify-content: space-between;
    margin-top: 10px;
}

.create-topic {
    background-color: #efefef;
    border-radius: 5px;
    height: 40px;
    color: grey;
    font-size: 14px;
    line-height: 40px;
    padding: 0 10px;

    &:hover {
        cursor: pointer;
    }
}

.dark {
    .create-topic {
        background-color: #232323;
    }

    .type-select-card {
        background-color: #282828;

        &.active {
            border: solid 1px #64594b;
        }

        &:hover {
            background-color: #5e5e5e;
        }
    }
}
</style>
