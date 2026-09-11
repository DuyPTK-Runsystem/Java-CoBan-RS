import { createRouter, createWebHistory, RouterView } from 'vue-router'

import { configureApiClient } from '@/services/apiClient'
import { hasAuthenticatedSession } from '@/services/authSession'
import { getAuthSession } from '@/services/authSession'
import { firstPermittedWorkspacePath, isStudentWorkspace, isStudentWorkspacePath, isTeacherWorkspace, studentWorkspacePaths } from '@/services/studentNavigation'
import type { UserRole } from '@/types/user'

declare module 'vue-router' {
  interface RouteMeta {
    guestOnly?: boolean
    requiresAuth?: boolean
    module?: string
    shell?: 'authenticated'
    allowedRoles?: UserRole[]
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
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/auth/RegisterView.vue'),
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
      component: () => import('@/views/shell/AuthenticatedV2ShellView.vue'),
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
          component: () => import('@/views/student/StudentListView.vue'),
          meta: { allowedRoles: ['ADMIN', 'ACADEMIC_OFFICE', 'TEACHER'] },
        },
        {
          path: 'students/new',
          name: 'v2-student-create',
          component: () => import('@/views/student/StudentFormView.vue'),
          meta: { allowedRoles: ['ADMIN', 'ACADEMIC_OFFICE'] },
        },
        {
          path: 'students/:studentId',
          name: 'v2-student-detail',
          component: () => import('@/views/student/StudentDetailView.vue'),
          meta: { allowedRoles: ['ADMIN', 'ACADEMIC_OFFICE', 'TEACHER'] },
        },
        {
          path: 'students/:studentId/edit',
          name: 'v2-student-edit',
          component: () => import('@/views/student/StudentFormView.vue'),
          meta: { allowedRoles: ['ADMIN', 'ACADEMIC_OFFICE'] },
        },
        {
          path: 'academic-years',
          name: 'v2-academic-years',
          component: () => import('@/views/academic/AcademicYearListView.vue'),
          meta: { allowedRoles: ['ADMIN', 'ACADEMIC_OFFICE'] },
        },
        {
          path: 'academic-years/:academicYearId/semesters',
          name: 'v2-semesters',
          component: () => import('@/views/academic/SemesterListView.vue'),
          meta: { allowedRoles: ['ADMIN', 'ACADEMIC_OFFICE'] },
        },
        {
          path: 'academic-catalog/grades',
          name: 'v2-academic-grades',
          component: () => import('@/views/academic/GradeListView.vue'),
          meta: { allowedRoles: ['ADMIN', 'ACADEMIC_OFFICE'] },
        },
        {
          path: 'academic-catalog/classes',
          name: 'v2-academic-classes',
          component: () => import('@/views/academic/SchoolClassListView.vue'),
        },
        {
          path: 'academic-catalog/subjects',
          name: 'v2-academic-subjects',
          component: () => import('@/views/academic/SubjectListView.vue'),
        },
        {
          path: 'academic-catalog/class-subjects',
          name: 'v2-academic-class-subjects',
          component: () => import('@/views/academic/ClassSubjectListView.vue'),
        },
        {
          path: 'enrollments',
          name: 'v2-enrollments',
          component: () => import('@/views/enrollment/EnrollmentListView.vue'),
          meta: { allowedRoles: ['ADMIN', 'ACADEMIC_OFFICE'] },
        },
        {
          path: 'enrollments/placement/new',
          name: 'v2-placement-new',
          component: () => import('@/views/enrollment/PlacementWorkspaceView.vue'),
          meta: { allowedRoles: ['ADMIN', 'ACADEMIC_OFFICE'] },
        },
        {
          path: 'enrollments/placement/:placementSessionId',
          name: 'v2-placement-session',
          component: () => import('@/views/enrollment/PlacementWorkspaceView.vue'),
          meta: { allowedRoles: ['ADMIN', 'ACADEMIC_OFFICE'] },
        },
        {
          path: 'teachers',
          name: 'v2-teachers',
          component: () => import('@/views/teacher/TeacherListView.vue'),
        },
        {
          path: 'teaching-assignments',
          name: 'v2-teaching-assignments',
          component: () => import('@/views/teacher/TeachingAssignmentView.vue'),
        },
        {
          path: 'attendance',
          name: 'v2-attendance',
          component: () => import('@/views/attendance/AttendanceWorkspaceView.vue'),
        },
        {
          path: 'scorebooks',
          name: 'v2-scorebooks',
          component: () => import('@/views/scorebook/ScorebookWorkspaceView.vue'),
        },
        {
          path: 'score-change-requests',
          name: 'v2-score-change-requests',
          component: () => import('@/views/scorebook/ScoreChangeRequestView.vue'),
        },
        {
          path: 'transcripts',
          name: 'v2-transcripts',
          component: () => import('@/views/transcript/TranscriptViewerView.vue'),
        },
        {
          path: 'class-transcripts',
          name: 'v2-class-transcripts',
          component: () => import('@/views/transcript/ClassTranscriptViewerView.vue'),
        },
        {
          path: 'retake-exams',
          name: 'v2-retake-exams',
          component: () => import('@/views/retake/RetakeResultView.vue'),
        },
        {
          path: 'scorebooks/operations',
          name: 'v2-scorebook-operations',
          component: () => import('@/views/calculation/CalculationOperationsView.vue'),
          meta: { allowedRoles: ['ADMIN', 'ACADEMIC_OFFICE'] },
        },
        {
          path: 'functional-rooms',
          name: 'v2-functional-rooms',
          component: () => import('@/views/functional-room/FunctionalRoomListView.vue'),
          meta: { allowedRoles: ['ADMIN', 'ACADEMIC_OFFICE'] },
        },
        {
          path: 'timetables',
          name: 'v2-timetables',
          component: () => import('@/views/timetable/TimetableListView.vue'),
          meta: { allowedRoles: ['ADMIN', 'ACADEMIC_OFFICE'] },
        },
        {
          path: 'timetables/settings',
          name: 'v2-timetable-settings',
          component: () => import('@/views/timetable/TimetableSettingsView.vue'),
          meta: { allowedRoles: ['ADMIN', 'ACADEMIC_OFFICE'] },
        },
        {
          path: 'timetables/unavailability',
          name: 'v2-timetable-unavailability-admin',
          component: () => import('@/views/timetable/TeacherUnavailabilityView.vue'),
          meta: { allowedRoles: ['ADMIN', 'ACADEMIC_OFFICE'] },
        },
        {
          path: 'timetables/:timetableId',
          name: 'v2-timetable-workspace',
          component: () => import('@/views/timetable/TimetableWorkspaceView.vue'),
          meta: { allowedRoles: ['ADMIN', 'ACADEMIC_OFFICE'] },
        },
        {
          path: 'my-timetable',
          name: 'v2-my-timetable',
          component: () => import('@/views/timetable/MyTimetableView.vue'),
          meta: { allowedRoles: ['TEACHER'] },
        },
        {
          path: 'my-timetable/unavailability',
          name: 'v2-my-timetable-unavailability',
          component: () => import('@/views/timetable/TeacherUnavailabilityView.vue'),
          meta: { allowedRoles: ['TEACHER'] },
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
    return firstPermittedWorkspacePath(getAuthSession()?.user.roles ?? [])
  }
  const roles = getAuthSession()?.user.roles ?? []
  if (to.name === 'v2-shell') {
    return firstPermittedWorkspacePath(roles)
  }
  if (to.meta.requiresAuth && isStudentWorkspace(roles) && !isStudentWorkspacePath(to.path)) {
    return studentWorkspacePaths[0]
  }
  if (to.meta.requiresAuth && isTeacherWorkspace(roles) && to.meta.allowedRoles && !roles.some((role) => to.meta.allowedRoles?.includes(role))) {
    return { name: 'v2-attendance' }
  }
  if (to.meta.allowedRoles) {
    if (!roles.some((role) => to.meta.allowedRoles?.includes(role))) {
      return { name: 'v2-shell' }
    }
  }
  return true
})

configureApiClient({
  onUnauthorized: async () => {
    if (router.currentRoute.value.name !== 'login') await router.replace({ name: 'login' })
  },
})

export default router
