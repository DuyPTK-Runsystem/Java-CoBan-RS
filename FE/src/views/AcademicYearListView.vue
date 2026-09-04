<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
import Button from 'primevue/button'
import ConfirmDialog from 'primevue/confirmdialog'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import { useRouter } from 'vue-router'

import AcademicYearDialog from '@/components/AcademicYearDialog.vue'
import AcademicYearTable from '@/components/AcademicYearTable.vue'
import FormAlert from '@/components/FormAlert.vue'
import PageState from '@/components/PageState.vue'
import { clearAuthSession, getAuthSession } from '@/services/authSession'
import { closeAcademicYear, createAcademicYear, fetchAcademicYears, updateAcademicYear } from '@/services/academicApi'
import { isApiError } from '@/types/api'
import type { AcademicYear, AcademicYearFormValues, AcademicYearStatus } from '@/types/academic'
import type { LoadingState } from '@/types/ui'

type StatusFilter = 'ALL' | AcademicYearStatus

const router = useRouter()
const confirm = useConfirm()
const academicYears = ref<AcademicYear[]>([])
const loadingState = ref<LoadingState>('loading')
const loading = computed(() => loadingState.value === 'loading')
const forbidden = ref(false)
const errorMessage = ref('')
const statusMessage = ref('')
const searchText = ref('')
const statusFilter = ref<StatusFilter>('ALL')
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const selectedAcademicYear = ref<AcademicYear | null>(null)
const saving = ref(false)
const dialogErrorMessage = ref('')

const statusOptions: Array<{ label: string; value: StatusFilter }> = [
  { label: 'Tất cả trạng thái', value: 'ALL' },
  { label: 'Chưa hoạt động', value: 'DRAFT' },
  { label: 'Đang hoạt động', value: 'ACTIVE' },
  { label: 'Đã đóng', value: 'CLOSED' },
]

const filteredAcademicYears = computed(() => {
  const normalizedSearch = searchText.value.trim().toLocaleLowerCase()
  return academicYears.value.filter((academicYear) => {
    const matchesSearch = !normalizedSearch || academicYear.code.toLocaleLowerCase().includes(normalizedSearch)
    const matchesStatus = statusFilter.value === 'ALL' || academicYear.status === statusFilter.value
    return matchesSearch && matchesStatus
  })
})
const pageState = computed<LoadingState>(() => loadingState.value === 'success' && academicYears.value.length === 0 ? 'empty' : loadingState.value)

function accessToken(): string | null {
  const session = getAuthSession()
  if (session) return session.accessToken
  clearAuthSession()
  void router.replace({ name: 'login' })
  return null
}

function messageFor(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

async function load(): Promise<void> {
  const token = accessToken()
  if (!token) return
  loadingState.value = 'loading'
  forbidden.value = false
  errorMessage.value = ''
  try {
    academicYears.value = await fetchAcademicYears(token)
    loadingState.value = 'success'
  } catch (error) {
    if (isApiError(error, 401)) return
    forbidden.value = isApiError(error, 403)
    errorMessage.value = messageFor(error, 'Không thể tải danh sách năm học.')
    loadingState.value = 'error'
  }
}

function openCreate(): void {
  selectedAcademicYear.value = null
  dialogMode.value = 'create'
  dialogErrorMessage.value = ''
  dialogVisible.value = true
}

function openEdit(academicYear: AcademicYear): void {
  selectedAcademicYear.value = academicYear
  dialogMode.value = 'edit'
  dialogErrorMessage.value = ''
  dialogVisible.value = true
}

function closeDialog(): void {
  dialogVisible.value = false
  dialogErrorMessage.value = ''
}

async function saveAcademicYear(values: AcademicYearFormValues): Promise<void> {
  const token = accessToken()
  if (!token) return
  saving.value = true
  dialogErrorMessage.value = ''
  const request = {
    code: values.code.trim(),
    startDate: values.startDate,
    endDate: values.endDate,
    status: values.status,
    notes: values.notes.trim() || null,
  }
  try {
    if (dialogMode.value === 'edit' && selectedAcademicYear.value) {
      await updateAcademicYear(token, selectedAcademicYear.value.id, request)
      statusMessage.value = 'Đã cập nhật năm học.'
    } else {
      await createAcademicYear(token, request)
      statusMessage.value = 'Đã tạo năm học.'
    }
    await load()
    closeDialog()
  } catch (error) {
    if (isApiError(error, 401)) return
    dialogErrorMessage.value = messageFor(error, 'Không thể lưu năm học.')
  } finally {
    saving.value = false
  }
}

async function viewSemesters(academicYear: AcademicYear): Promise<void> {
  await router.push({ name: 'v2-semesters', params: { academicYearId: academicYear.id } })
}

function confirmClose(academicYear: AcademicYear): void {
  confirm.require({
    message: `Bạn có chắc muốn đóng năm học ${academicYear.code}? Dữ liệu lịch sử sẽ được giữ nguyên.`,
    header: 'Đóng năm học',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Đóng năm học',
    rejectLabel: 'Hủy',
    accept: () => { void close(academicYear) },
  })
}

async function close(academicYear: AcademicYear): Promise<void> {
  const token = accessToken()
  if (!token) return
  errorMessage.value = ''
  try {
    await closeAcademicYear(token, academicYear.id)
    statusMessage.value = 'Đã đóng năm học.'
    await load()
  } catch (error) {
    if (isApiError(error, 401)) return
    errorMessage.value = messageFor(error, 'Không thể đóng năm học.')
  }
}

onMounted(() => { void load() })
</script>

<template>
  <ConfirmDialog />
  <div class="page-heading">
    <div>
      <p class="eyebrow">Academic structure</p>
      <h1>Năm học</h1>
      <p>Quản lý năm học và mở danh sách học kỳ tương ứng.</p>
    </div>
    <div class="page-heading-actions">
      <Button label="Tạo năm học" icon="pi pi-plus" @click="openCreate" />
    </div>
  </div>
  <FormAlert v-if="statusMessage" tone="success" :message="statusMessage" />
  <FormAlert v-if="errorMessage && !forbidden" tone="error" :message="errorMessage" />
  <section class="content-surface">
    <div class="section-heading">
      <div>
        <h2>Danh sách năm học</h2>
        <p class="section-caption">Tìm theo mã năm học và lọc trên danh sách hiện tại.</p>
      </div>
    </div>
    <div class="search-grid">
      <div class="field-group">
        <label for="academic-year-search">Tìm theo mã</label>
        <InputText id="academic-year-search" v-model="searchText" placeholder="Ví dụ: 2026-2027" />
      </div>
      <div class="field-group">
        <label for="academic-year-status-filter">Trạng thái</label>
        <Select id="academic-year-status-filter" v-model="statusFilter" :options="statusOptions" option-label="label" option-value="value" fluid />
      </div>
    </div>
  </section>
  <section class="content-surface">
    <PageState
      :state="pageState"
      :forbidden="forbidden"
      forbidden-message="Bạn không có quyền xem danh sách năm học."
      :error-message="errorMessage"
      empty-heading="Chưa có năm học"
      empty-message="Hãy tạo năm học đầu tiên để bắt đầu quản lý học kỳ."
      @retry="load"
    >
      <AcademicYearTable :academic-years="filteredAcademicYears" :loading="loading" @edit="openEdit" @close="confirmClose" @view-semesters="viewSemesters" />
    </PageState>
  </section>
  <AcademicYearDialog v-model:visible="dialogVisible" :mode="dialogMode" :initial-value="selectedAcademicYear" :saving="saving" :error-message="dialogErrorMessage" @save="saveAcademicYear" @cancel="closeDialog" />
</template>
