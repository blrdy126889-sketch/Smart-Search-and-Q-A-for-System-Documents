<template>
  <div class="login-page">
    <div class="bg-deco deco-a"></div>
    <div class="bg-deco deco-b"></div>
    <div class="bg-grid"></div>

    <div class="login-card">
      <div class="login-left">
        <div class="brand-mark">智</div>
        <h1>制度知识<br/><span class="grad-text">智能中枢</span></h1>
        <p class="slogan">知识库构建 · 混合检索 · 智能问答</p>
        <ul class="features">
          <li><span class="fi">📄</span>制度文档全生命周期管理</li>
          <li><span class="fi">🔎</span>BM25 + 语义向量双路召回</li>
          <li><span class="fi">💬</span>RAG 问答，答案自动标注来源</li>
          <li><span class="fi">📊</span>高频问题与引用热度可视化</li>
        </ul>
      </div>
      <div class="login-right">
        <h2>欢迎回来</h2>
        <p class="sub">登录您的账号，开启智能制度问答</p>
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
          内置账号：admin / editor / auditor / zhangsan，密码均为 123456
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
  background: #f2f4fb; position: relative; overflow: hidden; padding: 24px;
}
/* 氛围光斑 */
.bg-deco { position: absolute; border-radius: 50%; filter: blur(90px); opacity: .5; pointer-events: none; }
.deco-a { width: 520px; height: 520px; background: #cdd9ff; top: -140px; left: -100px; animation: float 9s ease-in-out infinite; }
.deco-b { width: 460px; height: 460px; background: #e4d9ff; bottom: -160px; right: -80px; animation: float 11s ease-in-out infinite reverse; }
.bg-grid {
  position: absolute; inset: 0; pointer-events: none;
  background-image: linear-gradient(rgba(29,33,41,.035) 1px, transparent 1px), linear-gradient(90deg, rgba(29,33,41,.035) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: radial-gradient(ellipse 70% 60% at 50% 40%, #000 30%, transparent 100%);
  -webkit-mask-image: radial-gradient(ellipse 70% 60% at 50% 40%, #000 30%, transparent 100%);
}
@keyframes float { 0%,100% { transform: translate(0,0) } 50% { transform: translate(30px,-24px) } }

.login-card {
  position: relative; z-index: 1; width: 880px; min-height: 500px; border-radius: 24px;
  overflow: hidden; display: flex; background: rgba(255,255,255,.86);
  backdrop-filter: blur(18px); -webkit-backdrop-filter: blur(18px);
  border: 1px solid rgba(255,255,255,.7);
  box-shadow: 0 24px 64px rgba(44, 60, 150, .13), 0 2px 8px rgba(44,60,150,.06);
  animation: fadeUp .55s var(--ease) both;
}
.login-left {
  flex: 1.05; color: #fff; padding: 52px 44px; position: relative;
  background: linear-gradient(155deg, #2b3ecc 0%, #3b5bfd 55%, #7a5cff 100%);
  display: flex; flex-direction: column; justify-content: center;
  overflow: hidden;
}
.login-left::before {
  content: ''; position: absolute; inset: 0;
  background-image: linear-gradient(rgba(255,255,255,.07) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,.07) 1px, transparent 1px);
  background-size: 40px 40px;
  mask-image: radial-gradient(ellipse at 30% 30%, #000 20%, transparent 75%);
  -webkit-mask-image: radial-gradient(ellipse at 30% 30%, #000 20%, transparent 75%);
}
.brand-mark {
  width: 46px; height: 46px; border-radius: 14px; margin-bottom: 26px;
  background: rgba(255,255,255,.16); border: 1px solid rgba(255,255,255,.28);
  backdrop-filter: blur(6px);
  display: flex; align-items: center; justify-content: center; font-size: 21px; font-weight: 700;
}
.login-left h1 { font-size: 34px; line-height: 1.35; margin-bottom: 12px; font-weight: 700; letter-spacing: 1px; position: relative; }
.login-left h1 .grad-text {
  background: linear-gradient(90deg, #ffe9a8, #ffb2dd);
  -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent;
}
.slogan { opacity: .82; font-size: 13px; letter-spacing: 4px; margin-bottom: 34px; }
.features { list-style: none; position: relative; }
.features li { padding: 9px 0; font-size: 14px; opacity: .92; display: flex; align-items: center; gap: 10px; }
.features .fi {
  width: 30px; height: 30px; border-radius: 9px; flex-shrink: 0;
  background: rgba(255,255,255,.14); border: 1px solid rgba(255,255,255,.2);
  display: flex; align-items: center; justify-content: center; font-size: 14px;
}
.login-right { flex: 1; padding: 52px 46px; display: flex; flex-direction: column; justify-content: center; }
.login-right h2 { margin-bottom: 6px; font-size: 24px; font-weight: 700; color: var(--text-main); }
.login-right .sub { margin-bottom: 28px; }
.login-btn { width: 100%; margin-top: 6px; height: 44px; font-size: 15px; letter-spacing: 6px; border: none; background: var(--primary-grad); }
.tips { margin-top: 20px; line-height: 1.8; text-align: center; }
</style>
