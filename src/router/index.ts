import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../layouts/PortalLayout.vue'),
    redirect: '/home',
    children: [
      { path: 'home', name: 'home', component: () => import('../views/HomeView.vue'), meta: { title: '首页' } },
      { path: 'search', name: 'search', component: () => import('../views/SearchView.vue'), meta: { title: '智能检索' } },
      { path: 'chat', name: 'chat', component: () => import('../views/ChatView.vue'), meta: { title: 'AI 问答' } },
      { path: 'docs', name: 'docs', component: () => import('../views/DocCenterView.vue'), meta: { title: '文档中心' } },
      { path: 'docs/:id', name: 'docDetail', component: () => import('../views/DocDetailView.vue'), meta: { title: '文档详情' } },
      { path: 'my', name: 'my', component: () => import('../views/MyView.vue'), meta: { title: '我的' } }
    ]
  },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('../views/admin/DashboardView.vue'), meta: { title: '仪表盘', perm: 'stats:view' } },
      { path: 'documents', name: 'adminDocs', component: () => import('../views/admin/DocManageView.vue'), meta: { title: '文档管理', perm: 'doc:list' } },
      { path: 'categories', name: 'categories', component: () => import('../views/admin/CategoryView.vue'), meta: { title: '分类管理', perm: 'doc:category:list' } },
      { path: 'audit', name: 'audit', component: () => import('../views/admin/AuditCenterView.vue'), meta: { title: '审核中心', perm: 'doc:audit' } },
      { path: 'stats', name: 'stats', component: () => import('../views/admin/StatsView.vue'), meta: { title: '统计分析', perm: 'stats:view' } },
      { path: 'users', name: 'users', component: () => import('../views/admin/UserView.vue'), meta: { title: '用户管理', perm: 'system:user:list' } },
      { path: 'roles', name: 'roles', component: () => import('../views/admin/RoleView.vue'), meta: { title: '角色权限', perm: 'system:role:list' } },
      { path: 'logs', name: 'logs', component: () => import('../views/admin/LogView.vue'), meta: { title: '日志查询', perm: 'system:log:list' } },
      { path: 'settings', name: 'settings', component: () => import('../views/admin/SettingView.vue'), meta: { title: '系统设置', perm: 'system:setting:list' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.meta.public) return true
  if (!userStore.token) return { name: 'login' }
  // 权限校验：无对应权限码则回首页
  const perm = to.meta.perm as string | undefined
  if (perm && !userStore.hasPerm(perm)) {
    return { name: 'home' }
  }
  return true
})

export default router
