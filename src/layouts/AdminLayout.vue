<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="aside">
      <div class="aside-brand" @click="router.push('/home')">
        <div class="logo">智</div>
        <span>管理控制台</span>
      </div>
      <el-menu :default-active="$route.path" router class="menu">
        <el-menu-item index="/admin/dashboard"><el-icon><Odometer /></el-icon>仪表盘</el-menu-item>
        <el-menu-item index="/admin/documents"><el-icon><Document /></el-icon>文档管理</el-menu-item>
        <el-menu-item index="/admin/categories"><el-icon><FolderOpened /></el-icon>分类管理</el-menu-item>
        <el-menu-item index="/admin/audit"><el-icon><Checked /></el-icon>审核中心</el-menu-item>
        <el-menu-item index="/admin/stats"><el-icon><TrendCharts /></el-icon>统计分析</el-menu-item>
        <el-menu-item index="/admin/users"><el-icon><UserFilled /></el-icon>用户管理</el-menu-item>
        <el-menu-item index="/admin/roles"><el-icon><Key /></el-icon>角色权限</el-menu-item>
        <el-menu-item index="/admin/logs"><el-icon><Tickets /></el-icon>日志查询</el-menu-item>
        <el-menu-item index="/admin/settings"><el-icon><Setting /></el-icon>系统设置</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header height="56px" class="header">
        <span class="crumb">制度文档智能检索与问答系统 / 管理端</span>
        <div class="header-right">
          <router-link to="/home" class="back-portal">返回用户端</router-link>
          <span class="user">{{ userStore.nickname || userStore.username }}</span>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { Odometer, Document, FolderOpened, Checked, TrendCharts, UserFilled, Key, Tickets, Setting } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
</script>

<style scoped>
.admin-layout { height: 100%; }
.aside { background: #fff; border-right: 1px solid #eef0f5; display: flex; flex-direction: column; }
.aside-brand { height: 56px; display: flex; align-items: center; gap: 10px; padding: 0 20px; font-weight: 600; cursor: pointer; border-bottom: 1px solid #f0f2f7; }
.logo {
  width: 30px; height: 30px; border-radius: 8px; background: linear-gradient(135deg, #4f6ef7, #7c9bff);
  color: #fff; display: flex; align-items: center; justify-content: center; font-size: 14px;
}
.menu { border-right: none; flex: 1; }
.header { background: #fff; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #eef0f5; }
.crumb { color: #7a8699; font-size: 13px; }
.header-right { display: flex; align-items: center; gap: 16px; }
.back-portal { color: var(--primary); text-decoration: none; font-size: 13px; }
.user { font-size: 14px; color: #2c3e50; }
.main { background: #f5f7fb; }
</style>
