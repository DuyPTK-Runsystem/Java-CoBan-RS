import { defineComponent, h } from 'vue'
import { createRouter, createWebHistory, RouterView } from 'vue-router'

import { configureApiClient } from '@/services/apiClient'
import { hasAuthenticatedSession } from '@/services/authSession'

const StudentDetailPlaceholder = defineComponent({
  name: 'StudentDetailPlaceholder',
  setup() {
    return () => h('div', { class: 'student-detail-placeholder' })
  },
})

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
      redirect: '/v2/students',
    },
    {
      path: '/students/new',
      redirect: '/v2/students/new',
    },
    {
      path: '/students/:studentId/edit',
      redirect: (to) => `/v2/students/${to.params.studentId}/edit`,
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
          path: 'students',
          name: 'v2-students',
          component: () => import('@/views/StudentListView.vue'),
        },
        {
          path: 'students/new',
          name: 'v2-student-create',
          component: () => import('@/views/StudentFormView.vue'),
        },
        {
          path: 'students/:studentId',
          name: 'v2-student-detail',
          component: StudentDetailPlaceholder,
        },
        {
          path: 'students/:studentId/edit',
          name: 'v2-student-edit',
          component: () => import('@/views/StudentFormView.vue'),
        },
        {
          path: 'academic-years',
          name: 'v2-academic-years',
          component: () => import('@/views/AcademicYearListView.vue'),
        },
        {
          path: 'academic-years/:academicYearId/semesters',
          name: 'v2-semesters',
          component: () => import('@/views/SemesterListView.vue'),
        },
        {
          path: 'academic-catalog/grades',
          name: 'v2-academic-grades',
          component: () => import('@/views/GradeListView.vue'),
        },
        {
          path: 'academic-catalog/classes',
          name: 'v2-academic-classes',
          component: () => import('@/views/SchoolClassListView.vue'),
        },
        {
          path: 'academic-catalog/subjects',
          name: 'v2-academic-subjects',
          component: () => import('@/views/SubjectListView.vue'),
        },
        {
          path: 'academic-catalog/class-subjects',
          name: 'v2-academic-class-subjects',
          component: () => import('@/views/ClassSubjectListView.vue'),
        },
        {
          path: 'enrollments',
          name: 'v2-enrollments',
          component: () => import('@/views/EnrollmentListView.vue'),
        },
        {
          path: 'teachers',
          name: 'v2-teachers',
          component: () => import('@/views/TeacherListView.vue'),
        },
        {
          path: 'teaching-assignments',
          name: 'v2-teaching-assignments',
          component: () => import('@/views/TeachingAssignmentView.vue'),
        },
        {
          path: 'attendance',
          name: 'v2-attendance',
          component: () => import('@/views/AttendanceWorkspaceView.vue'),
        },
        {
          path: 'scorebooks',
          name: 'v2-scorebooks',
          component: () => import('@/views/ScorebookWorkspaceView.vue'),
        },
        {
          path: 'score-change-requests',
          name: 'v2-score-change-requests',
          component: () => import('@/views/ScoreChangeRequestView.vue'),
        },
        {
          path: 'transcripts',
          name: 'v2-transcripts',
          component: () => import('@/views/TranscriptViewerView.vue'),
        },
        {
          path: 'class-transcripts',
          name: 'v2-class-transcripts',
          component: () => import('@/views/ClassTranscriptViewerView.vue'),
        },
        {
          path: 'retake-exams',
          name: 'v2-retake-exams',
          component: () => import('@/views/RetakeResultView.vue'),
        },
        {
          path: 'scorebooks/operations',
          name: 'v2-scorebook-operations',
          component: () => import('@/views/CalculationOperationsView.vue'),
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
    return '/v2'
  }
  return true
})

configureApiClient({
  onUnauthorized: async () => {
    if (router.currentRoute.value.name !== 'login') await router.replace({ name: 'login' })
  },
})

export default router
