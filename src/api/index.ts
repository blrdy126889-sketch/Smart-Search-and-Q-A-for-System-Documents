import request from './request'

/* ---------------- Auth ---------------- */
export const authApi = {
  login: (data: { username: string; password: string }) => request.post('/api/v1/auth/login', data),
  logout: () => request.post('/api/v1/auth/logout'),
  me: () => request.get('/api/v1/auth/me'),
  changePassword: (data: { oldPassword: string; newPassword: string }) => request.put('/api/v1/auth/password', data)
}

/* ---------------- System ---------------- */
export const sysApi = {
  users: (params: any) => request.get('/api/v1/users', { params }),
  createUser: (data: any) => request.post('/api/v1/users', data),
  updateUser: (id: number, data: any) => request.put(`/api/v1/users/${id}`, data),
  deleteUser: (id: number) => request.delete(`/api/v1/users/${id}`),
  assignRoles: (id: number, roleIds: number[]) => request.post(`/api/v1/users/${id}/roles`, { roleIds }),
  roles: () => request.get('/api/v1/roles'),
  createRole: (data: any) => request.post('/api/v1/roles', data),
  updateRole: (id: number, data: any) => request.put(`/api/v1/roles/${id}`, data),
  deleteRole: (id: number) => request.delete(`/api/v1/roles/${id}`),
  assignPerms: (id: number, permIds: number[]) => request.put(`/api/v1/roles/${id}/permissions`, { permIds }),
  permTree: () => request.get('/api/v1/permissions/tree'),
  opLogs: (params: any) => request.get('/api/v1/logs/operations', { params }),
  qaLogs: (params: any) => request.get('/api/v1/logs/qa', { params }),
  accessLogs: (params: any) => request.get('/api/v1/logs/access', { params })
}

/* ---------------- Category ---------------- */
export const categoryApi = {
  tree: () => request.get('/api/v1/categories/tree'),
  create: (data: any) => request.post('/api/v1/categories', data),
  update: (id: number, data: any) => request.put(`/api/v1/categories/${id}`, data),
  remove: (id: number) => request.delete(`/api/v1/categories/${id}`),
  assignPerms: (id: number, roleIds: number[]) => request.put(`/api/v1/categories/${id}/perms`, { roleIds })
}

/* ---------------- Document ---------------- */
export const docApi = {
  upload: (fd: FormData, onProgress?: (p: number) => void) =>
    request.post('/api/v1/documents/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => onProgress && onProgress(Math.round((e.loaded / (e.total || 1)) * 100))
    }),
  list: (params: any) => request.get('/api/v1/documents', { params }),
  detail: (id: number) => request.get(`/api/v1/documents/${id}`),
  update: (id: number, data: any) => request.put(`/api/v1/documents/${id}`, data),
  remove: (id: number) => request.delete(`/api/v1/documents/${id}`),
  submit: (id: number) => request.post(`/api/v1/documents/${id}/submit`),
  audit: (id: number, data: { pass: boolean; remark: string }) => request.post(`/api/v1/documents/${id}/audit`, data),
  offline: (id: number, reason: string) => request.post(`/api/v1/documents/${id}/offline`, { reason }),
  reindex: (id: number) => request.post(`/api/v1/documents/${id}/reindex`),
  uploadVersion: (id: number, fd: FormData) => request.post(`/api/v1/documents/${id}/versions`, fd, { headers: { 'Content-Type': 'multipart/form-data' } }),
  publishVersion: (versionId: number) => request.put(`/api/v1/versions/${versionId}/publish`),
  diff: (id: number, fromVersionId: number, toVersionId: number) =>
    request.get(`/api/v1/documents/${id}/diff`, { params: { fromVersionId, toVersionId } }),
  favorite: (id: number) => request.post(`/api/v1/documents/${id}/favorite`),
  unfavorite: (id: number) => request.delete(`/api/v1/documents/${id}/favorite`),
  favorites: (params: any) => request.get('/api/v1/favorites', { params }),
  subscribe: (data: { subType: string; targetId: number }) => request.post('/api/v1/subscriptions', data),
  unsubscribe: (id: number) => request.delete(`/api/v1/subscriptions/${id}`),
  subscriptions: () => request.get('/api/v1/subscriptions'),
  notifies: (params: any) => request.get('/api/v1/notifies', { params }),
  readNotify: (id: number) => request.put(`/api/v1/notifies/${id}/read`)
}

/* ---------------- Search ---------------- */
export const searchApi = {
  search: (params: { q: string; mode?: string; categoryId?: number; page?: number; size?: number }) =>
    request.get('/api/v1/search', { params }),
  suggest: (q: string) => request.get('/api/v1/search/suggest', { params: { q } })
}

/* ---------------- QA ---------------- */
export const qaApi = {
  createSession: (title?: string) => request.post('/api/v1/qa/sessions', { title }),
  sessions: () => request.get('/api/v1/qa/sessions'),
  deleteSession: (id: number) => request.delete(`/api/v1/qa/sessions/${id}`),
  messages: (id: number) => request.get(`/api/v1/qa/sessions/${id}/messages`),
  feedback: (qaId: number, feedback: number) => request.post(`/api/v1/qa/${qaId}/feedback`, { feedback })
}

/* ---------------- Stats ---------------- */
export const statsApi = {
  overview: () => request.get('/api/v1/stats/overview'),
  hotQuestions: (days = 30, topN = 10) => request.get('/api/v1/stats/hot-questions', { params: { days, topN } }),
  docQuotes: (days = 30, topN = 10) => request.get('/api/v1/stats/doc-quotes', { params: { days, topN } }),
  uploadTrend: (months = 6) => request.get('/api/v1/stats/upload-trend', { params: { months } })
}
