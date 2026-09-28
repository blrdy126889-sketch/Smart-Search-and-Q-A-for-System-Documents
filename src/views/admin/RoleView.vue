<template>
  <div class="page-card">
    <div class="toolbar">
      <span class="page-title" style="margin: 0">🔑 角色与权限管理</span>
      <el-button type="primary" size="small" @click="openCreate">＋ 新增角色</el-button>
    </div>
    <el-table :data="roles">
      <el-table-column prop="roleName" label="角色名称" width="140" />
      <el-table-column prop="roleCode" label="角色编码" width="120" />
      <el-table-column prop="roleDesc" label="描述" min-width="200" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="openPerm(row)">权限配置</el-button>
          <el-button size="small" text @click="openEdit(row)">编辑</el-button>
          <el-button size="small" text type="danger" :disabled="row.roleCode === 'ADMIN'" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dlgVisible" :title="form.id ? '编辑角色' : '新增角色'" width="420px">
      <el-form label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="form.roleCode" :disabled="form.roleCode === 'ADMIN' || form.roleCode === 'USER'" placeholder="如 MANAGER" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.roleDesc" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlgVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permVisible" :title="`权限配置 - ${permTarget?.roleName}`" width="440px">
      <el-tree
        ref="permTreeRef" :data="permTree" show-checkbox node-key="id"
        :props="{ label: 'permName', children: 'children' }" default-expand-all
        :default-checked-keys="checkedPermIds" />
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" @click="savePerms">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { sysApi } from '../../api'

const roles = ref<any[]>([])
const permTree = ref<any[]>([])
const dlgVisible = ref(false)
const form = reactive({ id: 0, roleName: '', roleCode: '', roleDesc: '' })
const permVisible = ref(false)
const permTarget = ref<any>(null)
const checkedPermIds = ref<number[]>([])
const permTreeRef = ref<any>()

onMounted(load)

async function load() {
  roles.value = (await sysApi.roles()) as any[]
}

function openCreate() {
  Object.assign(form, { id: 0, roleName: '', roleCode: '', roleDesc: '' })
  dlgVisible.value = true
}

function openEdit(row: any) {
  Object.assign(form, { id: row.id, roleName: row.roleName, roleCode: row.roleCode, roleDesc: row.roleDesc })
  dlgVisible.value = true
}

async function save() {
  if (!form.roleName) { ElMessage.warning('请输入角色名称'); return }
  if (form.id) await sysApi.updateRole(form.id, form)
  else await sysApi.createRole(form)
  ElMessage.success('保存成功')
  dlgVisible.value = false
  load()
}

async function openPerm(row: any) {
  permTarget.value = row
  permTree.value = (await sysApi.permTree()) as any[]
  checkedPermIds.value = row.permIds || []
  permVisible.value = true
}

async function savePerms() {
  const ids = permTreeRef.value?.getCheckedKeys(false) || []
  const half = permTreeRef.value?.getHalfCheckedKeys() || []
  await sysApi.assignPerms(permTarget.value.id, [...ids, ...half])
  ElMessage.success('权限已保存')
  permVisible.value = false
  load()
}

function remove(row: any) {
  ElMessageBox.confirm(`确认删除角色「${row.roleName}」？`, '删除确认', { type: 'warning' })
    .then(() => sysApi.deleteRole(row.id))
    .then(() => { ElMessage.success('已删除'); load() })
    .catch(() => {})
}
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
</style>
