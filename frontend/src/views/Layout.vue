<template>
  <el-container class="app-container">
    <el-header class="app-header">
      <div class="logo">护理不良事件上报与根因辅助分析系统</div>
      <div class="user">
        <span>{{ user ? user.realName : '' }}（{{ roleText }}）</span>
        <el-button text type="primary" @click="logout">退出</el-button>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px" style="background:#fff">
        <el-menu :default-active="activeMenu" router>
          <el-menu-item index="/report">
            <el-icon><Edit /></el-icon><span>事件填报</span>
          </el-menu-item>
          <el-menu-item index="/events">
            <el-icon><List /></el-icon><span>事件查询</span>
          </el-menu-item>
          <el-menu-item index="/rca">
            <el-icon><Connection /></el-icon><span>RCA 根因分析</span>
          </el-menu-item>
          <el-menu-item index="/statistics">
            <el-icon><DataAnalysis /></el-icon><span>统计分析</span>
          </el-menu-item>
          <el-menu-item v-if="isAdmin" index="/users">
            <el-icon><User /></el-icon><span>用户管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { auth } from '../store/auth'

const route = useRoute()
const router = useRouter()
const user = auth.getUser()
const activeMenu = computed(() => route.path)

const roleMap = { ADMIN: '系统管理员', HEAD_NURSE: '护士长', NURSE: '护士' }
const roleText = computed(() => (user ? roleMap[user.role] || user.role : ''))
const isAdmin = computed(() => user && user.role === 'ADMIN')

function logout() {
  auth.clear()
  router.push({ name: 'login' })
}
</script>
