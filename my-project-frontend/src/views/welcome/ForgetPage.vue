<template>
  <div v-if="isMobile" class="mobile-forget">
    <van-steps :active="active" :steps="steps" />
    <!-- 步骤1：验证邮箱 -->
    <van-form @submit="confirmReset" v-if="active === 0" style="margin-top: 30px">
      <van-field
        v-model="form.email" name="email" label="邮箱"
        placeholder="电子邮件地址" type="email"
        :rules="[{ required: true, message: '请输入邮箱' }, { validator: (v) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v) || '邮箱格式不正确' }]"
        clearable
      />
      <van-field
        v-model="form.code" name="code" label="验证码"
        placeholder="输入验证码" maxlength="6"
        :rules="[{ required: true, message: '请输入验证码' }]"
        clearable
      >
        <template #button>
          <van-button size="small" type="primary" :disabled="coldTime > 0" @click="askCode">
            {{ coldTime > 0 ? coldTime + 's' : '获取验证码' }}
          </van-button>
        </template>
      </van-field>
      <div style="margin: 30px 16px">
        <van-button round block type="primary" native-type="submit">下一步</van-button>
      </div>
    </van-form>
    <!-- 步骤2：重置密码 -->
    <van-form @submit="doReset" v-if="active === 1" style="margin-top: 30px">
      <van-field
        v-model="form.password" type="password" name="password" label="新密码"
        placeholder="6-16位密码"
        :rules="[{ required: true, message: '请输入密码' }]"
        clearable maxlength="16"
      />
      <van-field
        v-model="form.password_repeat" type="password" name="password_repeat" label="确认密码"
        placeholder="再次输入新密码"
        :rules="[{ required: true, message: '请确认密码' }, { validator: (v) => v === form.password || '两次密码不一致' }]"
        clearable maxlength="16"
      />
      <div style="margin: 30px 16px">
        <van-button round block type="primary" native-type="submit">立即重置</van-button>
      </div>
    </van-form>
  </div>
  <!-- 桌面端 -->
  <div v-else>
    <div style="margin: 30px 20px">
      <el-steps :active="active" finish-status="success" align-center>
        <el-step title="验证电子邮件" />
        <el-step title="重新设定密码" />
      </el-steps>
    </div>
    <transition name="el-fade-in-linear" mode="out-in">
      <div style="text-align: center;margin: 0 20px;height: 100%" v-if="active === 0">
        <div style="margin-top: 80px">
          <div style="font-size: 25px;font-weight: bold">重置密码</div>
          <div style="font-size: 14px;color: grey">请输入需要重置密码的电子邮件地址</div>
        </div>
        <div style="margin-top: 50px">
          <el-form :model="form" :rules="rules" @validate="onValidate" ref="formRef">
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
                  <el-button type="success" @click="validateEmail"
                             :disabled="!isEmailValid || coldTime > 0">
                    {{coldTime > 0 ? '请稍后 ' + coldTime + ' 秒' : '获取验证码'}}
                  </el-button>
                </el-col>
              </el-row>
            </el-form-item>
          </el-form>
        </div>
        <div style="margin-top: 70px">
          <el-button @click="confirmReset()" style="width: 270px;" type="danger" plain>开始重置密码</el-button>
        </div>
      </div>
    </transition>
    <transition name="el-fade-in-linear" mode="out-in">
      <div style="text-align: center;margin: 0 20px;height: 100%" v-if="active === 1">
        <div style="margin-top: 80px">
          <div style="font-size: 25px;font-weight: bold">重置密码</div>
          <div style="font-size: 14px;color: grey">请填写您的新密码，务必牢记，防止丢失</div>
        </div>
        <div style="margin-top: 50px">
          <el-form :model="form" :rules="rules" @validate="onValidate" ref="formRef">
            <el-form-item prop="password">
              <el-input v-model="form.password" :maxlength="16" type="password" placeholder="新密码">
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item prop="password_repeat">
              <el-input v-model="form.password_repeat" :maxlength="16" type="password" placeholder="重复新密码">
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>
          </el-form>
        </div>
        <div style="margin-top: 70px">
          <el-button @click="doReset()" style="width: 270px;" type="danger" plain>立即重置密码</el-button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import {reactive, ref} from "vue";
import {EditPen, Lock, Message} from "@element-plus/icons-vue";
import {apiAuthAskCode, apiAuthResetPassword, apiAuthRestConfirm} from "@/net/api/user";
import { isMobile } from "@/utils/device";

const active = ref(0)
const steps = [
  { text: '验证邮箱' },
  { text: '重置密码' }
]

const form = reactive({
    email: '',
    code: '',
    password: '',
    password_repeat: '',
})

const rules = {
    email: [
        { required: true, message: '请输入邮件地址', trigger: 'blur' },
        {type: 'email', message: '请输入合法的电子邮件地址', trigger: ['blur', 'change']}
    ],
    code: [
        { required: true, message: '请输入获取的验证码', trigger: 'blur' },
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 16, message: '密码的长度必须在6-16个字符之间', trigger: ['blur'] }
    ],
    password_repeat: [
        { required: true, message: '请再次输入密码', trigger: 'blur' },
        { validator: (rule, value, callback) => {
            if (value !== form.password) callback(new Error('两次输入的密码不一致'))
            else callback()
        }, trigger: ['blur', 'change'] }
    ],
}

const formRef = ref()
const isEmailValid = ref(false)
const coldTime = ref(0)

const onValidate = (prop, isValid) => {
    if(prop === 'email')
        isEmailValid.value = isValid
}

function askCode() {
  apiAuthAskCode(form.email, coldTime, 'reset')
}

const validateEmail = () => apiAuthAskCode(form.email, coldTime, 'reset')

const confirmReset = () => {
  apiAuthRestConfirm({ email: form.email, code: form.code }, active)
}

const doReset = () => {
  apiAuthResetPassword({
    email: form.email,
    code: form.code,
    password: form.password
  })
}
</script>

<style scoped>
.mobile-forget {
  padding: 16px 0;
}
</style>
