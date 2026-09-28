<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-left">
        <h1>制度文档智能检索与问答平台</h1>
        <p class="slogan">知识库构建 · 混合检索 · 智能问答</p>
        <ul class="features">
          <li>📄 制度文档统一管理与版本审核</li>
          <li>🔎 关键词 BM25 + 语义向量双路召回</li>
          <li>💬 RAG 智能问答，答案自动标注来源</li>
          <li>📊 高频问题与引用热度可视化</li>
        </ul>
      </div>
      <div class="login-right">
        <h2>欢迎登录</h2>
        <el-form :model="form" @keyup.enter="onLogin">
          <el-form-item>
            <el-input v-model="form.username" placeholder="用户名" size="large" prefix-icon="User" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" placeholder="密码" size="large" prefix-icon="Lock" show-password />
          </el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="onLogin">登 录</el-button>
        </el-form>
        <div class="tips text-sub">
          内置账号：admin/123456（管理员）、zhangsan/123456（普通用户）
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '123456' })

async function onLogin() {
  if (!form.username || !form.password) { ElMessage.warning('请输入用户名和密码'); return }
  loading.value = true
  try {
    const data: any = await authApi.login(form)
    userStore.setLogin(data)
    ElMessage.success('登录成功')
    router.push(data.roles?.includes('USER') && data.roles?.length === 1 ? '/home' : '/admin/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100%; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #4f6ef7 0%, #7c9bff 50%, #a5b4fc 100%);
}
.login-card {
  width: 860px; min-height: 480px; border-radius: 20px; overflow: hidden; display: flex;
  box-shadow: 0 20px 60px rgba(30, 41, 120, .35); background: #fff;
}
.login-left {
  flex: 1.1; background: linear-gradient(160deg, #4338ca, #4f6ef7); color: #fff; padding: 48px 40px;
  display: flex; flex-direction: column; justify-content: center;
}
.login-left h1 { font-size: 24px; margin-bottom: 10px; line-height: 1.5; }
.slogan { opacity: .85; margin-bottom: 28px; font-size: 14px; letter-spacing: 2px; }
.features { list-style: none; }
.features li { padding: 8px 0; font-size: 14px; opacity: .92; }
.login-right { flex: 1; padding: 48px 44px; display: flex; flex-direction: column; justify-content: center; }
.login-right h2 { margin-bottom: 26px; color: #2c3e50; }
.login-btn { width: 100%; margin-top: 6px; }
.tips { margin-top: 18px; line-height: 1.8; }
</style>
