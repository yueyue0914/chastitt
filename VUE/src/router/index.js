import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/login', component: () => import('../views/LoginView.vue'), meta: { guest: true } },
  { path: '/', component: () => import('../views/HomeView.vue'), meta: { auth: true } },
  { path: '/profile', component: () => import('../views/ProfileView.vue'), meta: { auth: true } },
  { path: '/keys', component: () => import('../views/KeysView.vue'), meta: { auth: true } },
  { path: '/key/:code', component: () => import('../views/KeyholderView.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.auth && !auth.isLoggedIn) return '/login'
  if (to.meta.guest && auth.isLoggedIn) return '/'
  return true
})

export default router
