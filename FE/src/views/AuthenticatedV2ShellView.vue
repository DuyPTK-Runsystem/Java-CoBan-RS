<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import AuthenticatedLayout from '@/components/AuthenticatedLayout.vue'
import { clearAuthSession, getAuthSession } from '@/services/authSession'
import { logout as logoutApi } from '@/services/userApi'

const router = useRouter()
const session = computed(() => getAuthSession())
const navigation = computed(() => {
  const items = [
    { label: 'Năm học & học kỳ', to: '/v2/academic-years', icon: 'pi pi-calendar' },
    { label: 'Khối', to: '/v2/academic-catalog/grades', icon: 'pi pi-sitemap' },
    { label: 'Lớp', to: '/v2/academic-catalog/classes', icon: 'pi pi-building' },
    { label: 'Môn học', to: '/v2/academic-catalog/subjects', icon: 'pi pi-book' },
    { label: 'Quản lí môn học các lớp', to: '/v2/academic-catalog/class-subjects', icon: 'pi pi-link' },
    { label: 'Xếp lớp', to: '/v2/enrollments', icon: 'pi pi-users' },
    { label: 'Hồ sơ giáo viên', to: '/v2/teachers', icon: 'pi pi-id-card' },
    { label: 'Phân công giảng dạy', to: '/v2/teaching-assignments', icon: 'pi pi-briefcase' },
    { label: 'Điểm danh', to: '/v2/attendance', icon: 'pi pi-calendar' },
    { label: 'Bảng điểm', to: '/v2/transcripts', icon: 'pi pi-table' },
  ]
  const roles = session.value?.user.roles ?? []
  if (roles.some((role) => role === 'ADMIN' || role === 'ACADEMIC_OFFICE' || role === 'TEACHER')) {
    items.push({ label: 'Sổ điểm', to: '/v2/scorebooks', icon: 'pi pi-book' })
    items.push({ label: 'Yêu cầu sửa điểm', to: '/v2/score-change-requests', icon: 'pi pi-file-edit' })
  }
  if (!roles.length || roles.some((role) => role === 'ADMIN' || role === 'ACADEMIC_OFFICE')) {
    items.push({ label: 'Kết quả thi lại', to: '/v2/retake-exams', icon: 'pi pi-check-square' })
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
