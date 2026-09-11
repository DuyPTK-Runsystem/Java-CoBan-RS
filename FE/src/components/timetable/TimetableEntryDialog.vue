<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Button from 'primevue/button'
import DatePicker from 'primevue/datepicker'
import Dialog from 'primevue/dialog'
import Select from 'primevue/select'

import FormAlert from '@/components/common/FormAlert.vue'
import { useAuthSession } from '@/composables/useAuthSession'
import { getRoomsForSubject } from '@/services/subjectFunctionalRoomApi'
import type { FunctionalRoom } from '@/types/functionalRoom'
import type { TimetableEntry, TimetablePeriod } from '@/types/timetable'

export interface AssignmentOption {
  id: number
  classId: number
  className: string
  subjectId: number
  subjectName: string
  teacherId: number
  teacherName: string
}

const props = defineProps<{
  visible: boolean
  entry: TimetableEntry | null
  presetPeriod: TimetablePeriod | null
  periods: TimetablePeriod[]
  assignments: AssignmentOption[]
  defaultValidFrom?: string
  defaultValidTo?: string
  loading?: boolean
  errorMessage?: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (
    e: 'save',
    payload: {
      id?: number | null
      assignmentId: number
      periodId: number
      functionalRoomId?: number | null
      validFrom: string
      validTo: string
    },
  ): void
  (e: 'delete', entryId: number): void
}>()

const { requireAccessToken } = useAuthSession()

const selectedAssignmentId = ref<number | null>(null)
const selectedPeriodId = ref<number | null>(null)
const selectedRoomId = ref<number | null>(null)
const validFrom = ref<Date | null>(new Date())
const validTo = ref<Date | null>(new Date())
const availableRooms = ref<FunctionalRoom[]>([])
const roomsLoading = ref(false)
const errors = ref<Record<string, string>>({})

const isEdit = computed(() => Boolean(props.entry))

const assignmentOptionsFormatted = computed(() => {
  return props.assignments.map((a) => ({
    id: a.id,
    label: `${a.subjectName} · Lớp ${a.className} · GV: ${a.teacherName}`,
    subjectId: a.subjectId,
  }))
})

const periodOptionsFormatted = computed(() => {
  const days: Record<number, string> = {
    2: 'Thứ 2',
    3: 'Thứ 3',
    4: 'Thứ 4',
    5: 'Thứ 5',
    6: 'Thứ 6',
    7: 'Thứ 7',
  }
  return props.periods.map((p) => ({
    id: p.id,
    label: `${days[p.dayOfWeek] || p.dayOfWeek} · ${p.session === 'MORNING' ? 'Sáng' : 'Chiều'} · Tiết ${p.periodIndex} (${p.startTime} - ${p.endTime})`,
  }))
})

async function fetchRoomsForSubject(subjectId: number) {
  const token = requireAccessToken()
  if (!token) return
  roomsLoading.value = true
  try {
    const res = await getRoomsForSubject(subjectId, token)
    availableRooms.value = res.rooms.filter((r) => r.status === 'ACTIVE')
  } catch {
    availableRooms.value = []
  } finally {
    roomsLoading.value = false
  }
}

watch(
  () => selectedAssignmentId.value,
  (newAssignmentId) => {
    if (!newAssignmentId) {
      availableRooms.value = []
      selectedRoomId.value = null
      return
    }
    const found = props.assignments.find((a) => a.id === newAssignmentId)
    if (found) {
      void fetchRoomsForSubject(found.subjectId)
    }
  },
)

watch(
  () => props.visible,
  (visible) => {
    if (!visible) return
    if (props.entry) {
      selectedAssignmentId.value = props.entry.assignmentId
      selectedPeriodId.value = props.entry.periodId
      selectedRoomId.value = props.entry.functionalRoomId ?? null
      validFrom.value = new Date(props.entry.validFrom)
      validTo.value = new Date(props.entry.validTo)
    } else {
      selectedAssignmentId.value = null
      selectedPeriodId.value = props.presetPeriod?.id ?? props.periods[0]?.id ?? null
      selectedRoomId.value = null
      validFrom.value = props.defaultValidFrom ? new Date(props.defaultValidFrom) : new Date()
      validTo.value = props.defaultValidTo ? new Date(props.defaultValidTo) : new Date()
    }
    errors.value = {}
  },
  { immediate: true },
)

function formatDateStr(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function validate(): boolean {
  errors.value = {}
  if (!selectedAssignmentId.value) {
    errors.value.assignment = 'Vui lòng chọn phân công môn học'
  }
  if (!selectedPeriodId.value) {
    errors.value.period = 'Vui lòng chọn tiết học'
  }
  if (!validFrom.value) {
    errors.value.validFrom = 'Vui lòng chọn ngày bắt đầu'
  }
  if (!validTo.value) {
    errors.value.validTo = 'Vui lòng chọn ngày kết thúc'
  }
  if (validFrom.value && validTo.value && validTo.value < validFrom.value) {
    errors.value.validTo = 'Ngày kết thúc phải sau hoặc bằng ngày bắt đầu'
  }
  return Object.keys(errors.value).length === 0
}

function handleSave() {
  if (!validate()) return
  emit('save', {
    id: props.entry?.id ?? null,
    assignmentId: selectedAssignmentId.value!,
    periodId: selectedPeriodId.value!,
    functionalRoomId: selectedRoomId.value || null,
    validFrom: formatDateStr(validFrom.value!),
    validTo: formatDateStr(validTo.value!),
  })
}

function handleDelete() {
  if (!props.entry) return
  emit('delete', props.entry.id)
}
</script>

<template>
  <Dialog
    :visible="visible"
    :header="isEdit ? 'Chỉnh sửa tiết học' : 'Thêm tiết học mới'"
    modal
    :style="{ width: '560px' }"
    @update:visible="emit('update:visible', $event)"
  >
    <div class="flex flex-col gap-4">
      <FormAlert v-if="errorMessage" :message="errorMessage" type="error" />

      <div class="flex flex-col gap-1">
        <label class="font-medium text-sm">Phân công môn học <span class="text-red-500">*</span></label>
        <Select
          v-model="selectedAssignmentId"
          :options="assignmentOptionsFormatted"
          option-label="label"
          option-value="id"
          placeholder="Chọn môn, lớp, giáo viên..."
          filter
          class="w-full"
          :invalid="Boolean(errors.assignment)"
        />
        <small v-if="errors.assignment" class="text-red-500 text-xs">{{ errors.assignment }}</small>
      </div>

      <div class="flex flex-col gap-1">
        <label class="font-medium text-sm">Thời gian / Tiết học <span class="text-red-500">*</span></label>
        <Select
          v-model="selectedPeriodId"
          :options="periodOptionsFormatted"
          option-label="label"
          option-value="id"
          placeholder="Chọn thứ, buổi và tiết..."
          filter
          class="w-full"
          :invalid="Boolean(errors.period)"
        />
        <small v-if="errors.period" class="text-red-500 text-xs">{{ errors.period }}</small>
      </div>

      <div v-if="availableRooms.length > 0" class="flex flex-col gap-1">
        <label class="font-medium text-sm">Phòng chức năng (nếu có)</label>
        <Select
          v-model="selectedRoomId"
          :options="availableRooms"
          option-label="name"
          option-value="id"
          placeholder="Chọn phòng chức năng..."
          show-clear
          class="w-full"
        />
        <small class="text-xs text-gray-500">Môn học này có phòng thực hành/bộ môn được cấu hình sẵn.</small>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div class="flex flex-col gap-1">
          <label class="font-medium text-sm">Áp dụng từ ngày <span class="text-red-500">*</span></label>
          <DatePicker v-model="validFrom" date-format="yy-mm-dd" show-icon />
        </div>
        <div class="flex flex-col gap-1">
          <label class="font-medium text-sm">Áp dụng đến ngày <span class="text-red-500">*</span></label>
          <DatePicker v-model="validTo" date-format="yy-mm-dd" show-icon :invalid="Boolean(errors.validTo)" />
          <small v-if="errors.validTo" class="text-red-500 text-xs">{{ errors.validTo }}</small>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-between items-center mt-4">
        <div>
          <Button
            v-if="isEdit"
            label="Xóa tiết học"
            severity="danger"
            text
            @click="handleDelete"
          />
        </div>
        <div class="flex gap-2">
          <Button label="Hủy" severity="secondary" text @click="emit('update:visible', false)" />
          <Button :label="isEdit ? 'Lưu thay đổi' : 'Thêm tiết'" :loading="loading" @click="handleSave" />
        </div>
      </div>
    </template>
  </Dialog>
</template>

