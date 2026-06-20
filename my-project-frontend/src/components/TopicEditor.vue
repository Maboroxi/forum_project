<script setup>
import {Check, Document, Files} from "@element-plus/icons-vue";
import {computed, reactive, ref} from "vue";
import {Delta, Quill, QuillEditor} from "@vueup/vue-quill";
import ImageResize from "quill-image-resize-vue";
import { ImageExtend, QuillWatch } from "quill-image-super-solution-module";
import '@vueup/vue-quill/dist/vue-quill.snow.css';
import axios from "axios";
import {accessHeader} from "@/net";
import {ElMessage} from "element-plus";
import ColorDot from "@/components/ColorDot.vue";
import {useStore} from "@/store";
import {apiForumTopicCreate, apiForumTopicDraftDelete, apiForumTopicDraftSave} from "@/net/api/forum";
import { isMobile } from "@/utils/device";

const store = useStore()

const props = defineProps({
    show: Boolean,
    defaultTitle: {
        default: '',
        type: String
    },
    defaultText: {
        default: '',
        type: String
    },
    defaultType: {
        default: null,
        type: Number
    },
    draftId: {
        default: null,
        type: Number
    },
    draftable: {
        default: true,
        type: Boolean
    },
    submitButton: {
        default: '立即发表主题',
        type: String
    },
    submit: {
        default: (editor, success) => {
            apiForumTopicCreate({
                type: editor.type.id,
                title: editor.title,
                content: editor.text
            }, () => {
                ElMessage.success("帖子发表成功！")
                success()
            })
        },
        type: Function
    }
})

const emit = defineEmits(['close', 'success', 'draft-save'])

const refEditor = ref()
const currentDraftId = ref(null)
const editor = reactive({
    type: null,
    title: '',
    text: '',
    loading: false,
    uploading: false
})

function initEditor() {
    currentDraftId.value = props.draftId
    if(props.defaultText) {
        const content = typeof props.defaultText === 'string' ? JSON.parse(props.defaultText) : props.defaultText
        editor.text = new Delta(content)
    } else {
        refEditor.value.setContents('', 'user')
        editor.text = ''
    }
    editor.title = props.defaultTitle
    editor.type = findTypeById(props.defaultType)
}

function deltaToText(delta) {
    if(!delta.ops) return ""
    let str = ""
    for (let op of delta.ops)
        str += op.insert
    return str.replace(/\s/g, "")
}

const contentLength = computed(() => deltaToText(editor.text).length)
const editorTitle = computed(() => props.draftable && currentDraftId.value ? '编辑帖子草稿' : '发表新的帖子')
const editorHeight = computed(() => isMobile.value ? 220 : 440)
const drawerSize = computed(() => isMobile.value ? '90vh' : 650)

function findTypeById(id){
    for (let type of store.forum.types) {
        if(type.id === id)
            return type
    }
}

function saveDraft() {
    apiForumTopicDraftSave({
        id: currentDraftId.value,
        title: editor.title,
        type: editor.type?.id,
        content: editor.text?.ops ? editor.text : null
    }, draft => {
        currentDraftId.value = draft.id
        ElMessage.success('草稿保存成功！')
        emit('draft-save', draft)
    })
}

function submitTopic() {
    const text = deltaToText(editor.text)
    if(text.length > 20000) {
        ElMessage.warning('字数超出限制，无法发布主题！')
        return
    }
    if(!editor.title) {
        ElMessage.warning('请填写标题！')
        return
    }
    if(!editor.type) {
        ElMessage.warning('请选择一个合适的帖子类型！')
        return
    }
    props.submit(editor, () => {
        if(props.draftable && currentDraftId.value) {
            apiForumTopicDraftDelete(currentDraftId.value,
                () => emit('success', currentDraftId.value),
                () => emit('success', currentDraftId.value))
        } else {
            emit('success', currentDraftId.value)
        }
    })
}

Quill.register('modules/imageResize', ImageResize)
Quill.register('modules/ImageExtend', ImageExtend)
const editorOption = {
    modules: {
        toolbar: {
            container: [
                "bold", "italic", "underline", "strike","clean",
                {color: []}, {'background': []},
                {size: ["small", false, "large", "huge"]},
                { header: [1, 2, 3, 4, 5, 6, false] },
                {list: "ordered"}, {list: "bullet"}, {align: []},
                "blockquote", "code-block", "link", "image",
                { indent: '-1' }, { indent: '+1' }
            ],
            handlers: {
                'image': function () {
                    QuillWatch.emit(this.quill.id)
                }
            }
        },
        imageResize: {
            modules: [ 'Resize', 'DisplaySize' ]
        },
        ImageExtend: {
            action:  axios.defaults.baseURL + '/api/image/cache',
            name: 'file',
            size: 5,
            loading: true,
            accept: 'image/png, image/jpeg',
            response: (resp) => {
                if(resp.data) {
                    return axios.defaults.baseURL + '/images' + resp.data
                } else {
                    return null
                }
            },
            methods: 'POST',
            headers: xhr => {
                xhr.setRequestHeader('Authorization', accessHeader().Authorization);
            },
            start: () => editor.uploading = true,
            success: () => {
                ElMessage.success('图片上传成功!')
                editor.uploading = false
            },
            error: () => {
                ElMessage.warning('图片上传失败，请联系管理员!')
                editor.uploading = false
            }
        }
    }
}
</script>

<template>
  <el-drawer :model-value="show"
             class="topic-editor-drawer"
             direction="btt"
             @open="initEditor"
             :close-on-click-modal="false"
             :size="drawerSize"
             @close="emit('close')">
    <template #header>
      <div>
        <div style="font-weight: bold">{{editorTitle}}</div>
        <div style="font-size: 13px">发表内容之前，请遵守相关法律法规，不要出现骂人等爆粗口的不文明行为。</div>
      </div>
    </template>
    <div style="display: flex;gap: 10px">
      <div style="width: 150px">
        <el-select placeholder="选择主题类型..." value-key="id" v-model="editor.type" :disabled="!store.forum.types.length">
          <el-option v-for="item in store.forum.types.filter(type => type.id > 0)" :value="item" :label="item.name">
            <div>
              <color-dot :color="item.color"/>
              <span style="margin-left: 10px">{{item.name}}</span>
            </div>
          </el-option>
        </el-select>
      </div>
      <div style="flex: 1">
        <el-input v-model="editor.title" placeholder="请输入帖子标题..." :prefix-icon="Document"
                  style="height: 100%" maxlength="30"/>
      </div>
    </div>
    <div style="margin-top: 5px;font-size: 13px;color: grey">
      <color-dot :color="editor.type ? editor.type.color : '#dedede'"/>
      <span style="margin-left: 5px">{{editor.type ? editor.type.desc : '请在上方选择一个帖子类型'}}</span>
    </div>
    <div :style="{ marginTop: '10px', height: editorHeight + 'px', overflow: 'hidden', borderRadius: '5px' }"
         v-loading="editor.uploading"
         element-loading-text="这种上传图片，请稍后...">
      <quill-editor v-model:content="editor.text" style="height: calc(100% - 45px)"
                    content-type="delta" ref="refEditor"
                    placeholder="今天想分享点什么呢？" :options="editorOption"/>
    </div>
    <div style="display: flex;justify-content: space-between;margin-top: 5px">
      <div style="color: grey;font-size: 13px">
        当前字数 {{contentLength}}（最大支持20000字）
      </div>
      <div>
        <el-button v-if="draftable" :icon="Files" @click="saveDraft" plain>保存草稿</el-button>
        <el-button type="success" :icon="Check" @click="submitTopic" plain>{{submitButton}}</el-button>
      </div>
    </div>
  </el-drawer>
</template>

<style scoped>
:deep(.topic-editor-drawer) {
    width: 800px;
    margin: auto;
    border-radius: 10px 10px 0 0;
}

@media (max-width: 768px) {
    :deep(.topic-editor-drawer) {
        width: 100% !important;
    }
}
:deep(.topic-editor-drawer .el-drawer__header) {
    margin: 0;
}
</style>
