<template>
  <div class="page-card">
    <el-tabs v-model="tab" @tab-change="load(1)">
      <el-tab-pane label="📝 操作日志" name="op">
        <el-table :data="rows" v-loading="loading">
          <el-table-column prop="createdAt" label="时间" width="170" />
          <el-table-column prop="username" label="操作人" width="110" />
          <el-table-column prop="module" label="模块" width="110" />
          <el-table-column prop="operation" label="操作" width="130" />
          <el-table-column prop="requestPath" label="接口" min-width="200" show-overflow-tooltip />
          <el-table-column label="结果" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.resultCode === 0 ? 'success' : 'danger'">{{ row.resultCode === 0 ? '成功' : '失败' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="costMs" label="耗时ms" width="90" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="💬 问答日志" name="qa">
        <el-table :data="rows" v-loading="loading">
          <el-table-column prop="createdAt" label="时间" width="170" />
          <el-table-column prop="username" label="提问人" width="110" />
          <el-table-column prop="question" label="问题" min-width="200" show-overflow-tooltip />
          <el-table-column prop="answer" label="答案摘要" min-width="240" show-overflow-tooltip />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="{ DONE: 'success', FAILED: 'danger' }[row.status] || 'info'">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="latencyMs" label="总耗时ms" width="100" />
          <el-table-column prop="firstTokenMs" label="首字ms" width="100" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="👁 访问日志" name="access">
        <el-table :data="rows" v-loading="loading">
          <el-table-column prop="createdAt" label="时间" width="170" />
          <el-table-column prop="username" label="用户" width="110" />
          <el-table-column prop="action" label="行为" width="120" />
          <el-table-column prop="docTitle" label="文档" min-width="200" show-overflow-tooltip />
          <el-table-column prop="queryText" label="来源检索词" min-width="150" show-overflow-tooltip />
          <el-table-column prop="ip" label="IP" width="140" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
    <div class="pager">
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="size" :current-page="page" @current-change="load" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { sysApi } from '../../api'

const tab = ref('op')
const rows = ref<any[]>([])
const page = ref(1)
const size = 15
const total = ref(0)
const loading = ref(false)

onMounted(() => load(1))

async function load(p = 1) {
  page.value = p
  loading.value = true
  try {
    const fn = tab.value === 'op' ? sysApi.opLogs : tab.value === 'qa' ? sysApi.qaLogs : sysApi.accessLogs
    const res: any = await fn({ page: p, size })
    rows.value = res.records || []
    total.value = res.total || 0
  } finally { loading.value = false }
}
</script>

<style scoped>
.pager { display: flex; justify-content: center; padding-top: 14px; }
</style>
