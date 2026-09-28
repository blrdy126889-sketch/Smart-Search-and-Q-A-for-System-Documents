import type { Directive } from 'vue'
import MarkdownIt from 'markdown-it'
import { useUserStore } from '../stores/user'

/** v-perm="'doc:upload'" —— 无权限则移除该元素 */
export const permDirective: Directive<HTMLElement, string> = {
  mounted(el, binding) {
    const userStore = useUserStore()
    if (binding.value && !userStore.hasPerm(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  }
}

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })

/** markdown 渲染 */
export function renderMarkdown(text: string): string {
  return md.render(text || '')
}

/**
 * 将答案中的 [1] [2] 引用标记替换为可交互脚注徽标
 */
export function renderAnswerWithRefs(html: string): string {
  return html.replace(/\[(\d{1,2})\]/g, '<span class="ref-mark" data-no="$1">$1</span>')
}
