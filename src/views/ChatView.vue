<template>
  <div class="chat-page">
    <!-- 左侧会话列表 -->
    <aside class="session-panel">
      <el-button type="primary" class="new-chat" @click="newSession">＋ 新的对话</el-button>
      <div class="session-list">
        <div
          v-for="s in sessions" :key="s.sessionId"
          class="session-item" :class="{ active: s.sessionId === currentSessionId }"
          @click="switchSession(s.sessionId)">
          <span class="title">{{ s.title || '新对话' }}</span>
          <el-icon class="del" @click.stop="removeSession(s.sessionId)"><Delete /></el-icon>
        </div>
      </div>
    </aside>

    <!-- 中间消息区 -->
    <section class="chat-main">
      <div class="messages" ref="messagesRef">
        <template v-if="messages.length">
          <div v-for="(m, i) in messages" :key="i" class="chat-row" :class="m.role">
            <div class="chat-avatar" :class="m.role">{{ m.role === 'user' ? '我' : 'AI' }}</div>
            <div class="bubble-wrap">
              <div class="chat-bubble" :class="[m.role, { error: m.error }]">
                <template v-if="m.role === 'assistant'">
                  <div v-if="m.content" class="md-body" :class="{ 'typing-cursor': m.streaming }" v-html="renderAi(m)"></div>
                  <div v-else-if="m.streaming" class="text-sub typing-cursor">正在检索知识库并思考</div>
                </template>
                <template v-else>{{ m.content }}</template>
              </div>
              <div v-if="m.role === 'assistant' && !m.streaming && m.qaId && m.qaId > 0" class="msg-actions">
                <el-button :type="m.feedback === 1 ? 'primary' : 'default'" size="small" text :class="{ liked: m.feedback === 1 }" @click="sendFeedback(m, 1)">👍 有用</el-button>
                <el-button :type="m.feedback === -1 ? 'danger' : 'default'" size="small" text :class="{ disliked: m.feedback === -1 }" @click="sendFeedback(m, -1)">👎 无用</el-button>
                <span v-if="m.sources?.length" class="text-sub">{{ m.sources.length }} 条来源</span>
              </div>
            </div>
          </div>
        </template>
        <div v-else class="welcome">
          <div class="big">💬</div>
          <h2>制度文档智能问答</h2>
          <p class="text-sub">答案基于已发布的制度文档生成，并自动标注来源</p>
          <div class="suggest">
            <div v-for="s in suggestList" :key="s" class="suggest-item" @click="quickAsk(s)">{{ s }}</div>
          </div>
        </div>
      </div>

      <div class="input-area">
        <el-input
          v-model="input" :disabled="sse.streaming" size="large"
          placeholder="请输入您的问题，Enter 发送" @keyup.enter="send" />
        <el-button v-if="!sse.streaming" type="primary" size="large" @click="send">发送</el-button>
        <el-button v-else type="danger" size="large" plain @click="sse.abort()">停止生成</el-button>
      </div>
    </section>

    <!-- 右侧来源面板 -->
    <aside class="source-panel" v-if="activeSources.length">
      <div class="panel-title">📎 答案来源（点击跳转文档）</div>
      <div class="scroll">
        <div
          v-for="s in activeSources" :key="s.no" class="source-card"
          :class="{ hl: hoverNo === s.no }" @mouseenter="hoverNo = s.no" @mouseleave="hoverNo = -1"
          @click="router.push(`/docs/${s.docId}`)">
          <div class="head">
            <span class="no">{{ s.no }}</span>
            <span class="doc-title">{{ s.docTitle }}</span>
            <el-tag size="small" effect="plain">{{ s.versionNo }}</el-tag>
          </div>
          <div class="text-sub path">{{ s.headingPath }}</div>
          <div class="snippet">{{ s.snippet }}</div>
          <div class="text-sub score">相关度 {{ (s.score * 100).toFixed(1) }}%</div>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { qaApi } from '../api'
import { useSseChat, type ChatMessageVO, type QaSource } from '../composables/useSseChat'
import { renderMarkdown, renderAnswerWithRefs } from '../utils/perm'

const route = useRoute()
const router = useRouter()
const sse = useSseChat()

const sessions = ref<any[]>([])
const currentSessionId = ref<number>(0)
const messages = ref<ChatMessageVO[]>([])
const input = ref('')
const messagesRef = ref<HTMLElement>()
const hoverNo = ref(-1)
const suggestList = [
  '差旅费的报销标准是什么？',
  '年假有多少天？怎么申请？',
  '采购流程需要哪些审批环节？',
  '新员工试用期多长时间？'
]

const activeSources = ref<QaSource[]>([])

onMounted(async () => {
  await loadSessions()
  const q = route.query.q as string
  if (q) { input.value = q; await send() }
})

async function loadSessions() {
  try {
    sessions.value = (await qaApi.sessions()) as any[]
    if (sessions.value.length && !currentSessionId.value) {
      await switchSession(sessions.value[0].sessionId)
    }
  } catch { /* ignore */ }
}

async function newSession() {
  const s: any = await qaApi.createSession()
  sessions.value.unshift(s)
  currentSessionId.value = s.sessionId
  messages.value = []
  activeSources.value = []
}

async function switchSession(id: number) {
  currentSessionId.value = id
  messages.value = []
  activeSources.value = []
  try {
    const list: any = await qaApi.messages(id)
    messages.value = (list || []).map((m: any) => ({
      role: m.role, content: m.content, sources: m.sources || [], qaId: m.qaId, feedback: m.feedback || 0
    }))
    const last = [...messages.value].reverse().find(m => m.role === 'assistant' && m.sources?.length)
    activeSources.value = last?.sources || []
    scrollToBottom()
  } catch { /* ignore */ }
}

async function removeSession(id: number) {
  await qaApi.deleteSession(id)
  sessions.value = sessions.value.filter(s => s.sessionId !== id)
  if (currentSessionId.value === id) {
    currentSessionId.value = 0
    messages.value = []
    activeSources.value = []
    if (sessions.value.length) switchSession(sessions.value[0].sessionId)
  }
}

function quickAsk(q: string) { input.value = q; send() }

async function send() {
  const question = input.value.trim()
  if (!question || sse.streaming.value) return
  if (!currentSessionId.value) await newSession()
  input.value = ''
  messages.value.push({ role: 'user', content: question })
  const aiMsg: ChatMessageVO = { role: 'assistant', content: '', streaming: true }
  messages.value.push(aiMsg)
  activeSources.value = []
  scrollToBottom()

  await sse.ask(question, currentSessionId.value, {
    onMeta: (sources) => {
      aiMsg.sources = sources
      activeSources.value = sources
      scrollToBottom()
    },
    onDelta: (text) => {
      aiMsg.content += text
      scrollToBottom()
    },
    onDone: (qaId) => {
      aiMsg.streaming = false
      aiMsg.qaId = qaId
      // 若被中断（qaId=-1）保留已生成内容
      scrollToBottom()
    },
    onError: (msg) => {
      aiMsg.streaming = false
      aiMsg.error = true
      aiMsg.content = aiMsg.content || `生成失败：${msg}`
      scrollToBottom()
    }
  })
}

async function sendFeedback(m: ChatMessageVO, fb: number) {
  if (!m.qaId || m.qaId <= 0) return
  m.feedback = m.feedback === fb ? 0 : fb
  if (m.feedback !== 0) {
    try { await qaApi.feedback(m.qaId, m.feedback) } catch { /* ignore */ }
  }
}

/** AI 消息渲染：markdown + [n] 引用转脚注 */
function renderAi(m: ChatMessageVO): string {
  if (m.error) return m.content
  return renderAnswerWithRefs(renderMarkdown(m.content))
}

function scrollToBottom() {
  nextTick(() => {
    const el = messagesRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}
</script>

<style scoped>
.chat-page { display: flex; gap: 14px; height: calc(100vh - 110px); }
.session-panel { width: 240px; background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-lg); padding: 14px; box-shadow: var(--shadow-sm); display: flex; flex-direction: column; }
.new-chat { width: 100%; margin-bottom: 12px; }
.session-list { flex: 1; overflow-y: auto; }
.session-item { display: flex; align-items: center; gap: 8px; padding: 10px 12px; border-radius: 10px; cursor: pointer; font-size: 13px; color: #4e5969; transition: all .22s var(--ease); }
.session-item:hover { background: var(--primary-light); }
.session-item.active { background: var(--primary-light); color: var(--primary); font-weight: 600; }
.session-item .title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.session-item .del { visibility: hidden; color: var(--danger); }
.session-item:hover .del { visibility: visible; }

.chat-main { flex: 1; display: flex; flex-direction: column; background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); overflow: hidden; }
.messages { flex: 1; overflow-y: auto; padding: 26px 24px; background: linear-gradient(180deg, #fafbff 0%, #f6f7fb 100%); }
.welcome { height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.welcome .big { font-size: 46px; margin-bottom: 12px; }
.welcome h2 { font-size: 22px; font-weight: 700; }
.suggest { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 22px; max-width: 500px; justify-content: center; }
.suggest-item { padding: 8px 18px; background: var(--bg-card); border: 1px solid #dbe1fe; color: var(--primary); border-radius: 20px; font-size: 13px; cursor: pointer; transition: all .22s var(--ease); }
.suggest-item:hover { background: var(--primary-grad); color: #fff; border-color: transparent; box-shadow: 0 4px 12px rgba(59,91,253,.24); transform: translateY(-1px); }
.input-area { display: flex; gap: 12px; padding: 16px 18px; border-top: 1px solid var(--border-light); background: var(--bg-card); }
.input-area .el-input :deep(.el-input__wrapper) { padding: 8px 16px; }
.msg-actions { margin-top: 6px; display: flex; gap: 4px; align-items: center; }
.msg-actions :deep(.el-button.liked) { color: var(--primary); }
.msg-actions :deep(.el-button.disliked) { color: var(--danger); }

.source-panel { width: 304px; background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); padding: 14px; display: flex; flex-direction: column; }
.panel-title { font-size: 14px; font-weight: 600; margin-bottom: 12px; }
.source-panel .scroll { flex: 1; overflow-y: auto; }
</style>
