<template>
  <div class="home">
    <div class="hero">
      <h1>您好，{{ userStore.nickname || userStore.username }} 👋</h1>
      <p class="text-sub">向制度知识库提问，或输入关键词开始混合检索</p>
      <div class="search-bar">
        <el-input v-model="q" size="large" placeholder="例如：差旅费报销标准是多少？" @keyup.enter="goSearch">
          <template #append>
            <el-button type="primary" @click="goSearch">搜 索</el-button>
          </template>
        </el-input>
        <el-button size="large" type="primary" plain class="ask-btn" @click="router.push('/chat')">💬 直接问 AI</el-button>
      </div>
    </div>

    <div class="cards">
      <div class="page-card stat" v-for="s in statCards" :key="s.label">
        <div class="num">{{ s.value }}</div>
        <div class="text-sub">{{ s.label }}</div>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="12">
        <div class="page-card">
          <div class="page-title">🔥 高频咨询问题 TOP5</div>
          <div v-if="hotQuestions.length === 0" class="text-sub empty">暂无数据，快去 AI 问答提一个问题吧</div>
          <div v-for="(hq, i) in hotQuestions" :key="i" class="hot-item" @click="goAsk(hq.question)">
            <span class="rank" :class="{ top: i < 3 }">{{ i + 1 }}</span>
            <span class="q">{{ hq.question }}</span>
            <span class="count">{{ hq.count }} 次</span>
          </div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="page-card">
          <div class="page-title">📚 热门制度文档 TOP5</div>
          <div v-if="hotDocs.length === 0" class="text-sub empty">暂无数据</div>
          <div v-for="(d, i) in hotDocs" :key="i" class="hot-item" @click="router.push(`/docs/${d.docId}`)">
            <span class="rank" :class="{ top: i < 3 }">{{ i + 1 }}</span>
            <span class="q">{{ d.docTitle }}</span>
            <span class="count">引用 {{ d.quoteCount }} 次</span>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { statsApi, searchApi } from '../api'

const router = useRouter()
const userStore = useUserStore()
const q = ref('')
const hotQuestions = ref<any[]>([])
const hotDocs = ref<any[]>([])
const statCards = ref([
  { label: '制度文档总数', value: '-' },
  { label: '已发布文档', value: '-' },
  { label: '今日问答次数', value: '-' }
])

onMounted(async () => {
  try {
    const ov: any = await statsApi.overview()
    statCards.value = [
      { label: '制度文档总数', value: ov.docCount ?? 0 },
      { label: '已发布文档', value: ov.publishedCount ?? 0 },
      { label: '今日问答次数', value: ov.todayQaCount ?? 0 }
    ]
  } catch { /* 后端未启动时静默 */ }
  try { hotQuestions.value = (await statsApi.hotQuestions(30, 5)) as any[] } catch { /* ignore */ }
  try { hotDocs.value = (await statsApi.docQuotes(30, 5)) as any[] } catch { /* ignore */ }
})

function goSearch() {
  if (!q.value.trim()) return
  router.push({ path: '/search', query: { q: q.value.trim() } })
}
function goAsk(question: string) {
  router.push({ path: '/chat', query: { q: question } })
}
</script>

<style scoped>
.hero { text-align: center; padding: 54px 0 36px; animation: fadeUp .5s var(--ease) both; }
.hero h1 { margin-bottom: 10px; font-size: 28px; font-weight: 700; }
.hero .text-sub { font-size: 14px; }
.search-bar { display: flex; gap: 12px; max-width: 660px; margin: 28px auto 0; }
.search-bar .el-input { flex: 1; }
.search-bar .el-input :deep(.el-input__wrapper) { box-shadow: 0 4px 20px rgba(29,33,41,.07); padding: 6px 16px; }
.cards { display: flex; gap: 16px; margin-bottom: 16px; }
.stat { flex: 1; text-align: center; padding: 20px 12px; }
.stat .num { font-size: 30px; font-weight: 800; background: var(--primary-grad); -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent; font-variant-numeric: tabular-nums; }
.hot-item { display: flex; align-items: center; gap: 12px; padding: 11px 10px; border-radius: 10px; cursor: pointer; transition: all .22s var(--ease); }
.hot-item:hover { background: var(--primary-light); transform: translateX(3px); }
.hot-item .rank { width: 22px; height: 22px; border-radius: 7px; background: #eef0f5; color: #86909c; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; }
.hot-item .rank.top { background: var(--primary-grad); color: #fff; box-shadow: 0 3px 8px rgba(59,91,253,.28); }
.hot-item .q { flex: 1; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hot-item .count { font-size: 12px; color: var(--text-sub); font-variant-numeric: tabular-nums; }
.empty { text-align: center; padding: 24px 0; }
</style>
