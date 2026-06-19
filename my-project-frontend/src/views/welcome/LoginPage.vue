<template>
  <!-- 移动端：Vant 表单 -->
  <div v-if="isMobile" class="mobile-login">
    <van-form @submit="userLogin">
      <van-field
        v-model="form.username"
        name="username"
        label="账号"
        placeholder="用户名/邮箱"
        :rules="[{ required: true, message: '请输入用户名' }]"
        clearable
      />
      <van-field
        v-model="form.password"
        type="password"
        name="password"
        label="密码"
        placeholder="请输入密码"
        :rules="[{ required: true, message: '请输入密码' }]"
        clearable
      />
      <div style="display: flex;justify-content: space-between;padding: 8px 16px">
        <van-checkbox v-model="form.remember" shape="square">记住我</van-checkbox>
        <van-button type="default" size="small" plain @click="router.push('/forget')">忘记密码？</van-button>
      </div>
      <div style="margin: 20px 16px">
        <van-button round block type="primary" native-type="submit">登录</van-button>
      </div>
    </van-form>
    <div style="text-align: center;padding: 0 16px">
      <van-divider>还没有账号</van-divider>
      <van-button round block plain type="primary" @click="router.push('/register')">注册账号</van-button>
    </div>
  </div>
  <!-- 桌面端：Element Plus 表单 -->
  <div v-else style="text-align: center;margin: 0 20px">
    <div style="margin-top: 150px">
      <div style="font-size: 25px;font-weight: bold">登录</div>
      <div style="font-size: 14px;color: grey">在进入系统之前请先输入用户名和密码进行登录</div>
    </div>
    <div style="margin-top: 50px">
      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="username">
          <el-input v-model="form.username" maxlength="20" type="text" placeholder="用户名/邮箱">
            <template #prefix>
              <el-icon><User/></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" maxlength="20" style="margin-top: 10px" placeholder="密码">
            <template #prefix>
              <el-icon><Lock/></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-row style="margin-top: 5px">
          <el-col :span="12" style="text-align: left">
            <el-form-item prop="remember">
              <el-checkbox v-model="form.remember" label="记住我"/>
            </el-form-item>
          </el-col>
          <el-col :span="12" style="text-align: right">
            <el-link @click="router.push('/forget')">忘记密码？</el-link>
          </el-col>
        </el-row>
      </el-form>
    </div>
    <div style="margin-top: 40px">
      <el-button @click="userLogin()" style="width: 270px" type="success" plain>立即登录</el-button>
    </div>
    <el-divider>
      <span style="color: grey;font-size: 13px">没有账号</span>
    </el-divider>
    <div>
      <el-button style="width: 270px" @click="router.push('/register')" type="warning" plain>注册账号</el-button>
    </div>
  </div>
</template>

<script setup>
import {User, Lock} from '@element-plus/icons-vue'
import router from "@/router";
import {inject, reactive, ref} from "vue";
import {login} from '@/net'
import {apiUserInfo} from "@/net/api/user";
import { isMobile } from "@/utils/device";
import { showToast } from 'vant';

const formRef = ref()
const form = reactive({
  username: '',
  password: '',
  remember: false
})

const rules = {
  username: [
    { required: true, message: '请输入用户名' }
  ],
  password: [
    { required: true, message: '请输入密码'}
  ]
}

const loading = inject('userLoading')

function userLogin() {
  if (isMobile.value) {
    // Vant 表单通过 @submit 触发，已经验证通过
    login(form.username, form.password, form.remember, () => {
      apiUserInfo(loading)
      router.push("/index")
    }, () => {
      showToast('登录失败，请检查账号密码')
    })
  } else {
    formRef.value?.validate((isValid) => {
      if(isValid) {
        login(form.username, form.password, form.remember, () => {
            apiUserInfo(loading)
            router.push("/index")
        })
      }
    });
  }
}
</script>

<style scoped>
.mobile-login {
  padding: 8px 0;
}
</style>
