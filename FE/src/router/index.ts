import { createRouter, createWebHistory } from 'vue-router'

import { hasAuthenticatedSession } from '@/services/authSession'

declare module 'vue-router' {
  interface RouteMeta {
    guestOnly?: boolean
    requiresAuth?: boolean
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

export default router
