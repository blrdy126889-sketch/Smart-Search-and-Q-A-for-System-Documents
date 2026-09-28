<template>
  <div>
    <div class="page-card">
      <div class="toolbar">
        <span class="page-title" style="margin: 0">✅ 审核中心</span>
        <el-radio-group v-model="tab">
          <el-radio-button value="PENDING_AUDIT">待审核（{{ pending.length }}）</el-radio-button>
          <el-radio-button value="AUDITED">已审记录</el-radio-button>
        </el-radio-group>
      </div>
      <el-table :data="tab === 'PENDING_AUDIT' ? pending : audited" v-loading="loading">
        <el-table-column prop="docCode" label="编号" width="130" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="ownerName" label="上传人" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="{ PENDING_AUDIT: 'warning', PUBLISHED: 'success', REJECTED: 'danger', OFFLINE: 'info' }[row.status]">
              {{ { PENDING_AUDIT: '待审核', PUBLISHED: '已发布', REJECTED: '已驳回', OFFLINE: '已下线' }[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="auditRemark" label="审核意见" min-width="140" show-overflow-tooltip />
        <el-table-column prop="updatedAt" label="时间" width="160" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING_AUDIT'">
              <el-button size="small" text type="primary" @click="openAudit(row, true)">通过</el-button>
              <el-button size="small" text type="danger" @click="openAudit(row, false)">驳回</el-button>
            </template>
            <el-button size="small" text @click="router.push(`/docs/${row.docId ?? row.id}`)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="auditVisible" :title="passFlag ? '审核通过' : '驳回'" width="430px">
      <p style="margin-bottom: 12px"><b>{{ auditRow?.title }}</b></p>
      <el-input v-model="remark" type="textarea" :rows="3" :placeholder="passFlag ? '审核意见（选填）' : '驳回原因（必填）'" />
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button :type="passFlag ? 'primary' : 'danger'" @click="doAudit">确认{{ passFlag ? '通过' : '驳回' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { docApi } from '../../api'

const router = useRouter()
const tab = ref('PENDING_AUDIT')
const pending = ref<any[]>([])
const audited = ref<any[]>([])
const loading = ref(false)
const auditVisible = ref(false)
const passFlag = ref(true)
const remark = ref('')
const auditRow = ref<any>(null)

onMounted(load)

async function load() {
  loading.value = true
  try {
    const [p, a]: any[] = await Promise.all([
      docApi.list({ page: 1, size: 50, status: 'PENDING_AUDIT' }),
      docApi.list({ page: 1, size: 50 })
    ])
    pending.value = p.records || []
    audited.value = (a.records || []).filter((r: any) => ['PUBLISHED', 'REJECTED', 'OFFLINE'].includes(r.status))
  } finally { loading.value = false }
}

function openAudit(row: any, pass: boolean) {
  auditRow.value = row
  passFlag.value = pass
  remark.value = ''
  auditVisible.value = true
}

async function doAudit() {
  if (!passFlag.value && !remark.value.trim()) { ElMessage.warning('驳回原因必填'); return }
  await docApi.audit(auditRow.value.docId ?? auditRow.value.id, { pass: passFlag.value, remark: remark.value })
  ElMessage.success(passFlag.value ? '已通过并发布' : '已驳回')
  auditVisible.value = false
  load()
}
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
</style>
