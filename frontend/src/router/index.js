import { createRouter, createWebHistory } from 'vue-router'
import { getAuthSession } from '../api/client'

const routes = [
  { path: '/', component: () => import('../pages/Home.vue') },
  { path: '/login', component: () => import('../pages/LoginPage.vue') },
  { path: '/register', component: () => import('../pages/RegisterPage.vue') },
  { path: '/lost-publish', component: () => import('../pages/LostPublishPage.vue'), meta: { requiresAuth: true, roles: ['USER'] } },
  { path: '/found-publish', component: () => import('../pages/FoundPublishPage.vue'), meta: { requiresAuth: true, roles: ['USER'] } },
  { path: '/search', component: () => import('../pages/SearchPage.vue') },
  { path: '/item-detail', component: () => import('../pages/ItemDetailPage.vue') },
  { path: '/messages', component: () => import('../pages/MessageCenter.vue'), meta: { requiresAuth: true } },
  { path: '/user-center', component: () => import('../pages/UserCenterPage.vue'), meta: { requiresAuth: true } },
  { path: '/items', component: () => import('../pages/ItemListPage.vue') },
  { path: '/admin-login', component: () => import('../pages/AdminLoginPage.vue') },
  { path: '/review-dashboard', component: () => import('../pages/ReviewDashboardPage.vue'), meta: { requiresAuth: true, roles: ['REVIEW_ADMIN', 'SYS_ADMIN'] } },
  { path: '/review-pending', component: () => import('../pages/ReviewPendingPage.vue'), meta: { requiresAuth: true, roles: ['REVIEW_ADMIN', 'SYS_ADMIN'] } },
  { path: '/review-history', component: () => import('../pages/ReviewHistoryPage.vue'), meta: { requiresAuth: true, roles: ['REVIEW_ADMIN', 'SYS_ADMIN'] } },
  { path: '/admin-dashboard', component: () => import('../pages/SysAdminDashboardPage.vue'), meta: { requiresAuth: true, roles: ['SYS_ADMIN'] } },
  { path: '/admin-lost-items', component: () => import('../pages/AdminLostManagePage.vue'), meta: { requiresAuth: true, roles: ['SYS_ADMIN'] } },
  { path: '/admin-found-items', component: () => import('../pages/AdminFoundManagePage.vue'), meta: { requiresAuth: true, roles: ['SYS_ADMIN'] } },
  { path: '/admin-pending-items', component: () => import('../pages/AdminPendingManagePage.vue'), meta: { requiresAuth: true, roles: ['SYS_ADMIN'] } },
  { path: '/admin-claims', component: () => import('../pages/AdminClaimsPage.vue'), meta: { requiresAuth: true, roles: ['SYS_ADMIN'] } },
  { path: '/admin-users', component: () => import('../pages/AdminUsersPage.vue'), meta: { requiresAuth: true, roles: ['SYS_ADMIN'] } },
  { path: '/admin-announcements', component: () => import('../pages/AdminAnnouncementsPage.vue'), meta: { requiresAuth: true, roles: ['SYS_ADMIN'] } },
  { path: '/admin-accounts', component: () => import('../pages/AdminAccountsPage.vue'), meta: { requiresAuth: true, roles: ['SYS_ADMIN'] } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const { token, role } = getAuthSession()
  if (to.meta.requiresAuth && !token) {
    return to.path.startsWith('/admin') || to.path.startsWith('/review')
      ? '/admin-login'
      : `/login?redirect=${encodeURIComponent(to.fullPath)}`
  }

  if (to.meta.roles && !to.meta.roles.includes(role)) {
    if (role === 'SYS_ADMIN') return '/admin-dashboard'
    if (role === 'REVIEW_ADMIN') return '/review-dashboard'
    if (role === 'USER') return '/user-center'
    return '/'
  }

  return true
})

export default router
