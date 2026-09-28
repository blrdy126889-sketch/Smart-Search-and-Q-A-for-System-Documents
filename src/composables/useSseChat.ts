/**
 * SSE 流式问答 composable
 * 使用 fetch + ReadableStream 而非 EventSource（需支持 POST + Authorization header）
 * 事件协议：meta(来源列表) → delta(增量文本) → done(qaId) / error；心跳为 `: ping` 注释行
 */
import { ref, onUnmounted } from 'vue'

export interface QaSource {
  no: number
  docId: number
  docTitle: string
  versionNo: string
  headingPath: string
  score: number
  snippet: string
}

export interface ChatMessageVO {
  role: 'user' | 'assistant'
  content: string
  sources?: QaSource[]
  qaId?: number
  streaming?: boolean
  error?: boolean
  feedback?: number
}

export function useSseChat() {
  const streaming = ref(false)
  let abortController: AbortController | null = null
  let typewriterTimer: number | null = null

  function getToken(): string {
    try { return JSON.parse(localStorage.getItem('docqa_user') || '{}').token || '' } catch { return '' }
  }

  function stopTimer() {
    if (typewriterTimer !== null) { clearInterval(typewriterTimer); typewriterTimer = null }
  }

  /**
   * 发起 SSE 流式问答
   * @param question 用户问题
   * @param sessionId 会话 ID
   * @param hooks 回调：onMeta(来源)、onDelta(新增文本)、onDone、onError
   */
  async function ask(
    question: string,
    sessionId: number,
    hooks: {
      onMeta?: (sources: QaSource[]) => void
      onDelta?: (text: string) => void
      onDone?: (qaId: number, firstTokenMs: number) => void
      onError?: (msg: string) => void
    }
  ) {
    if (streaming.value) return
    streaming.value = true
    abortController = new AbortController()

    let frameBuf = ''   // SSE 帧缓冲（帧可能跨网络 chunk）
    let pending = ''    // 打字机待输出缓冲
    let finished = false
    const startTime = Date.now()
    let firstTokenMs = 0

    const finish = (qaId: number) => {
      if (finished) return
      finished = true
      // 等待打字机排空缓冲后再结束
      const drain = () => {
        if (pending !== '') { setTimeout(drain, 16); return }
        stopTimer()
        streaming.value = false
        abortController = null
        hooks.onDone?.(qaId, firstTokenMs)
      }
      drain()
    }

    const fail = (msg: string) => {
      if (finished) return
      finished = true
      stopTimer()
      streaming.value = false
      abortController = null
      hooks.onError?.(msg)
    }

    // 打字机：16ms 一帧，每帧从 pending 取 2~4 字符（随积压量自适应速度）
    typewriterTimer = window.setInterval(() => {
      if (pending === '') return
      const n = pending.length > 120 ? 4 : pending.length > 40 ? 3 : 2
      const piece = pending.slice(0, n)
      pending = pending.slice(n)
      hooks.onDelta?.(piece)
    }, 16)

    try {
      const resp = await fetch('/api/v1/qa/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${getToken()}` },
        body: JSON.stringify({ sessionId, question }),
        signal: abortController.signal
      })
      if (!resp.ok || !resp.body) throw new Error(`HTTP ${resp.status}`)

      const reader = resp.body.getReader()
      const decoder = new TextDecoder()

      for (;;) {
        const { value, done: readerDone } = await reader.read()
        if (readerDone) break
        frameBuf += decoder.decode(value, { stream: true })
        // SSE 帧以空行（\n\n）分隔
        const frames = frameBuf.split('\n\n')
        frameBuf = frames.pop() || ''
        for (const frame of frames) {
          if (!frame.trim() || frame.startsWith(':')) continue // 跳过心跳注释行
          let event = 'message'
          let data = ''
          for (const line of frame.split('\n')) {
            if (line.startsWith('event:')) event = line.slice(6).trim()
            else if (line.startsWith('data:')) data += line.slice(5).trim()
          }
          if (!data) continue
          let payload: any
          try { payload = JSON.parse(data) } catch { continue }
          if (event === 'meta') {
            hooks.onMeta?.(payload.sources || [])
          } else if (event === 'delta') {
            if (!firstTokenMs) firstTokenMs = Date.now() - startTime
            pending += payload.content || ''
          } else if (event === 'done') {
            finish(payload.qaId ?? -1)
            return
          } else if (event === 'error') {
            fail(payload.msg || '生成失败')
            return
          }
        }
      }
      // 流自然关闭但未收到 done 事件：也正常收尾
      finish(-1)
    } catch (e: any) {
      if (e?.name === 'AbortError') {
        // 用户主动停止：立即排空并结束
        finished = true
        const drain = () => {
          if (pending !== '') { pending = ''; }
          stopTimer()
          streaming.value = false
          abortController = null
          hooks.onDone?.(-1, firstTokenMs)
        }
        drain()
      } else {
        fail(e?.message || '网络异常，请重试')
      }
    }
  }

  /** 停止生成 */
  function abort() {
    abortController?.abort()
  }

  onUnmounted(() => abort())

  return { streaming, ask, abort }
}
