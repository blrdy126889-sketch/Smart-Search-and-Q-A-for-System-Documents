<template>
  <div class="doc-center">
    <aside class="cate-panel page-card">
      <div class="page-title" style="font-size:15px">📂 分类目录</div>
      <el-tree
        :data="tree" :props="{ label: 'categoryName', children: 'children' }"
        node-key="id" highlight-current default-expand-all
        @node-click="(n: any) => { activeCategory = n.id; loadDocs(1) }" />
    </aside>

    <section class="doc-list">
      <div class="page-card toolbar">
        <el-input v-model="keyword" placeholder="搜索文档标题/编号" clearable style="width: 260px" @keyup.enter="loadDocs(1)" />
        <el-button type="primary" @click="loadDocs(1)">查询</el-button>
        <el-button v-perm="'doc:upload'" type="success" plain @click="uploadVisible = true">⬆ 上传文档</el-button>
      </div>

      <div v-if="loading" class="page-card"><el-skeleton :rows="5" animated /></div>
      <template v-else>
        <div v-for="d in docs" :key="d.docId ?? d.id" class="page-card doc-card" @click="router.push(`/docs/${d.docId ?? d.id}`)">
          <div class="head">
            <div class="title">{{ d.title }}</div>
            <div class="meta">
              <el-tag size="small" :type="statusType(d.status)">{{ statusText(d.status) }}</el-tag>
              <span class="text-sub">{{ d.docCode }}</span>
            </div>
          </div>
          <div class="summary text-sub">{{ d.summary || '暂无摘要（入库后自动生成）' }}</div>
          <div class="foot text-sub">
            <span>👁 {{ d.viewCount ?? 0 }}</span>
            <span>💬 引用 {{ d.quoteCount ?? 0 }}</span>
            <span>⭐ {{ d.favoriteCount ?? 0 }}</span>
            <span>{{ d.sourceType }}</span>
            <span v-if="d.updatedAt">{{ d.updatedAt }}</span>
          </div>
        </div>
        <div v-if="!docs.length" class="page-card"><el-empty description="暂无文档" /></div>
        <div class="pager">
          <el-pagination background layout="prev, pager, next" :total="total" :page-size="size" :current-page="page" @current-change="loadDocs" />
        </div>
      </template>
    </section>

    <!-- 上传对话框 -->
    <el-dialog v-model="uploadVisible" title="上传制度文档" width="480px">
      <el-form label-width="90px">
        <el-form-item label="文档分类">
          <el-tree-select v-model="uploadForm.categoryId" :data="tree" check-strictly :props="{ label: 'categoryName', value: 'id' }" style="width: 100%" />
        </el-form-item>
        <el-form-item label="文档标题">
          <el-input v-model="uploadForm.title" placeholder="留空则取文件名" />
        </el-form-item>
        <el-form-item label="文档编号">
          <el-input v-model="uploadForm.docCode" placeholder="留空自动生成" />
        </el-form-item>
        <el-form-item label="变更说明">
          <el-input v-model="uploadForm.changeLog" placeholder="选填" />
        </el-form-item>
        <el-form-item label="选择文件">
          <el-upload
            drag :auto-upload="false" :limit="1" :on-change="(f: any) => (file = f.raw)"
            accept=".txt,.doc,.docx,.pdf">
            <div class="el-upload__text">拖拽文件到此处或 <em>点击选择</em></div>
            <template #tip>
              <div class="text-sub">支持 TXT / Word / PDF，单文件 ≤ 50MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="doUpload">开始上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { categoryApi, docApi } from '../api'

const router = useRouter()
const tree = ref<any[]>([])
const activeCategory = ref<number | undefined>(undefined)
const keyword = ref('')
const docs = ref<any[]>([])
const page = ref(1)
const size = 10
const total = ref(0)
const loading = ref(false)
const uploadVisible = ref(false)
const uploading = ref(false)
const file = ref<File | null>(null)
const uploadForm = reactive({ categoryId: undefined as number | undefined, title: '', docCode: '', changeLog: '' })

onMounted(() => {
  categoryApi.tree().then((t: any) => { tree.value = t || [] }).catch(() => {})
  loadDocs(1)
})

async function loadDocs(p = 1) {
  page.value = p
  loading.value = true
  try {
    const res: any = await docApi.list({ page: p, size, keyword: keyword.value, categoryId: activeCategory.value })
    docs.value = res.records || []
    total.value = res.total || 0
  } finally { loading.value = false }
}

async function doUpload() {
  if (!file.value) { ElMessage.warning('请选择文件'); return }
  if (!uploadForm.categoryId) { ElMessage.warning('请选择文档分类'); return }
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file.value)
    fd.append('categoryId', String(uploadForm.categoryId))
    if (uploadForm.title) fd.append('title', uploadForm.title)
    if (uploadForm.docCode) fd.append('docCode', uploadForm.docCode)
    if (uploadForm.changeLog) fd.append('changeLog', uploadForm.changeLog)
    await docApi.upload(fd)
    ElMessage.success('上传成功，已进入解析切片队列')
    uploadVisible.value = false
    file.value = null
    loadDocs(1)
  } finally { uploading.value = false }
}

function statusType(s: string) {
  return { PUBLISHED: 'success', PENDING_AUDIT: 'warning', REJECTED: 'danger', DRAFT: 'info', OFFLINE: 'info' }[s] || 'info'
}
function statusText(s: string) {
  return { PUBLISHED: '已发布', PENDING_AUDIT: '待审核', REJECTED: '已驳回', DRAFT: '草稿', OFFLINE: '已下线' }[s] || s
}
</script>

<style scoped>
.doc-center { display: flex; gap: 16px; }
.cate-panel { width: 240px; flex-shrink: 0; }
.doc-list { flex: 1; min-width: 0; }
.toolbar { display: flex; gap: 12px; }
.doc-card { cursor: pointer; transition: all .2s; }
.doc-card:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(79,110,247,.15); }
.doc-card .head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.doc-card .title { font-size: 15px; font-weight: 600; }
.doc-card .meta { display: flex; align-items: center; gap: 10px; }
.doc-card .summary { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 8px; }
.doc-card .foot { display: flex; gap: 16px; font-size: 12px; }
.pager { display: flex; justify-content: center; padding: 12px 0; }
</style>
