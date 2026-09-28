<template>
  <div class="portal-layout">
    <header class="portal-header">
      <div class="brand" @click="router.push('/home')">
        <div class="logo">智</div>
        <span>制度文档智能检索与问答平台</span>
      </div>
      <nav class="nav">
        <router-link to="/home">首页</router-link>
        <router-link to="/search">智能检索</router-link>
        <router-link to="/chat">AI 问答</router-link>
        <router-link to="/docs">文档中心</router-link>
      </nav>
      <div class="right">
        <router-link v-if="userStore.canManage" to="/admin" class="admin-entry">管理端</router-link>
        <el-dropdown @command="onCommand">
          <span class="user-chip">
            <el-icon><User /></el-icon>
            {{ userStore.nickname || userStore.username }}
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="my">我的收藏/订阅</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>
    <main class="portal-main">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { authApi } from '../api'

const router = useRouter()
const userStore = useUserStore()

async function onCommand(cmd: string) {
  if (cmd === 'my') router.push('/my')
  else if (cmd === 'logout') {
    try { await authApi.logout() } catch { /* ignore */ }
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped>
.portal-layout { min-height: 100%; display: flex; flex-direction: column; }
.portal-header {
  height: 60px; background: #fff; display: flex; align-items: center; padding: 0 28px;
  box-shadow: 0 1px 8px rgba(0,0,0,.05); position: sticky; top: 0; z-index: 100; gap: 40px;
}
.brand { display: flex; align-items: center; gap: 10px; cursor: pointer; font-weight: 600; font-size: 16px; }
.logo {
  width: 32px; height: 32px; border-radius: 8px; background: linear-gradient(135deg, #4f6ef7, #7c9bff);
  color: #fff; display: flex; align-items: center; justify-content: center; font-size: 15px;
}
.nav { display: flex; gap: 8px; flex: 1; }
.nav a {
  padding: 8px 16px; border-radius: 8px; color: #4a5568; text-decoration: none; font-size: 14px; transition: all .2s;
}
.nav a:hover { background: var(--primary-light); color: var(--primary); }
.nav a.router-link-active { background: var(--primary-light); color: var(--primary); font-weight: 600; }
.right { display: flex; align-items: center; gap: 18px; }
.admin-entry { color: var(--primary); text-decoration: none; font-size: 14px; }
.user-chip { display: flex; align-items: center; gap: 6px; cursor: pointer; font-size: 14px; color: #4a5568; }
.portal-main { flex: 1; padding: 24px; max-width: 1280px; width: 100%; margin: 0 auto; }
</style>
