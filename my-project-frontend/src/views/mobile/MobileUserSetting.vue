<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from '@/store'
import { accessHeader, logout } from '@/net'
import axios from 'axios'
import { showToast, showConfirmDialog, showLoadingToast, closeToast } from 'vant'
import { apiUserDetail, apiUserDetailSave, apiUserModifyEmail, apiAuthAskCode } from '@/net/api/user'

const router = useRouter()
const store = useStore()

const registerTime = computed(() => {
  if (store.user.registerTime) {
    return new Date(store.user.registerTime).toLocaleString()
  }
  return ''
})

// ---- 个人信息 ----
const baseForm = reactive({
  username: '',
  gender: 1,
  phone: '',
  qq: '',
  wx: '',
  desc: ''
})
const baseLoading = ref(false)
const baseFormRef = ref()

function saveDetails() {
  baseFormRef.value?.validate().then(() => {
    baseLoading.value = true
    apiUserDetailSave(baseForm, () => {
      showToast('用户信息保存成功')
      store.user.username = baseForm.username
      baseLoading.value = false
    }, () => {
      baseLoading.value = false
    })
  }).catch(() => {})
}

// ---- 头像 ----
function beforeAvatarUpload(file) {
  if (file.type !== 'image/jpeg' && file.type !== 'image/png') {
    showToast('头像只能是 JPG/PNG 格式')
    return false
  }
  if (file.size / 1024 > 100) {
    showToast('头像大小不能大于 100KB')
    return false
  }
  return true
}

function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file.file)
  const toast = showLoadingToast({
    message: '上传中...',
    forbidClick: true,
    duration: 0
  })
  axios.post('/api/image/avatar', formData, {
    headers: { ...accessHeader(), 'Content-Type': 'multipart/form-data' }
  }).then(res => {
    closeToast()
    store.user.avatar = res.data.data
    showToast('头像更新成功')
  }).catch(() => {
    closeToast()
    showToast('上传失败')
  })
}

// ---- 邮箱修改 ----
const emailForm = reactive({ email: '', code: '' })
const coldTime = ref(0)
const emailFormRef = ref()
const emailRules = {
  email: [{ required: true, message: '请输入邮箱地址' }, { pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: '邮箱格式不正确' }],
  code: [{ required: true, message: '请输入验证码' }]
}

function sendEmailCode() {
  emailFormRef.value?.validate('email').then(() => {
    apiAuthAskCode(emailForm.email, coldTime, 'modify')
  }).catch(() => {})
}

function modifyEmail() {
  emailFormRef.value?.validate().then(() => {
    apiUserModifyEmail(emailForm, () => {
      showToast('邮箱修改成功')
      store.user.email = emailForm.email
      emailForm.code = ''
    })
  }).catch(() => {})
}

// ---- 退出登录 ----
function userLogout() {
  showConfirmDialog({
    title: '提示',
    message: '确定要退出登录吗？'
  }).then(() => {
    logout(() => router.push('/'))
  }).catch(() => {})
}

// ---- 初始化 ----
onMounted(() => {
  apiUserDetail(data => {
    baseForm.username = store.user.username
    baseForm.desc = data.desc || ''
    baseForm.gender = data.gender ?? 1
    baseForm.phone = data.phone || ''
    baseForm.qq = data.qq || ''
    baseForm.wx = data.wx || ''
    emailForm.email = store.user.email
  })
})
</script>

<template>
  <div class="mobile-user-setting">
    <!-- 用户头像卡片 -->
    <div class="profile-card">
      <div class="avatar-wrapper">
        <van-uploader
          :after-read="uploadAvatar"
          :before-read="beforeAvatarUpload"
          max-count="1"
          accept="image/png,image/jpeg"
        >
          <div class="avatar-inner">
            <van-image
              round
              width="72"
              height="72"
              :src="store.avatarUrl"
              fit="cover"
            />
            <div class="avatar-overlay">
              <van-icon name="photograph" size="20" color="#fff" />
            </div>
          </div>
        </van-uploader>
      </div>
      <div class="profile-info">
        <div class="profile-name">{{ store.user.username }}</div>
        <div class="profile-email">{{ store.user.email }}</div>
        <div class="profile-bio" v-if="baseForm.desc">{{ baseForm.desc }}</div>
        <div class="profile-bio muted" v-else>这个用户很懒，没有填写个人简介~</div>
      </div>
      <div class="profile-register">注册时间：{{ registerTime }}</div>
    </div>

    <!-- 本人信息编辑 -->
    <div class="section-title">个人信息</div>
    <van-form ref="baseFormRef" @submit="saveDetails">
      <van-cell-group inset>
        <van-field
          v-model="baseForm.username"
          label="用户名"
          placeholder="请输入用户名"
          maxlength="10"
          :rules="[
            { required: true, message: '请输入用户名' },
            { pattern: /^[a-zA-Z0-9一-龥]+$/, message: '不能包含特殊字符' },
            { validator: (v) => v.length >= 2 || '至少2个字符' }
          ]"
        />
        <van-field label="性别" class="no-input">
          <template #input>
            <van-radio-group v-model="baseForm.gender" direction="horizontal">
              <van-radio :name="0">男</van-radio>
              <van-radio :name="1">女</van-radio>
            </van-radio-group>
          </template>
        </van-field>
        <van-field
          v-model="baseForm.phone"
          label="手机号"
          placeholder="请输入手机号"
          maxlength="11"
          type="tel"
        />
        <van-field
          v-model="baseForm.qq"
          label="QQ号"
          placeholder="请输入QQ号"
          maxlength="13"
          type="digit"
        />
        <van-field
          v-model="baseForm.wx"
          label="微信号"
          placeholder="请输入微信号"
          maxlength="20"
        />
        <van-field
          v-model="baseForm.desc"
          label="个人简介"
          placeholder="介绍一下自己吧~"
          maxlength="200"
          type="textarea"
          autosize
        />
      </van-cell-group>
      <div style="padding: 16px">
        <van-button
          round
          block
          type="primary"
          native-type="submit"
          :loading="baseLoading"
        >
          保存个人信息
        </van-button>
      </div>
    </van-form>

    <!-- 邮箱修改 -->
    <div class="section-title">电子邮箱</div>
    <van-form ref="emailFormRef" @submit="modifyEmail">
      <van-cell-group inset>
        <van-field
          v-model="emailForm.email"
          label="邮箱"
          placeholder="请输入邮箱地址"
          :rules="emailRules.email"
        />
        <van-field
          v-model="emailForm.code"
          label="验证码"
          placeholder="请输入验证码"
          :rules="emailRules.code"
        >
          <template #button>
            <van-button
              size="small"
              type="primary"
              plain
              :disabled="coldTime > 0"
              @click="sendEmailCode"
            >
              {{ coldTime > 0 ? `${coldTime}s` : '获取验证码' }}
            </van-button>
          </template>
        </van-field>
      </van-cell-group>
      <div style="padding: 16px">
        <van-button
          round
          block
          type="primary"
          native-type="submit"
        >
          更新邮箱
        </van-button>
      </div>
    </van-form>

    <!-- 功能入口 -->
    <div class="section-title">更多</div>
    <van-cell-group inset>
      <van-cell title="我的帖子" is-link to="/index/forum-setting" />
      <van-cell title="隐私设置" is-link to="/index/privacy-setting" />
    </van-cell-group>

    <!-- 退出登录 -->
    <div style="padding: 24px 16px 40px">
      <van-button
        round
        block
        type="danger"
        plain
        @click="userLogout"
      >
        退出登录
      </van-button>
    </div>
  </div>
</template>

<style scoped>
.mobile-user-setting {
  padding-bottom: 12px;
}

.profile-card {
  background: #fff;
  margin: 12px 16px;
  border-radius: 12px;
  padding: 24px 20px 16px;
  text-align: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.avatar-wrapper {
  display: inline-block;
  position: relative;
}

.avatar-inner {
  position: relative;
  width: 72px;
  height: 72px;
  margin: 0 auto;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
  cursor: pointer;
}

.avatar-inner:hover .avatar-overlay {
  opacity: 1;
}

.profile-info {
  margin-top: 12px;
}

.profile-name {
  font-size: 18px;
  font-weight: 700;
  color: #323233;
}

.profile-email {
  font-size: 13px;
  color: #969799;
  margin-top: 4px;
}

.profile-bio {
  font-size: 13px;
  color: #646566;
  margin-top: 8px;
  line-height: 1.5;
}

.profile-bio.muted {
  color: #c8c9cc;
}

.profile-register {
  font-size: 12px;
  color: #c8c9cc;
  margin-top: 8px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #646566;
  padding: 16px 20px 8px;
}

:deep(.van-cell.no-input .van-field__body) {
  display: flex;
  align-items: center;
}
</style>
