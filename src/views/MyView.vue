<template>
  <div class="my-page">
    <div class="page-card">
      <el-tabs v-model="tab">
        <el-tab-pane label="⭐ 我的收藏" name="fav">
          <div v-for="f in favorites" :key="f.id" class="row" @click="router.push(`/docs/${f.docId}`)">
            <span class="t">{{ f.docTitle }}</span>
            <span class="text-sub">{{ f.createdAt }}</span>
          </div>
          <el-empty v-if="!favorites.length" description="暂无收藏" />
        </el-tab-pane>
        <el-tab-pane label="🔔 我的订阅" name="sub">
          <div v-for="s in subscriptions" :key="s.id" class="row">
            <span class="t">{{ s.subType === 'DOC' ? '📄 ' + s.targetName : '📂 ' + s.targetName }}</span>
            <el-button size="small" text type="danger" @click.stop=" unsub(s)">退订</el-button>
          </div>
          <el-empty v-if="!subscriptions.length" description="暂无订阅" />
        </el-tab-pane>
        <el-tab-pane :label="`🔔 通知${unread ? '(' + unread + ')' : ''}`" name="notify">
          <div v-for="n in notifies" :key="n.id" class="row" :class="{ unread: !n.isRead }">
            <span class="t" @click="router.push(`/docs/${n.docId}`)">{{ n.title }}</span>
            <span class="text-sub">{{ n.content }}</span>
            <el-button v-if="!n.isRead" size="small" text type="primary" @click.stop="read(n)">标为已读</el-button>
          </div>
          <el-empty v-if="!notifies.length" description="暂无通知" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { docApi } from '../api'

const router = useRouter()
const tab = ref('fav')
const favorites = ref<any[]>([])
const subscriptions = ref<any[]>([])
const notifies = ref<any[]>([])
const unread = computed(() => notifies.value.filter(n => !n.isRead).length)

onMounted(loadAll)

async function loadAll() {
  try { favorites.value = ((await docApi.favorites({ page: 1, size: 50 })) as any).records || [] } catch { /* */ }
  try { subscriptions.value = (await docApi.subscriptions()) as any[] } catch { /* */ }
  try { notifies.value = ((await docApi.notifies({ page: 1, size: 50 })) as any).records || [] } catch { /* */ }
}

async function unsub(s: any) {
  await docApi.unsubscribe(s.id)
  ElMessage.success('已退订')
  loadAll()
}

async function read(n: any) {
  await docApi.readNotify(n.id)
  n.isRead = true
}
</script>

<style scoped>
.my-page { max-width: 860px; margin: 0 auto; }
.row { display: flex; align-items: center; gap: 14px; padding: 12px 8px; border-bottom: 1px solid #f2f4f8; cursor: pointer; }
.row .t { font-size: 14px; flex: 1; }
.row.unread .t { font-weight: 600; color: var(--primary); }
</style>
