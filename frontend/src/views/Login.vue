<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <h2>护理不良事件上报系统</h2>
      <el-form :model="form" label-width="64px" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" @keyup.enter="onLogin" />
        </el-form-item>
        <el-button type="primary" style="width:100%" :loading="loading" @click="onLogin">登录</el-button>
      </el-form>
      <p class="hint">演示账号：admin / head1 / nurse1 / nurse2，密码均为 123456</p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api/auth'
import { auth } from '../store/auth'

const router = useRouter()
const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function onLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await login(form)
    auth.setToken(res.data.token)
    auth.setUser(res.data.user)
    ElMessage.success('登录成功')
    router.push({ name: 'events' })
  } catch (e) {
    // 错误信息已由响应拦截器统一提示
  } finally {
    loading.value = false
  }
}
</script>
