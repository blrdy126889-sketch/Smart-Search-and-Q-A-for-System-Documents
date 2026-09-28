<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="标题/编号" clearable style="width: 200px" @keyup.enter="load(1)" />
      <el-select v-model="status" placeholder="状态" clearable style="width: 140px">
        <el-option v-for="(t, s) in statusMap" :key="s" :label="t" :value="s" />
      </el-select>
      <el-button type="primary" @click="load(1)">查询</el-button>
      <el-button type="success" plain @click="uploadVisible = true">⬆ 上传文档</el-button>
    </div>

    <el-table :data="rows" v-loading="loading">
      <el-table-column prop="docCode" label="编号" width="130" />
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="categoryName" label="分类" width="110" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="{ PUBLISHED: 'success', PENDING_AUDIT: 'warning', REJECTED: 'danger', DRAFT: 'info', OFFLINE: 'info' }[row.status] || 'info'">
            {{ statusMap[row.status] || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="索引状态" width="105">
        <template #default="{ row }">
          <el-tag size="small" effect="plain" :type="{ READY: 'success', FAILED: 'danger' }[row.indexStatus] || 'info'">
            {{ idxMap[row.indexStatus] || row.indexStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览" width="70" />
      <el-table-column prop="quoteCount" label="引用" width="70" />
      <el-table-column prop="updatedAt" label="更新时间" width="160" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" size="small" text type="primary" v-perm="'doc:edit'" @click="submit(row)">提交审核</el-button>
          <el-button v-if="row.status === 'PENDING_AUDIT'" size="small" text type="warning" v-perm="'doc:audit'" @click="openAudit(row)">审核</el-button>
          <el-button v-if="row.status === 'PUBLISHED'" size="small" text type="danger" v-perm="'doc:audit'" @click="offline(row)">下线</el-button>
          <el-button v-if="row.indexStatus === 'FAILED'" size="small" text type="warning" v-perm="'doc:edit'" @click="reindex(row)">重建索引</el-button>
          <el-button size="small" text type="info" @click="router.push(`/docs/${row.docId ?? row.id}`)">详情</el-button>
          <el-button size="small" text type="danger" v-perm="'doc:delete'" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager">
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="size" :current-page="page" @current-change="load" />
    </div>

    <!-- 上传 -->
    <el-dialog v-model="uploadVisible" title="上传制度文档" width="470px">
      <el-form label-width="90px">
        <el-form-item label="文档分类">
          <el-tree-select v-model="upForm.categoryId" :data="tree" check-strictly :props="{ label: 'categoryName', value: 'id' }" style="width: 100%" />
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="upForm.title" placeholder="留空取文件名" /></el-form-item>
        <el-form-item label="编号"><el-input v-model="upForm.docCode" placeholder="留空自动生成" /></el-form-item>
        <el-form-item label="变更说明"><el-input v-model="upForm.changeLog" /></el-form-item>
        <el-form-item label="文件">
          <el-upload drag :auto-upload="false" :limit="1" :on-change="(f: any) => (file = f.raw)" accept=".txt,.doc,.docx,.pdf">
            <div class="el-upload__text">拖拽或点击选择（TXT/Word/PDF ≤ 50MB）</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="doUpload">上传并解析</el-button>
      </template>
    </el-dialog>

    <!-- 审核 -->
    <el-dialog v-model="auditVisible" title="文档审核" width="440px">
      <div class="audit-info">
        <p><b>{{ auditRow?.title }}</b></p>
        <p class="text-sub">{{ auditRow?.docCode }} · 上传人 {{ auditRow?.ownerName }}</p>
      </div>
      <el-input v-model="auditRemark" type="textarea" :rows="3" placeholder="审核意见（驳回时必填）" />
      <template #footer>
        <el-button type="danger" plain @click="audit(false)">驳 回</el-button>
        <el-button type="primary" @click="audit(true)">通过并发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { docApi, categoryApi } from '../../api'

const router = useRouter()
const keyword = ref('')
const status = ref('')
const rows = ref<any[]>([])
const page = ref(1)
const size = 10
const total = ref(0)
const loading = ref(false)
const tree = ref<any[]>([])
const uploadVisible = ref(false)
const uploading = ref(false)
const file = ref<File | null>(null)
const upForm = reactive({ categoryId: undefined as number | undefined, title: '', docCode: '', changeLog: '' })
const auditVisible = ref(false)
const auditRow = ref<any>(null)
const auditRemark = ref('')

const statusMap: any = { DRAFT: '草稿', PENDING_AUDIT: '待审核', REJECTED: '已驳回', PUBLISHED: '已发布', OFFLINE: '已下线' }
const idxMap: any = { PENDING: '排队中', PARSING: '解析中', CHUNKING: '切片中', EMBEDDING: '向量化中', READY: '就绪', FAILED: '失败' }

onMounted(() => {
  load(1)
  categoryApi.tree().then((t: any) => { tree.value = t || [] }).catch(() => {})
})

async function load(p = 1) {
  page.value = p
  loading.value = true
  try {
    const res: any = await docApi.list({ page: p, size, keyword: keyword.value, status: status.value || undefined })
    rows.value = res.records || []
    total.value = res.total || 0
  } finally { loading.value = false }
}

async function doUpload() {
  if (!file.value) { ElMessage.warning('请选择文件'); return }
  if (!upForm.categoryId) { ElMessage.warning('请选择分类'); return }
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file.value)
    fd.append('categoryId', String(upForm.categoryId))
    if (upForm.title) fd.append('title', upForm.title)
    if (upForm.docCode) fd.append('docCode', upForm.docCode)
    if (upForm.changeLog) fd.append('changeLog', upForm.changeLog)
    await docApi.upload(fd)
    ElMessage.success('上传成功，已进入解析队列')
    uploadVisible.value = false
    load(1)
  } finally { uploading.value = false }
}

function submit(row: any) {
  docApi.submit(row.docId ?? row.id).then(() => { ElMessage.success('已提交审核'); load(page.value) })
}

function openAudit(row: any) {
  auditRow.value = row
  auditRemark.value = ''
  auditVisible.value = true
}

async function audit(pass: boolean) {
  if (!pass && !auditRemark.value.trim()) { ElMessage.warning('驳回时必须填写审核意见'); return }
  await docApi.audit(auditRow.value.docId ?? auditRow.value.id, { pass, remark: auditRemark.value })
  ElMessage.success(pass ? '已通过并发布' : '已驳回')
  auditVisible.value = false
  load(page.value)
}

function offline(row: any) {
  ElMessageBox.prompt('请输入下线原因', '下线确认', { confirmButtonText: '确认下线', cancelButtonText: '取消' })
    .then(({ value }) => docApi.offline(row.docId ?? row.id, value || '管理员下线'))
    .then(() => { ElMessage.success('已下线'); load(page.value) })
    .catch(() => {})
}

function reindex(row: any) {
  docApi.reindex(row.docId ?? row.id).then(() => { ElMessage.success('已重建索引队列'); load(page.value) })
}

function remove(row: any) {
  ElMessageBox.confirm(`确认删除文档「${row.title}」？`, '删除确认', { type: 'warning' })
    .then(() => docApi.remove(row.docId ?? row.id))
    .then(() => { ElMessage.success('已删除'); load(page.value) })
    .catch(() => {})
}
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 14px; }
.pager { display: flex; justify-content: center; padding-top: 14px; }
.audit-info { margin-bottom: 14px; line-height: 1.8; }
</style>
