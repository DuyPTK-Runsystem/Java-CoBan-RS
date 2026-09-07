<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AuthenticatedLayout, { type NavigationItem } from '@/components/AuthenticatedLayout.vue'
import { clearAuthSession, getAuthSession } from '@/services/authSession'
import { logout as logoutApi } from '@/services/userApi'

const router = useRouter()
const route = useRoute()
const session = computed(() => getAuthSession())
const navigation = computed<NavigationItem[]>(() => {
  const items: NavigationItem[] = [
    { label: 'Năm học & học kỳ', to: '/v2/academic-years', icon: 'pi pi-calendar' },
    { label: 'Khối', to: '/v2/academic-catalog/grades', icon: 'pi pi-sitemap' },
    { label: 'Lớp', to: '/v2/academic-catalog/classes', icon: 'pi pi-building' },
    { label: 'Môn học', to: '/v2/academic-catalog/subjects', icon: 'pi pi-book' },
    { label: 'Quản lí môn học các lớp', to: '/v2/academic-catalog/class-subjects', icon: 'pi pi-link' },
    { label: 'Xếp lớp', to: '/v2/enrollments', icon: 'pi pi-users' },
    { label: 'Hồ sơ giáo viên', to: '/v2/teachers', icon: 'pi pi-id-card' },
    { label: 'Phân công giảng dạy', to: '/v2/teaching-assignments', icon: 'pi pi-briefcase' },
    { label: 'Điểm danh', to: '/v2/attendance', icon: 'pi pi-calendar' },
  ]
  const roles = session.value?.user.roles ?? []
  const isNonStudent = roles.some((role) => role === 'ADMIN' || role === 'ACADEMIC_OFFICE' || role === 'TEACHER')

  // Tab Bảng điểm chỉ hiển thị cho học sinh, ẩn hoàn toàn đối với non-student user
  if (!isNonStudent) {
    items.push({ label: 'Bảng điểm', to: '/v2/transcripts', icon: 'pi pi-table' })
  }

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
  if (!roles.length || roles.some((role) => role === 'ADMIN' || role === 'ACADEMIC_OFFICE')) {
    items.push({ label: 'Kết quả thi lại', to: '/v2/retake-exams', icon: 'pi pi-check-square' })
    items.push({ label: 'Vận hành tính điểm', to: '/v2/scorebooks/operations', icon: 'pi pi-cog' })
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
