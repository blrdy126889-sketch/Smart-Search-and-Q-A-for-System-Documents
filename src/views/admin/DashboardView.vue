<template>
  <div>
    <div class="cards">
      <div v-for="c in cards" :key="c.label" class="page-card stat">
        <div class="num" :style="{ color: c.color }">{{ c.value }}</div>
        <div class="text-sub">{{ c.label }}</div>
      </div>
    </div>
    <el-row :gutter="16">
      <el-col :span="12">
        <div class="page-card">
          <div class="page-title">🔥 高频咨询问题 TOP10（近30天）</div>
          <div ref="hotQRef" style="height: 340px"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="page-card">
          <div class="page-title">📚 制度文档被引用热度 TOP10（近30天）</div>
          <div ref="docQRef" style="height: 340px"></div>
        </div>
      </el-col>
    </el-row>
    <div class="page-card">
      <div class="page-title">📈 文档上传/发布趋势（近6个月）</div>
      <div ref="trendRef" style="height: 300px"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { statsApi } from '../../api'

const hotQRef = ref<HTMLElement>()
const docQRef = ref<HTMLElement>()
const trendRef = ref<HTMLElement>()
const cards = ref([
  { label: '制度文档总数', value: '-', color: '#4f6ef7' },
  { label: '已发布文档', value: '-', color: '#22c55e' },
  { label: '今日问答次数', value: '-', color: '#f59e0b' },
  { label: '待审核文档', value: '-', color: '#ef4444' },
  { label: '索引失败数', value: '-', color: '#7a8699' }
])

onMounted(async () => {
  try {
    const ov: any = await statsApi.overview()
    cards.value[0].value = ov.docCount ?? 0
    cards.value[1].value = ov.publishedCount ?? 0
    cards.value[2].value = ov.todayQaCount ?? 0
    cards.value[3].value = ov.pendingAuditCount ?? 0
    cards.value[4].value = ov.failedIndexCount ?? 0
  } catch { /* ignore */ }

  renderHotQ()
  renderDocQ()
  renderTrend()
})

async function renderHotQ() {
  let data: any[] = []
  try { data = (await statsApi.hotQuestions(30, 10)) as any[] } catch { /* ignore */ }
  const chart = echarts.init(hotQRef.value!)
  const qs = data.map(d => (d.question || '').slice(0, 18))
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 10, right: 40, top: 10, bottom: 10, containLabel: true },
    xAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed' } } },
    yAxis: { type: 'category', data: qs.reverse(), axisLabel: { fontSize: 12 } },
    series: [{
      type: 'bar', data: data.map(d => d.count).reverse(), barMaxWidth: 18,
      itemStyle: { borderRadius: [0, 6, 6, 0], color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#4f6ef7' }, { offset: 1, color: '#7c9bff' }]) },
      label: { show: true, position: 'right', fontSize: 12, color: '#7a8699' }
    }]
  })
}

async function renderDocQ() {
  let data: any[] = []
  try { data = (await statsApi.docQuotes(30, 10)) as any[] } catch { /* ignore */ }
  const chart = echarts.init(docQRef.value!)
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 10, right: 40, top: 10, bottom: 10, containLabel: true },
    xAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed' } } },
    yAxis: { type: 'category', data: data.map(d => (d.docTitle || '').slice(0, 16)).reverse(), axisLabel: { fontSize: 12 } },
    series: [{
      type: 'bar', data: data.map(d => d.quoteCount).reverse(), barMaxWidth: 18,
      itemStyle: { borderRadius: [0, 6, 6, 0], color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#22c55e' }, { offset: 1, color: '#86efac' }]) },
      label: { show: true, position: 'right', fontSize: 12, color: '#7a8699' }
    }]
  })
}

async function renderTrend() {
  let data: any[] = []
  try { data = (await statsApi.uploadTrend(6)) as any[] } catch { /* ignore */ }
  const chart = echarts.init(trendRef.value!)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 10, right: 20, top: 20, bottom: 10, containLabel: true },
    xAxis: { type: 'category', data: data.map(d => d.month), boundaryGap: false },
    yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed' } } },
    series: [{
      type: 'line', data: data.map(d => d.count), smooth: true, symbolSize: 8,
      lineStyle: { width: 3, color: '#4f6ef7' }, itemStyle: { color: '#4f6ef7' },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(79,110,247,.25)' }, { offset: 1, color: 'rgba(79,110,247,.02)' }]) }
    }]
  })
}
</script>

<style scoped>
.cards { display: flex; gap: 14px; margin-bottom: 16px; }
.stat { flex: 1; text-align: center; }
.stat .num { font-size: 26px; font-weight: 700; }
</style>
