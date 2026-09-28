<template>
  <div>
    <div class="page-card">
      <div class="toolbar">
        <span class="page-title" style="margin: 0">📊 智能数据统计分析</span>
        <el-select v-model="days" style="width: 140px" @change="loadAll">
          <el-option :value="7" label="近 7 天" />
          <el-option :value="30" label="近 30 天" />
          <el-option :value="90" label="近 90 天" />
        </el-select>
      </div>
      <el-row :gutter="16">
        <el-col :span="12"><div ref="qRef" style="height: 360px"></div></el-col>
        <el-col :span="12"><div ref="dRef" style="height: 360px"></div></el-col>
      </el-row>
    </div>
    <div class="page-card">
      <div class="page-title">📈 上传/发布趋势</div>
      <div ref="tRef" style="height: 300px"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { statsApi } from '../../api'

const days = ref(30)
const qRef = ref<HTMLElement>()
const dRef = ref<HTMLElement>()
const tRef = ref<HTMLElement>()
let qChart: echarts.ECharts | null = null
let dChart: echarts.ECharts | null = null
let tChart: echarts.ECharts | null = null

onMounted(() => {
  qChart = echarts.init(qRef.value!)
  dChart = echarts.init(dRef.value!)
  tChart = echarts.init(tRef.value!)
  loadAll()
})

async function loadAll() {
  try {
    const [qs, ds, tr]: any[] = await Promise.all([
      statsApi.hotQuestions(days.value, 10),
      statsApi.docQuotes(days.value, 10),
      statsApi.uploadTrend(6)
    ])
    qChart?.setOption({
      title: { text: `高频咨询问题 TOP10（近${days.value}天）`, left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 10, right: 40, top: 40, bottom: 10, containLabel: true },
      xAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed' } } },
      yAxis: { type: 'category', data: qs.map((x: any) => (x.question || '').slice(0, 18)).reverse() },
      series: [{ type: 'bar', data: qs.map((x: any) => x.count).reverse(), barMaxWidth: 16, itemStyle: { borderRadius: [0, 6, 6, 0], color: '#4f6ef7' }, label: { show: true, position: 'right' } }]
    })
    dChart?.setOption({
      title: { text: `文档被引用热度 TOP10（近${days.value}天）`, left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 10, right: 40, top: 40, bottom: 10, containLabel: true },
      xAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed' } } },
      yAxis: { type: 'category', data: ds.map((x: any) => (x.docTitle || '').slice(0, 14)).reverse() },
      series: [{ type: 'bar', data: ds.map((x: any) => x.quoteCount).reverse(), barMaxWidth: 16, itemStyle: { borderRadius: [0, 6, 6, 0], color: '#22c55e' }, label: { show: true, position: 'right' } }]
    })
    tChart?.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 10, right: 20, top: 20, bottom: 10, containLabel: true },
      xAxis: { type: 'category', data: tr.map((x: any) => x.month), boundaryGap: false },
      yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed' } } },
      series: [{ type: 'line', data: tr.map((x: any) => x.count), smooth: true, areaStyle: { opacity: .15 }, lineStyle: { width: 3, color: '#f59e0b' }, itemStyle: { color: '#f59e0b' } }]
    })
  } catch { /* ignore */ }
}
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
</style>
