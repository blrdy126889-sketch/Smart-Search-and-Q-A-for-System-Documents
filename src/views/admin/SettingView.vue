<template>
  <div>
    <div class="page-card">
      <div class="page-title">🤖 模型服务配置</div>
      <el-form label-width="150px" style="max-width: 620px">
        <el-divider content-position="left">Embedding 向量服务</el-divider>
        <el-form-item label="服务地址">
          <el-input v-model="form.embeddingBaseUrl" placeholder="https://open.bigmodel.cn/api/paas/v4" />
        </el-form-item>
        <el-form-item label="模型名称">
          <el-input v-model="form.embeddingModel" placeholder="embedding-3" />
        </el-form-item>
        <el-form-item label="向量维度">
          <el-input-number v-model="form.embeddingDim" :min="256" :max="2048" :step="128" />
        </el-form-item>
        <el-divider content-position="left">LLM 问答服务</el-divider>
        <el-form-item label="服务地址">
          <el-input v-model="form.llmBaseUrl" placeholder="https://open.bigmodel.cn/api/paas/v4" />
        </el-form-item>
        <el-form-item label="模型名称">
          <el-input v-model="form.llmModel" placeholder="glm-4-flash" />
        </el-form-item>
        <el-form-item label="温度">
          <el-slider v-model="form.temperature" :min="0" :max="1" :step="0.1" style="width: 300px" />
        </el-form-item>
        <el-divider content-position="left">检索与切片参数</el-divider>
        <el-form-item label="切片大小（字）">
          <el-input-number v-model="form.chunkSize" :min="200" :max="1000" :step="50" />
        </el-form-item>
        <el-form-item label="切片重叠（字）">
          <el-input-number v-model="form.chunkOverlap" :min="0" :max="200" :step="10" />
        </el-form-item>
        <el-form-item label="问答 Top-K">
          <el-input-number v-model="form.topK" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="BM25 权重">
          <el-slider v-model="form.bm25Weight" :min="0" :max="1" :step="0.05" style="width: 300px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="save">保存配置</el-button>
          <el-button @click="reset">恢复默认</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'

const STORAGE_KEY = 'docqa_admin_settings'
const DEFAULTS = {
  embeddingBaseUrl: 'https://open.bigmodel.cn/api/paas/v4',
  embeddingModel: 'embedding-3',
  embeddingDim: 1024,
  llmBaseUrl: 'https://open.bigmodel.cn/api/paas/v4',
  llmModel: 'glm-4-flash',
  temperature: 0.3,
  chunkSize: 500,
  chunkOverlap: 80,
  topK: 6,
  bm25Weight: 0.4
}
const form = reactive({ ...DEFAULTS, ...loadSaved() })

function loadSaved() {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}') } catch { return {} }
}
function save() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(form))
  ElMessage.success('配置已保存（后端接口对接后全局生效）')
}
function reset() {
  Object.assign(form, DEFAULTS)
  ElMessage.info('已恢复默认值')
}
</script>
