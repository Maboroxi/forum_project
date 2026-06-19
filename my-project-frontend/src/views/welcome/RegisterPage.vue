<template>
  <!-- 移动端：Vant 表单 -->
  <div v-if="isMobile" class="mobile-register">
    <van-form @submit="register" ref="formRef">
      <van-field
        v-model="form.username" name="username" label="用户名"
        placeholder="2-8位，中文/英文/数字"
        :rules="[{ required: true, message: '请输入用户名' }, { validator: validateUsername }]"
        clearable maxlength="8"
      />
      <van-field
        v-model="form.email" name="email" label="邮箱"
        placeholder="电子邮件地址" type="email"
        :rules="[{ required: true, message: '请输入邮箱' }, { validator: validateEmail }]"
        clearable
      />
      <van-field
        v-model="form.code" name="code" label="验证码"
        placeholder="请输入验证码" maxlength="6"
        :rules="[{ required: true, message: '请输入验证码' }]"
        clearable
      >
        <template #button>
          <van-button size="small" type="primary" :disabled="coldTime > 0" @click="askCode">
            {{ coldTime > 0 ? coldTime + 's' : '获取验证码' }}
          </van-button>
        </template>
      </van-field>
      <van-field
        v-model="form.password" type="password" name="password" label="密码"
        placeholder="6-16位密码"
        :rules="[{ required: true, message: '请输入密码' }]"
        clearable maxlength="16"
      />
      <van-field
        v-model="form.password_repeat" type="password" name="password_repeat" label="确认密码"
        placeholder="再次输入密码"
        :rules="[{ required: true, message: '请确认密码' }, { validator: validatePassword }]"
        clearable maxlength="16"
      />
      <div style="margin: 20px 16px">
        <van-button round block type="primary" native-type="submit">立即注册</van-button>
      </div>
    </van-form>
    <div style="text-align: center;padding: 0 16px 20px">
      <span style="font-size: 13px;color: #666">已有账号？</span>
      <van-button type="default" size="small" plain @click="router.push('/')">立即登录</van-button>
    </div>
  </div>
  <!-- 桌面端：Element Plus 表单 -->
  <div v-else style="text-align: center;margin: 0 20px">
    <div style="margin-top: 100px">
      <div style="font-size: 25px;font-weight: bold">注册新用户</div>
      <div style="font-size: 14px;color: grey">欢迎注册我们的学习平台，请在下方填写相关信息</div>
    </div>
    <div style="margin-top: 50px">
      <el-form :model="form" :rules="rules" @validate="onValidate" ref="formRef2">
        <el-form-item prop="username">
          <el-input v-model="form.username" :maxlength="8" type="text" placeholder="用户名">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" :maxlength="16" type="password" placeholder="密码">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password_repeat">
          <el-input v-model="form.password_repeat" :maxlength="16" type="password" placeholder="重复密码">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="form.email" type="email" placeholder="电子邮件地址">
            <template #prefix><el-icon><Message /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="code">
          <el-row :gutter="10" style="width: 100%">
            <el-col :span="17">
              <el-input v-model="form.code" :maxlength="6" type="text" placeholder="请输入验证码">
                <template #prefix><el-icon><EditPen /></el-icon></template>
              </el-input>
            </el-col>
            <el-col :span="5">
              <el-button type="success" @click="sendEmailCode"
                         :disabled="!isEmailValid || coldTime > 0">
                {{coldTime > 0 ? '请稍后 ' + coldTime + ' 秒' : '获取验证码'}}
              </el-button>
            </el-col>
          </el-row>
        </el-form-item>
      </el-form>
    </div>
    <div style="margin-top: 80px">
      <el-button style="width: 270px" type="warning" @click="register" plain>立即注册</el-button>
    </div>
    <div style="margin-top: 20px">
      <span style="font-size: 14px;line-height: 15px;color: grey">已有账号? </span>
      <el-link type="primary" style="translate: 0 -2px" @click="router.push('/')">立即登录</el-link>
    </div>
  </div>
</template>

<script setup>
import {EditPen, Lock, Message, User} from "@element-plus/icons-vue";
import router from "@/router";
import {reactive, ref} from "vue";
import {ElMessage} from "element-plus";
import {apiAuthAskCode, apiAuthRegister} from "@/net/api/user";
import { isMobile } from "@/utils/device";
import { showToast, showFailToast } from 'vant';

const form = reactive({
    username: '',
    password: '',
    password_repeat: '',
    email: '',
    code: ''
})

const formRef = ref()
const formRef2 = ref()
const isEmailValid = ref(false)
const coldTime = ref(0)

function validateUsername(val) {
  if (!/^[a-zA-Z0-9一-龥]+$/.test(val)) {
    return '用户名不能包含特殊字符'
  }
  if (val.length < 2 || val.length > 8) {
    return '用户名长度2-8个字符'
  }
  return true
}

function validateEmail(val) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val) ? true : '请输入合法的电子邮件地址'
}

function validatePassword(val) {
  return val === form.password ? true : '两次输入的密码不一致'
}

const rules = {
    username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 2, max: 8, message: '用户名的长度必须在2-8个字符之间', trigger: ['blur', 'change'] },
        { pattern: /^[a-zA-Z0-9一-龥]+$/, message: '用户名不能包含特殊字符', trigger: ['blur', 'change'] }
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 16, message: '密码的长度必须在6-16个字符之间', trigger: ['blur', 'change'] }
    ],
    password_repeat: [
        { required: true, message: '请再次输入密码', trigger: 'blur' },
        { validator: (rule, value, callback) => {
            if (value !== form.password) callback(new Error('两次输入的密码不一致'))
            else callback()
        }, trigger: ['blur', 'change'] }
    ],
    email: [
        { required: true, message: '请输入邮件地址', trigger: 'blur' },
        {type: 'email', message: '请输入合法的电子邮件地址', trigger: ['blur', 'change']}
    ],
    code: [
        { required: true, message: '请输入获取的验证码', trigger: 'blur' },
    ]
}

const onValidate = (prop, isValid) => {
    if(prop === 'email')
        isEmailValid.value = isValid
}

function askCode() {
  // Vant 移动端验证码按钮
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    showFailToast('请输入有效的邮箱地址')
    return
  }
  apiAuthAskCode(form.email, coldTime, 'register')
}

// Element Plus 桌面端验证码按钮
const sendEmailCode = () => apiAuthAskCode(form.email, coldTime, 'register')

function register() {
  if (isMobile.value) {
    // Vant form 已通过 @submit 验证
    apiAuthRegister({
      username: form.username,
      password: form.password,
      email: form.email,
      code: form.code
    }, () => {
      showToast('注册成功')
      router.push('/')
    })
  } else {
    formRef2.value?.validate((isValid) => {
      if(isValid) {
        apiAuthRegister({
          username: form.username,
          password: form.password,
          email: form.email,
          code: form.code
        })
      } else {
        ElMessage.warning('请完整填写注册表单内容！')
      }
    })
  }
}

</script>

<style scoped>
.mobile-register {
  padding: 8px 0;
}
</style>
