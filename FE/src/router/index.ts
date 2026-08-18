import { createRouter, createWebHistory } from 'vue-router'

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
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
    },
    {
      path: '/students',
      name: 'students',
      component: () => import('@/views/StudentListView.vue'),
    },
    {
      path: '/students/new',
      name: 'student-create',
      component: () => import('@/views/StudentFormView.vue'),
    },
    {
      path: '/students/:studentId/edit',
      name: 'student-edit',
      component: () => import('@/views/StudentFormView.vue'),
    },
  ],
})

export default router
