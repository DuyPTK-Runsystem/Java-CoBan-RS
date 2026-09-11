<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import Button from 'primevue/button'
import Column from 'primevue/column'
import ConfirmDialog from 'primevue/confirmdialog'
import DataTable from 'primevue/datatable'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import { useConfirm } from 'primevue/useconfirm'

import TeacherUnavailabilityDialog from '@/components/timetable/TeacherUnavailabilityDialog.vue'
import FormAlert from '@/components/common/FormAlert.vue'
import PageState from '@/components/common/PageState.vue'
import { useAuthSession } from '@/composables/useAuthSession'
import { fetchSemesters } from '@/services/academicApi'
import { fetchTeachers } from '@/services/teacherApi'
import {
  approveTeacherUnavailability,
  createTeacherUnavailability,
  listTeacherUnavailabilities,
  rejectTeacherUnavailability,
  updateTeacherUnavailability,
  withdrawTeacherUnavailability,
} from '@/services/teacherUnavailabilityApi'
import { extractApiErrorMessage } from '@/types/api'
import type { Semester } from '@/types/academic'
import type { Teacher } from '@/types/teacher'
import type {
  CreateUnavailabilityPayload,
  TeacherUnavailability,
  TeacherUnavailabilityStatus,
  UpdateUnavailabilityPayload,
} from '@/types/teacherUnavailability'
import type { LoadingState } from '@/types/ui'

const confirm = useConfirm()
const { roles, requireAccessToken } = useAuthSession()

const isTeacherRole = computed(() => {
  return roles.value.includes('TEACHER') && !roles.value.includes('ADMIN') && !roles.value.includes('ACADEMIC_OFFICE')
})

const unavailabilities = ref<TeacherUnavailability[]>([])
const semesters = ref<Semester[]>([])
const teachers = ref<Teacher[]>([])
const selectedSemesterId = ref<number | null>(null)
const selectedTeacherId = ref<number | null>(null)
const selectedStatus = ref<TeacherUnavailabilityStatus | 'ALL'>('ALL')
const loadingState = ref<LoadingState>('loading')
const generalError = ref('')
const dialogError = ref('')
const isDialogVisible = ref(false)
const dialogLoading = ref(false)
const editingUnavailability = ref<TeacherUnavailability | null>(null)

// Reject reason dialog
const isRejectDialogVisible = ref(false)
const rejectReason = ref('')
const rejectingItem = ref<TeacherUnavailability | null>(null)

const statusOptions = [
  { label: 'Tất cả trạng thái', value: 'ALL' },
  { label: 'Chờ duyệt', value: 'PENDING' },
  { label: 'Đã duyệt', value: 'APPROVED' },
  { label: 'Từ chối', value: 'REJECTED' },
  { label: 'Đã rút', value: 'WITHDRAWN' },
]

function getDayLabel(day?: number | null): string {
  if (!day) return ''
  const days: Record<number, string> = {
    2: 'Thứ 2',
    3: 'Thứ 3',
    4: 'Thứ 4',
    5: 'Thứ 5',
    6: 'Thứ 6',
    7: 'Thứ 7',
  }
  return days[day] ?? `Thứ ${day}`
}

function getStatusSeverity(status: TeacherUnavailabilityStatus): 'warn' | 'success' | 'danger' | 'secondary' {
  switch (status) {
    case 'PENDING':
      return 'warn'
    case 'APPROVED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    case 'WITHDRAWN':
      return 'secondary'
  }
}

function getStatusLabel(status: TeacherUnavailabilityStatus): string {
  switch (status) {
    case 'PENDING':
      return 'Chờ duyệt'
    case 'APPROVED':
      return 'Đã duyệt'
    case 'REJECTED':
      return 'Từ chối'
    case 'WITHDRAWN':
      return 'Đã rút'
  }
}

async function loadInitial() {
  const token = requireAccessToken()
  if (!token) return
  loadingState.value = 'loading'
  generalError.value = ''
  try {
    const [semList, teacherList] = await Promise.all([
      fetchSemesters(token),
      fetchTeachers(token),
    ])
    semesters.value = semList
    teachers.value = teacherList
    if (semList.length > 0) {
      selectedSemesterId.value = semList[0].id
    }
    await loadList()
  } catch (err) {
    loadingState.value = 'error'
    generalError.value = extractApiErrorMessage(err, 'Không thể tải dữ liệu khởi tạo')
  }
}

async function loadList() {
  const token = requireAccessToken()
  if (!token) return
  generalError.value = ''
  try {
    const data = await listTeacherUnavailabilities(
      {
        semesterId: selectedSemesterId.value ?? undefined,
        teacherId: isTeacherRole.value ? undefined : (selectedTeacherId.value ?? undefined),
        status: selectedStatus.value === 'ALL' ? undefined : selectedStatus.value,
      },
      token,
    )
    unavailabilities.value = data
    loadingState.value = data.length > 0 ? 'idle' : 'empty'
  } catch (err) {
    loadingState.value = 'error'
    generalError.value = extractApiErrorMessage(err, 'Không thể tải danh sách đăng ký lịch bận')
  }
}

watch([selectedSemesterId, selectedTeacherId, selectedStatus], () => {
  void loadList()
})

function openCreateDialog() {
  editingUnavailability.value = null
  dialogError.value = ''
  isDialogVisible.value = true
}

function openEditDialog(item: TeacherUnavailability) {
  editingUnavailability.value = item
  dialogError.value = ''
  isDialogVisible.value = true
}

async function handleSave(payload: {
  isEdit: boolean
  id?: number
  createPayload?: CreateUnavailabilityPayload
  updatePayload?: UpdateUnavailabilityPayload
}) {
  const token = requireAccessToken()
  if (!token) return
  dialogLoading.value = true
  dialogError.value = ''
  try {
    if (payload.isEdit && payload.id && payload.updatePayload) {
      await updateTeacherUnavailability(payload.id, payload.updatePayload, token)
    } else if (payload.createPayload) {
      await createTeacherUnavailability(payload.createPayload, token)
    }
    isDialogVisible.value = false
    void loadList()
  } catch (err) {
    dialogError.value = extractApiErrorMessage(err, 'Không thể lưu đăng ký lịch bận')
  } finally {
    dialogLoading.value = false
  }
}

function handleWithdraw(item: TeacherUnavailability) {
  confirm.require({
    message: 'Bạn có chắc chắn muốn rút đăng ký lịch bận này?',
    header: 'Xác nhận rút đơn',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Rút đơn',
    rejectLabel: 'Hủy',
    acceptClass: 'p-button-warning',
    accept: async () => {
      const token = requireAccessToken()
      if (!token) return
      try {
        await withdrawTeacherUnavailability(item.id, item.version, token)
        void loadList()
      } catch (err) {
        generalError.value = extractApiErrorMessage(err, 'Không thể rút đơn')
      }
    },
  })
}

function handleApprove(item: TeacherUnavailability) {
  confirm.require({
    message: `Xác nhận duyệt đăng ký lịch bận của giáo viên "${item.teacherName}"?`,
    header: 'Duyệt đăng ký',
    icon: 'pi pi-check-circle',
    acceptLabel: 'Duyệt',
    rejectLabel: 'Hủy',
    accept: async () => {
      const token = requireAccessToken()
      if (!token) return
      try {
        await approveTeacherUnavailability(item.id, item.version, token)
        void loadList()
      } catch (err) {
        generalError.value = extractApiErrorMessage(
          err,
          'Duyệt không thành công. Kiểm tra xem có trùng với thời khóa biểu đã công bố không.',
        )
      }
    },
  })
}

function openRejectDialog(item: TeacherUnavailability) {
  rejectingItem.value = item
  rejectReason.value = ''
  isRejectDialogVisible.value = true
}

async function confirmReject() {
  if (!rejectingItem.value || !rejectReason.value.trim()) return
  const token = requireAccessToken()
  if (!token) return
  try {
    await rejectTeacherUnavailability(
      rejectingItem.value.id,
      {
        expectedVersion: rejectingItem.value.version,
        reason: rejectReason.value.trim(),
      },
      token,
    )
    isRejectDialogVisible.value = false
    void loadList()
  } catch (err) {
    generalError.value = extractApiErrorMessage(err, 'Không thể từ chối đơn')
  }
}

onMounted(() => {
  void loadInitial()
})
</script>

<template>
  <div class="p-6 max-w-7xl mx-auto flex flex-col gap-6">
    <ConfirmDialog />

    <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">
          {{ isTeacherRole ? 'Đăng ký lịch bận giảng dạy' : 'Quản lý lịch bận giáo viên' }}
        </h1>
        <p class="text-sm text-gray-500">
          {{ isTeacherRole
            ? 'Đăng ký các buổi hoặc tiết không thể dạy để tổ học vụ sắp xếp thời khóa biểu'
            : 'Xem và phê duyệt các yêu cầu đăng ký lịch bận từ giáo viên' }}
        </p>
      </div>
      <Button label="Đăng ký lịch bận" icon="pi pi-plus" @click="openCreateDialog" />
    </div>

    <FormAlert v-if="generalError" :message="generalError" type="error" />

    <div class="bg-white p-4 rounded-xl border border-gray-200 shadow-sm flex flex-wrap gap-4 items-center">
      <div class="w-64">
        <Select
          v-model="selectedSemesterId"
          :options="semesters"
          option-label="name"
          option-value="id"
          placeholder="Chọn học kỳ"
          class="w-full"
        />
      </div>
      <div v-if="!isTeacherRole" class="w-64">
        <Select
          v-model="selectedTeacherId"
          :options="teachers"
          option-label="teacherName"
          option-value="id"
          placeholder="Tất cả giáo viên"
          show-clear
          class="w-full"
        />
      </div>
      <div class="w-48">
        <Select
          v-model="selectedStatus"
          :options="statusOptions"
          option-label="label"
          option-value="value"
          class="w-full"
        />
      </div>
    </div>

    <PageState
      v-if="loadingState === 'loading' || loadingState === 'error'"
      :state="loadingState"
      :message="generalError"
      @retry="loadList"
    />

    <div v-else class="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
      <DataTable :value="unavailabilities" responsive-layout="scroll">
        <template #empty>
          <div class="p-8 text-center text-gray-500">Không có đơn đăng ký lịch bận nào</div>
        </template>

        <Column v-if="!isTeacherRole" field="teacherName" header="Giáo viên" style="width: 180px" />
        <Column header="Thời gian bận" style="width: 220px">
          <template #body="{ data }">
            <div class="flex flex-col text-sm">
              <span class="font-medium text-gray-800">
                {{ data.specificDate ? `Ngày ${data.specificDate}` : getDayLabel(data.dayOfWeek) }}
              </span>
              <span class="text-xs text-gray-500">
                Buổi {{ data.session === 'MORNING' ? 'Sáng' : 'Chiều' }} (Tiết {{ data.periodIndexes }})
              </span>
            </div>
          </template>
        </Column>
        <Column header="Hiệu lực" style="width: 180px">
          <template #body="{ data }">
            <span class="text-xs text-gray-600">
              {{ data.validFrom }} → {{ data.validTo }}
            </span>
          </template>
        </Column>
        <Column field="note" header="Ghi chú lý do" />
        <Column header="Trạng thái" style="width: 140px">
          <template #body="{ data }">
            <Tag :value="getStatusLabel(data.status)" :severity="getStatusSeverity(data.status)" />
          </template>
        </Column>
        <Column header="Thao tác" style="width: 160px" class="text-right">
          <template #body="{ data }">
            <div class="flex justify-end gap-1">
              <!-- Actions for Teacher -->
              <template v-if="isTeacherRole">
                <Button
                  v-if="data.status === 'PENDING'"
                  icon="pi pi-pencil"
                  severity="secondary"
                  text
                  rounded
                  title="Sửa"
                  @click="openEditDialog(data)"
                />
                <Button
                  v-if="data.status === 'PENDING' || data.status === 'APPROVED'"
                  icon="pi pi-undo"
                  severity="warning"
                  text
                  rounded
                  title="Rút đơn"
                  @click="handleWithdraw(data)"
                />
              </template>
              <!-- Actions for Admin / Academic Office -->
              <template v-else>
                <Button
                  v-if="data.status === 'PENDING'"
                  icon="pi pi-check"
                  severity="success"
                  text
                  rounded
                  title="Duyệt"
                  @click="handleApprove(data)"
                />
                <Button
                  v-if="data.status === 'PENDING'"
                  icon="pi pi-times"
                  severity="danger"
                  text
                  rounded
                  title="Từ chối"
                  @click="openRejectDialog(data)"
                />
              </template>
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Create/Edit Dialog -->
    <TeacherUnavailabilityDialog
      v-model:visible="isDialogVisible"
      :unavailability="editingUnavailability"
      :semesters="semesters"
      :teachers="teachers"
      :is-teacher-role="isTeacherRole"
      :loading="dialogLoading"
      :error-message="dialogError"
      @save="handleSave"
    />

    <!-- Reject Reason Dialog -->
    <Dialog
      v-model:visible="isRejectDialogVisible"
      header="Từ chối đăng ký lịch bận"
      modal
      :style="{ width: '420px' }"
    >
      <div class="flex flex-col gap-3">
        <p class="text-sm text-gray-700">Vui lòng nhập lý do từ chối để thông báo tới giáo viên:</p>
        <InputText v-model="rejectReason" placeholder="Lý do từ chối..." class="w-full" />
      </div>
      <template #footer>
        <div class="flex justify-end gap-2 mt-4">
          <Button label="Hủy" severity="secondary" text @click="isRejectDialogVisible = false" />
          <Button label="Từ chối đơn" severity="danger" :disabled="!rejectReason.trim()" @click="confirmReject" />
        </div>
      </template>
    </Dialog>
  </div>
</template>
