<script setup lang="ts">
import Tag from 'primevue/tag'

import EmptyState from '@/components/common/EmptyState.vue'
import type { Student, StudentAcademicStatus } from '@/types/student'
import { formatStudentDate } from '@/utils/studentDate'

defineProps<{
  student: Student | null
}>()

function getStatusSeverity(status?: StudentAcademicStatus | string | null): 'success' | 'secondary' | 'info' {
  if (status === 'INACTIVE') return 'secondary'
  if (status === 'GRADUATED') return 'info'
  return 'success'
}

function getStatusLabel(status?: StudentAcademicStatus | string | null): string {
  if (status === 'INACTIVE') return 'Ngừng học'
  if (status === 'GRADUATED') return 'Tốt nghiệp'
  return 'Đang học'
}

function getRoleLabel(role?: string | null): string {
  return ({
    STUDENT: 'Học sinh',
    TEACHER: 'Giáo viên',
    ADMIN: 'Quản trị viên',
    ACADEMIC_OFFICE: 'Giáo vụ',
  } as Record<string, string>)[role ?? ''] ?? 'Chưa xác định'
}
</script>

<template>
  <div class="tab-content-grid">
    <section class="content-surface info-card">
      <div class="card-header">
        <h2>Thông tin lý lịch học sinh</h2>
        <p class="section-caption">Dữ liệu nhân khẩu học chính thức được quản lý trên hệ thống.</p>
      </div>

      <dl class="meta-grid">
        <div class="meta-item">
          <dt>Mã học sinh</dt>
          <dd class="font-mono font-semibold">{{ student?.studentCode || '—' }}</dd>
        </div>
        <div class="meta-item">
          <dt>Họ và tên</dt>
          <dd class="font-semibold">{{ student?.studentName || '—' }}</dd>
        </div>
        <div class="meta-item">
          <dt>Ngày sinh</dt>
          <dd>{{ formatStudentDate(student?.dateOfBirth) }}</dd>
        </div>
        <div class="meta-item">
          <dt>Địa chỉ thường trú</dt>
          <dd>{{ student?.address || '—' }}</dd>
        </div>
        <div class="meta-item">
          <dt>Trạng thái học vụ</dt>
          <dd>
            <Tag
              :value="getStatusLabel(student?.status)"
              :severity="getStatusSeverity(student?.status)"
            />
          </dd>
        </div>
      </dl>
    </section>

    <section class="content-surface info-card">
      <div class="card-header">
        <h2>Tài khoản đăng nhập liên kết (V3)</h2>
        <p class="section-caption">Tài khoản để học sinh đăng nhập và tra cứu thông tin của mình.</p>
      </div>

      <div v-if="student?.account" class="account-card-body">
        <div class="account-badge-box">
          <i class="pi pi-id-card text-primary text-3xl mb-2" />
          <Tag value="Tài khoản đang hoạt động" severity="success" />
        </div>
        <dl class="meta-grid">
          <div class="meta-item">
            <dt>Mã người dùng</dt>
            <dd class="font-mono">{{ student.account.userId }}</dd>
          </div>
          <div class="meta-item">
            <dt>Tên đăng nhập</dt>
            <dd class="font-mono font-semibold">{{ student.account.username }}</dd>
          </div>
          <div class="meta-item">
            <dt>Vai trò hệ thống</dt>
            <dd><Tag :value="getRoleLabel(student.account.role)" severity="info" /></dd>
          </div>
        </dl>
      </div>

      <EmptyState
        v-else
        icon="pi pi-user-minus"
        heading="Chưa cấp tài khoản đăng nhập"
        message="Học sinh này chưa có tài khoản người dùng liên kết trong hệ thống."
      />
    </section>
  </div>
</template>

<style scoped>
.tab-content-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1.25rem;
}
.info-card {
  padding: 1.5rem;
}
.card-header {
  margin-bottom: 1.25rem;
}
.card-header h2 {
  margin: 0 0 0.25rem;
  font-size: 1.25rem;
  font-weight: 600;
}
.section-caption {
  margin: 0;
  font-size: 0.875rem;
  color: var(--text-color-secondary, #64748b);
}
.meta-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 1rem;
  margin: 0;
}
.meta-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.meta-item dt {
  font-size: 0.8125rem;
  color: var(--text-color-secondary, #64748b);
  font-weight: 500;
}
.meta-item dd {
  margin: 0;
  font-size: 0.9375rem;
  color: #1e293b;
}
.account-card-body {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}
.account-badge-box {
  display: flex;
  align-items: center;
  gap: 1rem;
}
.font-mono {
  font-family: monospace;
}
.font-semibold {
  font-weight: 600;
}
</style>
