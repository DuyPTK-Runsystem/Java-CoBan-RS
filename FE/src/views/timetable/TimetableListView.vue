<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import DatePicker from 'primevue/datepicker'
import Dialog from 'primevue/dialog'
import Select from 'primevue/select'
import Tag from 'primevue/tag'

import FormAlert from '@/components/common/FormAlert.vue'
import PageState from '@/components/common/PageState.vue'
import { useAuthSession } from '@/composables/useAuthSession'
import { fetchSemesters } from '@/services/academicApi'
import { createTimetable, listTimetables } from '@/services/timetableApi'
import { extractApiErrorMessage } from '@/types/api'
import type { Semester } from '@/types/academic'
import type { TimetableRevisionStatus, TimetableSummary } from '@/types/timetable'
import type { LoadingState } from '@/types/ui'

const router = useRouter()
const { requireAccessToken } = useAuthSession()

const semesters = ref<Semester[]>([])
const selectedSemesterId = ref<number | null>(null)
const timetables = ref<TimetableSummary[]>([])
const loadingState = ref<LoadingState>('loading')
const generalError = ref('')
const page = ref(0)
const size = ref(10)
const totalElements = ref(0)

// Create timetable dialog
const isCreateDialogVisible = ref(false)
const createLoading = ref(false)
const createError = ref('')
const newEffectiveFrom = ref<Date | null>(new Date())

function getStatusSeverity(status?: TimetableRevisionStatus | null): 'success' | 'warn' | 'secondary' {
  switch (status) {
    case 'PUBLISHED':
      return 'success'
    case 'DRAFT':
      return 'warn'
    case 'ARCHIVED':
    default:
      return 'secondary'
  }
}

function getStatusLabel(status?: TimetableRevisionStatus | null): string {
  switch (status) {
    case 'PUBLISHED':
      return 'Đã công bố'
    case 'DRAFT':
      return 'Bản nháp'
    case 'ARCHIVED':
      return 'Lưu trữ'
    default:
      return 'Chưa có bản ghi'
  }
}

async function loadInitial() {
  const token = requireAccessToken()
  if (!token) return
  loadingState.value = 'loading'
  generalError.value = ''
  try {
    const list = await fetchSemesters(token)
    semesters.value = list
    if (list.length > 0) {
      selectedSemesterId.value = list[0].id
      await loadTimetables()
    } else {
      loadingState.value = 'empty'
    }
  } catch (err) {
    loadingState.value = 'error'
    generalError.value = extractApiErrorMessage(err, 'Không thể tải danh sách học kỳ')
  }
}

async function loadTimetables() {
  if (!selectedSemesterId.value) return
  const token = requireAccessToken()
  if (!token) return
  generalError.value = ''
  try {
    const res = await listTimetables(selectedSemesterId.value, page.value, size.value, token)
    timetables.value = res.result
    totalElements.value = res.meta.totalElements
    loadingState.value = res.result.length > 0 ? 'idle' : 'empty'
  } catch (err) {
    loadingState.value = 'error'
    generalError.value = extractApiErrorMessage(err, 'Không thể tải danh sách thời khóa biểu')
  }
}

watch([selectedSemesterId], () => {
  page.value = 0
  void loadTimetables()
})

function openCreateDialog() {
  createError.value = ''
  newEffectiveFrom.value = new Date()
  isCreateDialogVisible.value = true
}

function formatDateStr(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

async function handleCreateTimetable() {
  if (!selectedSemesterId.value || !newEffectiveFrom.value) return
  const token = requireAccessToken()
  if (!token) return
  createLoading.value = true
  createError.value = ''
  try {
    const res = await createTimetable(
      {
        semesterId: selectedSemesterId.value,
        effectiveFrom: formatDateStr(newEffectiveFrom.value),
      },
      token,
    )
    isCreateDialogVisible.value = false
    void router.push(`/v2/timetables/${res.timetableId}`)
  } catch (err) {
    createError.value = extractApiErrorMessage(err, 'Không thể tạo thời khóa biểu')
  } finally {
    createLoading.value = false
  }
}

function navigateToWorkspace(timetableId: number) {
  void router.push(`/v2/timetables/${timetableId}`)
}

onMounted(() => {
  void loadInitial()
})
</script>

<template>
  <div class="p-6 max-w-7xl mx-auto flex flex-col gap-6">
    <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Quản lý Thời khóa biểu</h1>
        <p class="text-sm text-gray-500">Xây dựng, kiểm tra và công bố thời khóa biểu theo học kỳ</p>
      </div>
      <div class="flex gap-2">
        <Button label="Cấu hình hệ thống" icon="pi pi-cog" severity="secondary" @click="router.push('/v2/timetables/settings')" />
        <Button label="Tạo thời khóa biểu" icon="pi pi-plus" @click="openCreateDialog" />
      </div>
    </div>

    <FormAlert v-if="generalError" :message="generalError" type="error" />

    <div class="bg-white p-4 rounded-xl border border-gray-200 shadow-sm flex flex-wrap gap-4 items-center justify-between">
      <div class="w-72">
        <Select
          v-model="selectedSemesterId"
          :options="semesters"
          option-label="name"
          option-value="id"
          placeholder="Chọn học kỳ"
          class="w-full"
        />
      </div>
    </div>

    <PageState
      v-if="loadingState === 'loading' || loadingState === 'error'"
      :state="loadingState"
      :message="generalError"
      @retry="loadTimetables"
    />

    <div v-else class="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
      <DataTable :value="timetables" responsive-layout="scroll">
        <template #empty>
          <div class="p-8 text-center text-gray-500">
            Chưa có thời khóa biểu nào cho học kỳ này. Nhấn "Tạo thời khóa biểu" để bắt đầu.
          </div>
        </template>

        <Column field="semesterName" header="Học kỳ" style="min-width: 160px" />
        <Column header="Phiên bản hiện hành" style="width: 160px">
          <template #body="{ data }">
            <span class="font-semibold text-sm">
              {{ data.currentRevisionNumber ? `Bản ${data.currentRevisionNumber}` : '—' }}
            </span>
          </template>
        </Column>
        <Column header="Thời gian áp dụng" style="width: 220px">
          <template #body="{ data }">
            <span class="text-xs text-gray-600">
              {{ data.effectiveFrom ? `${data.effectiveFrom} → ${data.effectiveTo || 'Hiện tại'}` : '—' }}
            </span>
          </template>
        </Column>
        <Column header="Trạng thái" style="width: 140px">
          <template #body="{ data }">
            <Tag :value="getStatusLabel(data.status)" :severity="getStatusSeverity(data.status)" />
          </template>
        </Column>
        <Column field="totalRevisions" header="Tổng số bản" style="width: 120px" class="text-center" />
        <Column header="Thao tác" style="width: 160px" class="text-right">
          <template #body="{ data }">
            <Button
              label="Biên tập"
              icon="pi pi-external-link"
              size="small"
              text
              @click="navigateToWorkspace(data.timetableId)"
            />
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Create Dialog -->
    <Dialog
      v-model:visible="isCreateDialogVisible"
      header="Khởi tạo Thời khóa biểu mới"
      modal
      :style="{ width: '480px' }"
    >
      <div class="flex flex-col gap-4">
        <FormAlert v-if="createError" :message="createError" type="error" />

        <div class="flex flex-col gap-1">
          <label class="font-medium text-sm">Học kỳ áp dụng</label>
          <Select
            v-model="selectedSemesterId"
            :options="semesters"
            option-label="name"
            option-value="id"
            class="w-full"
          />
        </div>

        <div class="flex flex-col gap-1">
          <label class="font-medium text-sm">Ngày bắt đầu áp dụng <span class="text-red-500">*</span></label>
          <DatePicker v-model="newEffectiveFrom" date-format="yy-mm-dd" show-icon />
        </div>
      </div>

      <template #footer>
        <div class="flex justify-end gap-2 mt-4">
          <Button label="Hủy" severity="secondary" text @click="isCreateDialogVisible = false" />
          <Button label="Tạo thời khóa biểu" :loading="createLoading" @click="handleCreateTimetable" />
        </div>
      </template>
    </Dialog>
  </div>
</template>

