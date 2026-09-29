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
  height: 62px; display: flex; align-items: center; padding: 0 28px; gap: 40px;
  background: rgba(255,255,255,.82); backdrop-filter: blur(14px); -webkit-backdrop-filter: blur(14px);
  border-bottom: 1px solid rgba(238,240,245,.9);
  position: sticky; top: 0; z-index: 100;
}
.brand { display: flex; align-items: center; gap: 11px; cursor: pointer; font-weight: 700; font-size: 16px; letter-spacing: .3px; }
.logo {
  width: 34px; height: 34px; border-radius: 11px; background: var(--primary-grad);
  color: #fff; display: flex; align-items: center; justify-content: center; font-size: 15px;
  box-shadow: 0 4px 12px rgba(59,91,253,.3);
}
.nav { display: flex; gap: 6px; flex: 1; }
.nav a {
  position: relative; padding: 8px 16px; border-radius: 10px; color: #4e5969;
  text-decoration: none; font-size: 14px; transition: all .25s var(--ease);
}
.nav a::after {
  content: ''; position: absolute; left: 50%; bottom: 2px; width: 0; height: 3px;
  border-radius: 2px; background: var(--primary-grad); transform: translateX(-50%); transition: width .3s var(--ease);
}
.nav a:hover { color: var(--primary); background: var(--primary-light); }
.nav a.router-link-active { color: var(--primary); font-weight: 600; }
.nav a.router-link-active::after { width: 18px; }
.right { display: flex; align-items: center; gap: 18px; }
.admin-entry {
  color: var(--primary); text-decoration: none; font-size: 13px; font-weight: 500;
  padding: 7px 14px; border-radius: 20px; border: 1px solid #dbe1fe; background: #fafbff;
  transition: all .25s var(--ease);
}
.admin-entry:hover { background: var(--primary-light); box-shadow: 0 2px 10px rgba(59,91,253,.14); }
.user-chip { display: flex; align-items: center; gap: 7px; cursor: pointer; font-size: 14px; color: #4e5969; font-weight: 500; }
.portal-main { flex: 1; padding: 26px 24px 34px; max-width: 1280px; width: 100%; margin: 0 auto; }
</style>
