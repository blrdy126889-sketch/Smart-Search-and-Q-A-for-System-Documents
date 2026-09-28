<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="用户名/昵称" clearable style="width: 200px" @keyup.enter="load(1)" />
      <el-button type="primary" @click="load(1)">查询</el-button>
      <el-button type="success" plain @click="openCreate">＋ 新增用户</el-button>
    </div>
    <el-table :data="rows" v-loading="loading">
      <el-table-column prop="username" label="用户名" width="130" />
      <el-table-column prop="nickname" label="昵称" width="130" />
      <el-table-column label="角色" min-width="180">
        <template #default="{ row }">
          <el-tag v-for="r in row.roles" :key="r" size="small" style="margin-right: 6px" :type="r === 'ADMIN' ? 'danger' : r === 'AUDITOR' ? 'warning' : r === 'EDITOR' ? 'success' : 'info'">
            {{ r }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" min-width="160" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastLoginAt" label="最近登录" width="160" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="openRoles(row)">分配角色</el-button>
          <el-button size="small" text @click="toggleStatus(row)">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
          <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager">
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="size" :current-page="page" @current-change="load" />
    </div>

    <el-dialog v-model="dlgVisible" :title="dlgTitle" width="420px">
      <el-form label-width="80px">
        <el-form-item label="用户名"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="昵称"><el-input v-model="form.nickname" /></el-form-item>
        <el-form-item v-if="!form.id" label="密码"><el-input v-model="form.password" type="password" show-password /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlgVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleVisible" title="分配角色" width="400px">
      <el-checkbox-group v-model="roleIds">
        <el-checkbox v-for="r in roles" :key="r.id" :value="r.id">{{ r.roleName }}（{{ r.roleCode }}）</el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { sysApi } from '../../api'

const keyword = ref('')
const rows = ref<any[]>([])
const page = ref(1)
const size = 10
const total = ref(0)
const loading = ref(false)
const roles = ref<any[]>([])
const dlgVisible = ref(false)
const dlgTitle = ref('')
const form = reactive({ id: 0, username: '', nickname: '', password: '', email: '' })
const roleVisible = ref(false)
const roleIds = ref<number[]>([])
const roleTarget = ref<any>(null)

onMounted(() => {
  load(1)
  sysApi.roles().then((r: any) => { roles.value = r || [] }).catch(() => {})
})

async function load(p = 1) {
  page.value = p
  loading.value = true
  try {
    const res: any = await sysApi.users({ page: p, size, keyword: keyword.value })
    rows.value = res.records || []
    total.value = res.total || 0
  } finally { loading.value = false }
}

function openCreate() {
  dlgTitle.value = '新增用户'
  Object.assign(form, { id: 0, username: '', nickname: '', password: '', email: '' })
  dlgVisible.value = true
}

async function save() {
  if (!form.username || (!form.id && !form.password)) { ElMessage.warning('请填写用户名和密码'); return }
  if (form.id) await sysApi.updateUser(form.id, { nickname: form.nickname, email: form.email })
  else await sysApi.createUser(form)
  ElMessage.success('保存成功')
  dlgVisible.value = false
  load(page.value)
}

function openRoles(row: any) {
  roleTarget.value = row
  roleIds.value = (row.roleIds || []) as number[]
  roleVisible.value = true
}

async function saveRoles() {
  await sysApi.assignRoles(roleTarget.value.id, roleIds.value)
  ElMessage.success('角色已更新')
  roleVisible.value = false
  load(page.value)
}

async function toggleStatus(row: any) {
  await sysApi.updateUser(row.id, { status: row.status === 1 ? 0 : 1 })
  load(page.value)
}

function remove(row: any) {
  ElMessageBox.confirm(`确认删除用户「${row.username}」？`, '删除确认', { type: 'warning' })
    .then(() => sysApi.deleteUser(row.id))
    .then(() => { ElMessage.success('已删除'); load(page.value) })
    .catch(() => {})
}
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 14px; }
.pager { display: flex; justify-content: center; padding-top: 14px; }
</style>
