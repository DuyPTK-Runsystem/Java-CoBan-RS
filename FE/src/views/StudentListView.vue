<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useConfirm } from 'primevue/useconfirm'
import ConfirmDialog from 'primevue/confirmdialog'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'

import StudentSearchForm from '@/components/StudentSearchForm.vue'
import StudentTable from '@/components/StudentTable.vue'
import { clearAuthSession, getAuthSession } from '@/services/authSession'
import { deleteStudent, fetchStudents, transitionStudentStatus } from '@/services/studentApi'
import { isApiError } from '@/types/api'
import type { Student, StudentAcademicStatus, StudentQuery, StudentSearchValues } from '@/types/student'

const router = useRouter()
const confirm = useConfirm()

const loading = ref(false)
const students = ref<Student[]>([])
const totalRecords = ref(0)
const totalPages = ref(0)
const errorMessage = ref('')
const statusMessage = ref('')

// Safe lifecycle dialog for R5
const safeDeleteDialogVisible = ref(false)
const affectedStudent = ref<Student | null>(null)
const updatingStatus = ref(false)
const canManageStudents = computed(() => {
  const roles = getAuthSession()?.user.roles ?? []
  return roles.includes('ADMIN') || roles.includes('ACADEMIC_OFFICE')
})

const query = ref<StudentQuery>({
  page: 0,
  pageSize: 10,
  sortField: 'studentCode',
  sortOrder: 1,
  search: { studentCode: '', studentName: '', dateOfBirth: null, status: '', classId: null },
})

function token(): string | null {
  const session = getAuthSession()
  if (session) return session.accessToken
  clearAuthSession()
  void router.replace('/login')
  return null
}

async function load(): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await fetchStudents(accessToken, query.value)
    students.value = response.content
    totalRecords.value = response.totalElements
    totalPages.value = response.totalPages
    query.value = { ...query.value, page: response.page }
  } catch (error) {
    if (isApiError(error, 401)) return
    errorMessage.value = error instanceof Error ? error.message : 'Unable to load students.'
  } finally {
    loading.value = false
  }
}

function search(values: StudentSearchValues): void {
  query.value = { ...query.value, page: 0, search: values }
  void load()
}

function page(value: number, pageSize: number): void {
  query.value = { ...query.value, page: value, pageSize }
  void load()
}

function sort(field: keyof Student, order: 1 | -1): void {
  query.value = { ...query.value, page: 0, sortField: field, sortOrder: order }
  void load()
}

function viewDetail(student: Student): void {
  void router.push(`/v2/students/${student.studentId}`)
}

function editStudent(student: Student): void {
  void router.push(`/v2/students/${student.studentId}/edit`)
}

function confirmDelete(student: Student): void {
  confirm.require({
    message: `Bạn có chắc muốn xóa học sinh ${student.studentName} (${student.studentCode})?`,
    header: 'Xác nhận xóa hồ sơ',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    acceptLabel: 'Xác nhận xóa',
    rejectLabel: 'Hủy',
    accept: () => {
      void remove(student)
    },
  })
}

async function remove(student: Student): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  try {
    await deleteStudent(accessToken, student.studentId)
    if (students.value.length === 1 && query.value.page > 0) query.value.page -= 1
    await load()
    statusMessage.value = `Đã xóa học sinh ${student.studentName}.`
  } catch (error) {
    if (isApiError(error, 401)) return
    if (isApiError(error, 409)) {
      affectedStudent.value = student
      safeDeleteDialogVisible.value = true
      return
    }
    errorMessage.value = error instanceof Error ? error.message : 'Không thể xóa hồ sơ học sinh.'
  }
}

async function changeStudentStatus(newStatus: StudentAcademicStatus): Promise<void> {
  if (!affectedStudent.value) return
  const accessToken = token()
  if (!accessToken) return
  updatingStatus.value = true
  try {
    await transitionStudentStatus(accessToken, affectedStudent.value.studentId, newStatus)
    safeDeleteDialogVisible.value = false
    statusMessage.value = `Đã cập nhật trạng thái học sinh thành ${newStatus}.`
    await load()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Không thể cập nhật trạng thái học sinh.'
  } finally {
    updatingStatus.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="student-list-view">
    <ConfirmDialog />

    <!-- R5 Safe Lifecycle Warning Dialog -->
    <Dialog
      v-model:visible="safeDeleteDialogVisible"
      modal
      header="Chính sách bảo toàn dữ liệu học vụ (R5)"
      :style="{ width: '32rem' }"
    >
      <div class="safe-delete-content">
        <i class="pi pi-shield text-amber-500 text-3xl mb-3" />
        <p class="font-medium text-lg mb-2">Không thể xóa cứng hồ sơ học sinh</p>
        <p class="text-secondary mb-4">
          Học sinh <strong>{{ affectedStudent?.studentName }}</strong> ({{ affectedStudent?.studentCode }})
          đã phát sinh dữ liệu học vụ (phân lớp, điểm danh hoặc sổ điểm).
          Để bảo toàn tính toàn vẹn lịch sử, bạn nên chuyển trạng thái học sinh sang <strong>INACTIVE</strong> (Ngừng học) hoặc <strong>GRADUATED</strong> (Tốt nghiệp).
        </p>
        <div class="flex justify-end gap-2">
          <Button
            v-if="canManageStudents"
            label="Chuyển sang INACTIVE"
            severity="secondary"
            icon="pi pi-ban"
            :loading="updatingStatus"
            @click="changeStudentStatus('INACTIVE')"
          />
          <Button
            v-if="canManageStudents"
            label="Chuyển sang GRADUATED"
            severity="info"
            icon="pi pi-graduation-cap"
            :loading="updatingStatus"
            @click="changeStudentStatus('GRADUATED')"
          />
          <Button
            label="Đóng"
            severity="contrast"
            text
            @click="safeDeleteDialogVisible = false"
          />
        </div>
      </div>
    </Dialog>

    <div class="page-heading">
      <div>
        <p class="eyebrow">Phân hệ học vụ V2</p>
        <h1>Hồ sơ học sinh</h1>
        <p>Tra cứu, quản lý hồ sơ và theo dõi tiến trình học vụ của học sinh.</p>
      </div>
      <div class="page-heading-actions">
        <Button
          v-if="canManageStudents"
          label="Thêm học sinh"
          icon="pi pi-plus"
          @click="router.push('/v2/students/new')"
        />
      </div>
    </div>

    <div v-if="statusMessage" class="form-alert form-alert-info" role="status">
      {{ statusMessage }}
    </div>
    <div v-if="errorMessage" class="form-alert form-alert-error" role="alert">
      {{ errorMessage }}
    </div>

    <section class="content-surface mb-4">
      <StudentSearchForm :loading="loading" @search="search" />
    </section>

    <section class="content-surface">
      <StudentTable
        :students="students"
        :loading="loading"
        :total-records="totalRecords"
        :total-pages="totalPages"
        :page="query.page"
        :rows-per-page="query.pageSize"
        :sort-field="query.sortField"
        :sort-order="query.sortOrder"
        :can-manage-students="canManageStudents"
        @page-change="page"
        @sort-change="sort"
        @view-detail="viewDetail"
        @edit="editStudent"
        @delete="confirmDelete"
      />
    </section>
  </div>
</template>

<style scoped>
.student-list-view {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.safe-delete-content {
  display: flex;
  flex-direction: column;
}
.text-amber-500 {
  color: #f59e0b;
}
.text-secondary {
  color: var(--text-color-secondary, #64748b);
}
</style>
