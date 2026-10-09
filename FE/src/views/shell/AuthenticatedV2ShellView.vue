<script setup lang="ts">
import { computed, onActivated, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AuthenticatedLayout, { type NavigationItem } from '@/components/common/AuthenticatedLayout.vue'
import { clearAuthSession, getAuthSession } from '@/services/authSession'
import {
  fetchUnreadNotificationCount,
  NOTIFICATION_INBOX_CHANGED_EVENT,
  NOTIFICATION_READ_EVENT,
  startNotificationEventStream,
} from '@/services/notificationApi'
import { isStudentWorkspace, isStudentWorkspacePath, isTeacherWorkspace } from '@/services/studentNavigation'
import { logout as logoutApi } from '@/services/userApi'
import { USER_ROLE } from '@/types/user'

const router = useRouter()
const route = useRoute()
const session = computed(() => getAuthSession())
const unreadNotificationCount = ref<number | null>(null)
let stopNotificationEventStream: (() => void) | undefined
let unreadCountRequestId = 0

async function refreshUnreadNotificationCount(): Promise<void> {
  const requestId = ++unreadCountRequestId
  const accessToken = session.value?.accessToken
  if (!accessToken) {
    unreadNotificationCount.value = null
    return
  }

  try {
    const count = await fetchUnreadNotificationCount(accessToken)
    if (requestId === unreadCountRequestId) unreadNotificationCount.value = count
  } catch {
    // Preserve the last known badge when a refresh fails; it is supplemental to the workspace.
  }
}

function handleNotificationRead(): void {
  void refreshUnreadNotificationCount()
}

function handleInboxChanged(): void {
  void refreshUnreadNotificationCount()
  window.dispatchEvent(new Event(NOTIFICATION_INBOX_CHANGED_EVENT))
}

onMounted(() => {
  window.addEventListener(NOTIFICATION_READ_EVENT, handleNotificationRead)
  const accessToken = session.value?.accessToken
  if (accessToken) stopNotificationEventStream = startNotificationEventStream(accessToken, handleInboxChanged)
  void refreshUnreadNotificationCount()
})

onActivated(() => {
  void refreshUnreadNotificationCount()
})

onBeforeUnmount(() => {
  window.removeEventListener(NOTIFICATION_READ_EVENT, handleNotificationRead)
  stopNotificationEventStream?.()
})

const navigation = computed<NavigationItem[]>(() => {
  const items: NavigationItem[] = [
    { label: 'Danh mục sách', to: '/v2/library/books', icon: 'pi pi-book' },
    { label: 'Lịch sử loan', to: '/v2/library/loans', icon: 'pi pi-history', active: route?.path === '/v2/library/loans' },
    { label: 'Reservation', to: '/v2/library/reservations', icon: 'pi pi-bookmark', active: route?.path === '/v2/library/reservations' },
    { label: 'Fine', to: '/v2/library/fines', icon: 'pi pi-wallet', active: route?.path === '/v2/library/fines' },
    { label: 'Thẻ thư viện của tôi', to: '/v2/library/my-card', icon: 'pi pi-id-card', active: route?.path === '/v2/library/my-card' },
    { label: 'Năm học & học kỳ', to: '/v2/academic-years', icon: 'pi pi-calendar' },
    { label: 'Khối', to: '/v2/academic-catalog/grades', icon: 'pi pi-sitemap' },
    { label: 'Lớp', to: '/v2/academic-catalog/classes', icon: 'pi pi-building' },
    { label: 'Môn học', to: '/v2/academic-catalog/subjects', icon: 'pi pi-book' },
    { label: 'Quản lí môn học các lớp', to: '/v2/academic-catalog/class-subjects', icon: 'pi pi-link' },
    { label: 'Xếp lớp', to: '/v2/enrollments', icon: 'pi pi-users', active: Boolean(route?.path?.startsWith('/v2/enrollments')) },
    { label: 'Hồ sơ giáo viên', to: '/v2/teachers', icon: 'pi pi-id-card' },
    { label: 'Phân công giảng dạy', to: '/v2/teaching-assignments', icon: 'pi pi-briefcase' },
    { label: 'Điểm danh', to: '/v2/attendance', icon: 'pi pi-calendar' },
  ]
  const roles = session.value?.user.roles ?? []
  const isNonStudent = roles.some((role) => role === USER_ROLE.ADMIN || role === USER_ROLE.ACADEMIC_OFFICE || role === USER_ROLE.TEACHER)

  if (roles.some((role) => role === USER_ROLE.ADMIN || role === USER_ROLE.LIBRARIAN)) {
    items.splice(1, 0,
      { label: 'Quầy lưu thông', to: '/v2/library/circulation', icon: 'pi pi-sync', active: route?.path === '/v2/library/circulation' },
      { label: 'Chính sách thư viện', to: '/v2/library/policy', icon: 'pi pi-sliders-h', active: route?.path === '/v2/library/policy' },
      { label: 'Overdue fine batch', to: '/v2/library/batch-jobs', icon: 'pi pi-clock', active: route?.path === '/v2/library/batch-jobs' },
    )
    items.splice(1, 0, {
      label: 'Bạn đọc & Thẻ thư viện',
      to: '/v2/library/patrons',
      icon: 'pi pi-users',
      active: Boolean(route?.path?.startsWith('/v2/library/patrons')),
    })
  }

  // Tab Bảng điểm chỉ hiển thị cho học sinh, ẩn hoàn toàn đối với non-student user
  if (!isNonStudent) {
    items.push({ label: 'Bảng điểm', to: '/v2/transcripts', icon: 'pi pi-table' })
  }
  items.push({
    label: 'Thông báo',
    to: '/v2/notifications',
    icon: 'pi pi-bell',
    active: Boolean(route?.path?.startsWith('/v2/notifications')),
    badge: unreadNotificationCount.value ?? undefined,
  })

  if (isNonStudent) {
    const isStudentActive = Boolean(route?.path?.startsWith('/v2/students'))
    const enrollmentsIndex = items.findIndex((item) => item.to === '/v2/enrollments')
    const studentItem: NavigationItem = {
      label: 'Hồ sơ học sinh',
      to: '/v2/students',
      icon: 'pi pi-user',
      active: isStudentActive,
    }
    if (enrollmentsIndex >= 0) {
      items.splice(enrollmentsIndex + 1, 0, studentItem)
    } else {
      items.push(studentItem)
    }

    // Khi admin/giáo vụ/teacher xem bảng điểm học sinh (/v2/transcripts), tab này vẫn sáng để đánh lừa thị giác
    const isClassTranscriptActive = route?.path === '/v2/class-transcripts' || route?.path === '/v2/transcripts'
    items.push({
      label: 'Bảng điểm theo lớp',
      to: '/v2/class-transcripts',
      icon: 'pi pi-list',
      active: isClassTranscriptActive,
    })
    items.push({ label: 'Sổ điểm', to: '/v2/scorebooks', icon: 'pi pi-book' })
    items.push({ label: 'Yêu cầu sửa điểm', to: '/v2/score-change-requests', icon: 'pi pi-file-edit' })
  }
  if (!roles.length || roles.some((role) => role === USER_ROLE.ADMIN || role === USER_ROLE.ACADEMIC_OFFICE)) {
    items.push({ label: 'Kết quả thi lại', to: '/v2/retake-exams', icon: 'pi pi-check-square' })
    items.push({ label: 'Phòng chức năng', to: '/v2/functional-rooms', icon: 'pi pi-home' })
    items.push({
      label: 'Thời khóa biểu',
      to: '/v2/timetables',
      icon: 'pi pi-calendar-plus',
      active: Boolean(route?.path?.startsWith('/v2/timetables')),
    })
  }
  if (roles.includes(USER_ROLE.TEACHER)) {
    items.push({
      label: 'Thời khóa biểu của tôi',
      to: '/v2/my-timetable',
      icon: 'pi pi-calendar',
      active: Boolean(route?.path?.startsWith('/v2/my-timetable')),
    })
    items.push({
      label: 'Sổ đầu bài của tôi',
      to: '/v2/my-lesson-logs',
      icon: 'pi pi-file-edit',
      active: Boolean(route?.path?.startsWith('/v2/my-lesson-logs')),
    })
  }
  if (roles.some((role) => role === USER_ROLE.ADMIN || role === USER_ROLE.ACADEMIC_OFFICE || role === USER_ROLE.TEACHER)) {
    items.push({
      label: 'Sổ đầu bài',
      to: '/v2/lesson-logs',
      icon: 'pi pi-book',
      active: Boolean(route?.path?.startsWith('/v2/lesson-logs')),
    })
  }
  const catalogOnly = roles.includes(USER_ROLE.LIBRARIAN)
    && !roles.some((role) => role === USER_ROLE.ADMIN || role === USER_ROLE.ACADEMIC_OFFICE || role === USER_ROLE.TEACHER || role === USER_ROLE.STUDENT)
  const librarySelfServicePaths = new Set(['/v2/library/books', '/v2/library/loans', '/v2/library/reservations', '/v2/library/fines', '/v2/library/my-card'])
  if (catalogOnly) return items.filter((item) => item.to === '/v2/library/books' || item.to === '/v2/library/patrons' || item.to === '/v2/library/my-card' || item.to === '/v2/library/loans' || item.to === '/v2/library/reservations' || item.to === '/v2/library/fines' || item.to === '/v2/library/circulation' || item.to === '/v2/library/policy' || item.to === '/v2/library/batch-jobs' || item.to === '/v2/notifications')
  if (isStudentWorkspace(roles)) return items.filter((item) => isStudentWorkspacePath(item.to) || librarySelfServicePaths.has(item.to))
  if (isTeacherWorkspace(roles)) {
    const teacherRestrictedPaths = new Set([
      '/v2/academic-years',
      '/v2/academic-catalog/grades',
      '/v2/enrollments',
      '/v2/scorebooks/operations',
    ])
    return items.filter((item) => !teacherRestrictedPaths.has(item.to))
  }
  return items
})

function logout(): void {
  const accessToken = session.value?.accessToken
  if (!accessToken) {
    clearAuthSession()
    void router.replace({ name: 'login' })
    return
  }
  void logoutApi(accessToken).catch(() => undefined).finally(() => {
    clearAuthSession()
    if (router.currentRoute.value.name !== 'login') return router.replace({ name: 'login' })
  })
}
</script>

<template>
  <AuthenticatedLayout
    :user-name="session?.user.username ?? ''"
    :navigation="navigation"
    @logout="logout"
  >
    <RouterView />
  </AuthenticatedLayout>
</template>
