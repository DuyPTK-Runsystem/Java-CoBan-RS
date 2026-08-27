import { createRouter, createWebHistory, RouterView } from 'vue-router'

import { configureApiClient } from '@/services/apiClient'
import { hasAuthenticatedSession } from '@/services/authSession'

declare module 'vue-router' {
  interface RouteMeta {
    guestOnly?: boolean
    requiresAuth?: boolean
    module?: string
    shell?: 'authenticated'
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login',
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/students',
      name: 'students',
      component: () => import('@/views/StudentListView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/students/new',
      name: 'student-create',
      component: () => import('@/views/StudentFormView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/students/:studentId/edit',
      name: 'student-edit',
      component: () => import('@/views/StudentFormView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/v2',
      component: () => import('@/views/AuthenticatedV2ShellView.vue'),
      meta: { requiresAuth: true, module: 'v2', shell: 'authenticated' },
      children: [
        {
          path: '',
          name: 'v2-shell',
          component: RouterView,
        },
        {
          // Business routes must be registered before this neutral outlet.
          path: ':pathMatch(.*)*',
          name: 'v2-outlet',
          component: RouterView,
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/login' },
  ],
})

router.beforeEach((to) => {
  const authenticated = hasAuthenticatedSession()
  if (to.meta.requiresAuth && !authenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && authenticated) {
    return { name: 'students' }
  }
  return true
})

configureApiClient({
  onUnauthorized: async () => {
    if (router.currentRoute.value.name !== 'login') await router.replace({ name: 'login' })
  },
})

export default router
