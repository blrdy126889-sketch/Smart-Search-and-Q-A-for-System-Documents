<template>
  <div class="page-card">
    <div class="toolbar">
      <span class="page-title" style="margin: 0">📂 分类目录管理</span>
      <el-button type="primary" size="small" @click="openCreate(0)">＋ 新建根分类</el-button>
      <span class="text-sub tip">支持拖拽调整层级；删除前须确保分类下无文档与子分类</span>
    </div>
    <el-tree
      :data="tree" :props="{ label: 'categoryName', children: 'children' }" node-key="id"
      default-expand-all draggable
      :allow-drop="(a: any, b: any, t: string) => t !== 'prev' && t !== 'next'"
      @node-drop="onDrop">
      <template #default="{ data }">
        <div class="tree-node">
          <span>{{ data.categoryName }}</span>
          <span class="text-sub count">{{ data.docCount ?? 0 }} 篇</span>
          <span class="ops">
            <el-button size="small" text type="primary" @click.stop="openCreate(data.id)">加子级</el-button>
            <el-button size="small" text @click.stop="openEdit(data)">编辑</el-button>
            <el-button size="small" text type="warning" @click.stop="openPerm(data)">授权</el-button>
            <el-button size="small" text type="danger" @click.stop="remove(data)">删除</el-button>
          </span>
        </div>
      </template>
    </el-tree>

    <el-dialog v-model="dlgVisible" :title="dlgTitle" width="420px">
      <el-form label-width="80px">
        <el-form-item label="名称"><el-input v-model="dlgForm.categoryName" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="dlgForm.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlgVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permVisible" title="分类数据授权（可见角色）" width="420px">
      <el-checkbox-group v-model="permRoleIds">
        <el-checkbox v-for="r in roles" :key="r.id" :value="r.id">{{ r.roleName }}（{{ r.roleCode }}）</el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" @click="savePerm">保存授权</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryApi, sysApi } from '../../api'

const tree = ref<any[]>([])
const roles = ref<any[]>([])
const dlgVisible = ref(false)
const dlgTitle = ref('')
const dlgForm = reactive({ id: 0, parentId: 0, categoryName: '', sortOrder: 0 })
const permVisible = ref(false)
const permRoleIds = ref<number[]>([])
const permTarget = ref<any>(null)

onMounted(async () => {
  await load()
  try { roles.value = (await sysApi.roles()) as any[] } catch { /* */ }
})

async function load() {
  tree.value = (await categoryApi.tree()) as any[]
}

function openCreate(parentId: number) {
  dlgTitle.value = parentId ? '新建子分类' : '新建根分类'
  Object.assign(dlgForm, { id: 0, parentId, categoryName: '', sortOrder: 0 })
  dlgVisible.value = true
}

function openEdit(data: any) {
  dlgTitle.value = '编辑分类'
  Object.assign(dlgForm, { id: data.id, parentId: data.parentId, categoryName: data.categoryName, sortOrder: data.sortOrder })
  dlgVisible.value = true
}

async function save() {
  if (!dlgForm.categoryName.trim()) { ElMessage.warning('请输入分类名称'); return }
  if (dlgForm.id) await categoryApi.update(dlgForm.id, { categoryName: dlgForm.categoryName, sortOrder: dlgForm.sortOrder })
  else await categoryApi.create({ parentId: dlgForm.parentId, categoryName: dlgForm.categoryName, sortOrder: dlgForm.sortOrder })
  ElMessage.success('保存成功')
  dlgVisible.value = false
  load()
}

async function onDrop(dragNode: any, dropNode: any) {
  await categoryApi.update(dragNode.data.id, { parentId: dropNode.data.id })
  ElMessage.success('已调整层级')
  load()
}

function openPerm(data: any) {
  permTarget.value = data
  permRoleIds.value = data.authorizedRoleIds || []
  permVisible.value = true
}

async function savePerm() {
  await categoryApi.assignPerms(permTarget.value.id, permRoleIds.value)
  ElMessage.success('授权已保存')
  permVisible.value = false
  load()
}

function remove(data: any) {
  ElMessageBox.confirm(`确认删除分类「${data.categoryName}」？`, '删除确认', { type: 'warning' })
    .then(() => categoryApi.remove(data.id))
    .then(() => { ElMessage.success('已删除'); load() })
    .catch((e) => { if (e?.message) load() })
}
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 14px; margin-bottom: 14px; }
.tip { font-size: 12px; }
.tree-node { flex: 1; display: flex; align-items: center; gap: 10px; padding: 4px 0; }
.tree-node .count { font-size: 12px; }
.tree-node .ops { margin-left: auto; opacity: 0; transition: opacity .2s; }
.tree-node:hover .ops { opacity: 1; }
</style>
