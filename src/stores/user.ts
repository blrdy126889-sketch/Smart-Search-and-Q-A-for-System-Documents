import { defineStore } from 'pinia'

interface UserInfo {
  userId: number
  nickname: string
  username: string
  roles: string[]
  perms: string[]
}

const STORAGE_KEY = 'docqa_user'

export const useUserStore = defineStore('user', {
  state: () => {
    let saved: any = null
    try { saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null') } catch { saved = null }
    return {
      token: saved?.token || '',
      userId: saved?.userId || 0,
      nickname: saved?.nickname || '',
      username: saved?.username || '',
      roles: saved?.roles || [],
      perms: saved?.perms || []
    }
  },
  getters: {
    isAdmin: (s) => s.roles.includes('ADMIN'),
    canManage: (s) => s.roles.includes('ADMIN') || s.roles.includes('EDITOR') || s.roles.includes('AUDITOR')
  },
  actions: {
    setLogin(data: { token: string } & Partial<UserInfo>) {
      this.token = data.token
      this.userId = data.userId || 0
      this.nickname = data.nickname || ''
      this.username = data.username || ''
      this.roles = data.roles || []
      this.perms = data.perms || []
      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        token: this.token, userId: this.userId, nickname: this.nickname,
        username: this.username, roles: this.roles, perms: this.perms
      }))
    },
    hasPerm(code: string) {
      if (this.roles.includes('ADMIN')) return true
      return this.perms.includes(code)
    },
    logout() {
      this.token = ''; this.userId = 0; this.nickname = ''; this.username = ''
      this.roles = []; this.perms = []
      localStorage.removeItem(STORAGE_KEY)
    }
  }
})
