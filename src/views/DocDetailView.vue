<template>
  <div v-if="doc" class="detail">
    <div class="page-card head-card">
      <div class="head">
        <h2>{{ doc.title }}</h2>
        <div class="actions">
          <el-button :type="doc.favorited ? 'warning' : 'default'" @click="toggleFavorite">
            {{ doc.favorited ? '⭐ 已收藏' : '☆ 收藏' }}
          </el-button>
          <el-button :type="doc.subscribed ? 'success' : 'default'" plain @click="toggleSubscribe">
            {{ doc.subscribed ? '已订阅更新' : '订阅更新' }}
          </el-button>
        </div>
      </div>
      <div class="meta text-sub">
        <span>编号：{{ doc.docCode }}</span>
        <span>格式：{{ doc.sourceType }}</span>
        <span>👁 {{ doc.viewCount }} 次浏览</span>
        <span>💬 被引用 {{ doc.quoteCount }} 次</span>
        <span v-if="doc.effectiveDate">生效日期：{{ doc.effectiveDate }}</span>
      </div>
      <div v-if="doc.summary" class="summary">
        <div class="label">🤖 AI 自动摘要</div>
        <div>{{ doc.summary }}</div>
      </div>
    </div>

    <div class="page-card">
      <div class="page-title">📚 版本记录</div>
      <el-table :data="doc.versions || []" size="default">
        <el-table-column prop="versionNo" label="版本" width="100" />
        <el-table-column prop="titleSnapshot" label="标题快照" min-width="180" />
        <el-table-column prop="indexStatus" label="索引状态" width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="idxType(row.indexStatus)">{{ idxText(row.indexStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="切片数" width="90" />
        <el-table-column prop="changeLog" label="变更说明" min-width="150" />
        <el-table-column prop="createdAt" label="上传时间" width="170" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="viewDiff(row)">查看内容</el-button>
            <el-button v-if="row.indexStatus === 'FAILED'" size="small" text type="warning" v-perm="'doc:edit'" @click="reindex(row)">重建索引</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 版本 diff 对话框 -->
    <el-dialog v-model="diffVisible" :title="diffTitle" width="860px" top="6vh">
      <div class="diff-toolbar">
        <el-select v-model="fromVersionId" placeholder="旧版本" size="small" style="width: 140px">
          <el-option v-for="v in doc.versions" :key="v.versionId" :label="v.versionNo" :value="v.versionId" />
        </el-select>
        <span class="arrow">→</span>
        <el-select v-model="toVersionId" placeholder="新版本" size="small" style="width: 140px">
          <el-option v-for="v in doc.versions" :key="v.versionId" :label="v.versionNo" :value="v.versionId" />
        </el-select>
        <el-button size="small" type="primary" @click="loadDiff">对比</el-button>
        <div class="legend text-sub">
          <span class="lg add">新增</span><span class="lg del">删除</span><span class="lg chg">修改</span>
        </div>
      </div>
      <div class="diff-body">
        <div v-if="diffSegments.length">
          <div v-for="(seg, i) in diffSegments" :key="i" class="diff-row" :class="seg.type">
            <div class="side from">{{ seg.fromText }}</div>
            <div class="side to">{{ seg.toText }}</div>
          </div>
        </div>
        <el-empty v-else description="选择两个版本进行对比" />
      </div>
    </el-dialog>
  </div>
  <div v-else class="page-card"><el-skeleton :rows="8" animated /></div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { docApi } from '../api'

const route = useRoute()
const docId = Number(route.params.id)
const doc = ref<any>(null)
const diffVisible = ref(false)
const diffTitle = ref('版本差异对比')
const diffSegments = ref<any[]>([])
const fromVersionId = ref<number>()
const toVersionId = ref<number>()

onMounted(load)

async function load() {
  doc.value = await docApi.detail(docId)
}

async function toggleFavorite() {
  if (doc.value.favorited) { await docApi.unfavorite(docId); doc.value.favorited = false }
  else { await docApi.favorite(docId); doc.value.favorited = true }
}

async function toggleSubscribe() {
  if (doc.value.subscribed) {
    await docApi.unsubscribe(doc.value.subscriptionId)
    doc.value.subscribed = false
  } else {
    await docApi.subscribe({ subType: 'DOC', targetId: docId })
    doc.value.subscribed = true
  }
  ElMessage.success('操作成功')
}

function viewDiff(row: any) {
  diffVisible.value = true
  diffSegments.value = []
  const vs = doc.value.versions || []
  const idx = vs.findIndex((v: any) => v.versionId === row.versionId)
  fromVersionId.value = vs[idx - 1]?.versionId ?? row.versionId
  toVersionId.value = row.versionId
  if (fromVersionId.value !== toVersionId.value) loadDiff()
}

async function loadDiff() {
  if (!fromVersionId.value || !toVersionId.value) return
  if (fromVersionId.value === toVersionId.value) { ElMessage.warning('请选择两个不同版本'); return }
  const res: any = await docApi.diff(docId, fromVersionId.value, toVersionId.value)
  diffSegments.value = res.segments || []
  diffTitle.value = `版本差异对比：${res.fromTitle} → ${res.toTitle}`
}

async function reindex(row: any) {
  await docApi.reindex(docId)
  ElMessage.success('已重新进入索引队列')
  load()
}

function idxType(s: string) {
  return { READY: 'success', FAILED: 'danger', PENDING: 'info', PARSING: 'warning', CHUNKING: 'warning', EMBEDDING: 'warning' }[s] || 'info'
}
function idxText(s: string) {
  return { READY: '索引就绪', FAILED: '索引失败', PENDING: '排队中', PARSING: '解析中', CHUNKING: '切片中', EMBEDDING: '向量化中' }[s] || s
}
</script>

<style scoped>
.detail { max-width: 980px; margin: 0 auto; }
.head-card .head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 10px; }
.head-card .meta { display: flex; gap: 18px; margin-bottom: 14px; }
.summary { background: var(--primary-light); border-radius: 10px; padding: 14px 16px; font-size: 14px; line-height: 1.8; }
.summary .label { font-weight: 600; color: var(--primary); margin-bottom: 6px; font-size: 13px; }
.diff-toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; }
.legend { margin-left: auto; display: flex; gap: 10px; }
.lg { padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.lg.add { background: #e9f9ee; color: #15803d; }
.lg.del { background: #fdecec; color: #b91c1c; }
.lg.chg { background: #fff7ed; color: #b45309; }
.diff-body { max-height: 60vh; overflow-y: auto; }
</style>
