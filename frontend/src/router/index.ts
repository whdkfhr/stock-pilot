import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/api/client'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
      meta: { requiresAuth: true, tab: true },
    },
    {
      path: '/recommend',
      name: 'recommend',
      component: () => import('@/views/RecommendView.vue'),
      meta: { requiresAuth: true, tab: true },
    },
    {
      path: '/notifications',
      name: 'notifications',
      component: () => import('@/views/NotificationsView.vue'),
      meta: { requiresAuth: true, tab: true, title: '알림' },
    },
    {
      path: '/my',
      name: 'my',
      component: () => import('@/views/ComingSoonView.vue'),
      meta: { requiresAuth: true, tab: true, title: '마이' },
    },
    {
      path: '/stocks/:code',
      name: 'stock-detail',
      component: () => import('@/views/StockDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/signup',
      name: 'signup',
      component: () => import('@/views/SignupView.vue'),
      meta: { guestOnly: true },
    },
  ],
})

router.beforeEach((to) => {
  const authed = !!getToken()
  if (to.meta.requiresAuth && !authed) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && authed) {
    return { name: 'home' }
  }
})

export default router
