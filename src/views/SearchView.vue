<template>
  <div class="search-page">
    <div class="page-card">
      <div class="search-box">
        <el-input v-model="q" size="large" placeholder="输入关键词，混合检索自动联想语义相关内容" clearable @keyup.enter="doSearch(1)" />
        <el-button type="primary" size="large" @click="doSearch(1)">搜索</el-button>
      </div>
      <div class="filters">
        <el-radio-group v-model="mode">
          <el-radio-button value="HYBRID">混合检索</el-radio-button>
          <el-radio-button value="KEYWORD">关键词（BM25）</el-radio-button>
          <el-radio-button value="SEMANTIC">语义向量</el-radio-button>
        </el-radio-group>
        <el-tree-select
          v-model="categoryId" :data="categoryTree" check-strictly clearable
          :props="{ label: 'categoryName', value: 'id' }" placeholder="全部分类" style="width: 200px" />
        <span v-if="searched && result" class="text-sub took">
          共 {{ result.total }} 条结果 · 耗时 {{ result.tookMs }}ms
        </span>
      </div>
    </div>

    <div v-if="loading" class="page-card"><el-skeleton :rows="6" animated /></div>

    <template v-else-if="result && result.records.length">
      <div v-for="hit in result.records" :key="hit.chunkId" class="page-card hit-card" @click="goDetail(hit)">
        <div class="hit-head">
          <span class="doc-title">{{ hit.docTitle }}</span>
          <span class="hit-types">
            <el-tag v-for="t in hit.hitTypes" :key="t" size="small" :type="t === 'BM25' ? 'warning' : 'success'" effect="plain">{{ t === 'BM25' ? '关键词' : '语义' }}</el-tag>
          </span>
        </div>
        <div v-if="hit.headingPath" class="text-sub path">📍 {{ hit.headingPath }}</div>
        <div class="snippet" v-html="hit.snippet"></div>
        <div class="score-bar"><i :style="{ width: (hit.score * 100).toFixed(1) + '%' }"></i></div>
        <div class="text-sub score">相关度 {{ (hit.score * 100).toFixed(1) }}%</div>
      </div>
      <div class="pager">
        <el-pagination background layout="prev, pager, next" :total="result.total" :page-size="size" :current-page="page" @current-change="doSearch" />
      </div>
    </template>

    <div v-else-if="searched" class="page-card empty">
      <el-empty description="未检索到相关内容，试试更换关键词或切换检索模式" />
    </div>
    <div v-else class="page-card empty">
      <el-empty description="输入关键词开始混合检索（BM25 + 语义向量双路召回）" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchApi, categoryApi } from '../api'

const route = useRoute()
const router = useRouter()
const q = ref((route.query.q as string) || '')
const mode = ref('HYBRID')
const categoryId = ref<number | undefined>(undefined)
const page = ref(1)
const size = 10
const loading = ref(false)
const searched = ref(false)
const result = ref<any>(null)
const categoryTree = ref<any[]>([])

onMounted(() => {
  if (q.value) doSearch(1)
  categoryApi.tree().then((t: any) => { categoryTree.value = t || [] }).catch(() => {})
})

async function doSearch(p = 1) {
  if (!q.value.trim()) return
  page.value = p
  loading.value = true
  searched.value = true
  try {
    result.value = await searchApi.search({
      q: q.value.trim(), mode: mode.value,
      categoryId: categoryId.value, page: page.value, size
    })
  } finally {
    loading.value = false
  }
}

function goDetail(hit: any) {
  router.push(`/docs/${hit.docId}`)
}
</script>

<style scoped>
.search-box { display: flex; gap: 12px; }
.search-box .el-input :deep(.el-input__wrapper) { padding: 8px 16px; box-shadow: 0 2px 12px rgba(29,33,41,.06); }
.filters { display: flex; align-items: center; gap: 16px; margin-top: 14px; }
.took { margin-left: auto; font-variant-numeric: tabular-nums; }
.hit { cursor: pointer; }
.hit-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.doc-title { font-size: 15.5px; }
.path { margin-bottom: 8px; }
.score { margin-top: 8px; font-variant-numeric: tabular-nums; }
.empty { min-height: 240px; display: flex; align-items: center; justify-content: center; }
.pager { display: flex; justify-content: center; padding: 14px 0; }
</style>
