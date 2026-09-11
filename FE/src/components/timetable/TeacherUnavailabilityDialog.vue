<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'
import DatePicker from 'primevue/datepicker'
import Dialog from 'primevue/dialog'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'

import FormAlert from '@/components/common/FormAlert.vue'
import type { Semester } from '@/types/academic'
import type { Teacher } from '@/types/teacher'
import type {
  CreateUnavailabilityPayload,
  SessionType,
  TeacherUnavailability,
  UpdateUnavailabilityPayload,
} from '@/types/teacherUnavailability'

const props = defineProps<{
  visible: boolean
  unavailability: TeacherUnavailability | null
  semesters: Semester[]
  teachers: Teacher[]
  isTeacherRole?: boolean
  currentTeacherId?: number | null
  loading?: boolean
  errorMessage?: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (
    e: 'save',
    payload: {
      isEdit: boolean
      id?: number
      createPayload?: CreateUnavailabilityPayload
      updatePayload?: UpdateUnavailabilityPayload
    },
  ): void
}>()

const selectedSemesterId = ref<number | null>(null)
const selectedTeacherId = ref<number | null>(null)
const repeatType = ref<'WEEKLY' | 'SPECIFIC_DATE'>('WEEKLY')
const dayOfWeek = ref<number>(2)
const specificDate = ref<Date | null>(null)
const validFrom = ref<Date | null>(new Date())
const validTo = ref<Date | null>(new Date())
const session = ref<SessionType>('MORNING')
const selectedPeriods = ref<number[]>([1, 2])
const note = ref('')
const errors = ref<Record<string, string>>({})

const isEdit = computed(() => Boolean(props.unavailability))

const dayOfWeekOptions = [
  { label: 'Thứ Hai', value: 2 },
  { label: 'Thứ Ba', value: 3 },
  { label: 'Thứ Tư', value: 4 },
  { label: 'Thứ Năm', value: 5 },
  { label: 'Thứ Sáu', value: 6 },
  { label: 'Thứ Bảy', value: 7 },
]

const sessionOptions = [
  { label: 'Buổi Sáng (Tiết 1 - 4)', value: 'MORNING' },
  { label: 'Buổi Chiều (Tiết 1 - 4)', value: 'AFTERNOON' },
]

watch(
  () => props.unavailability,
  (val) => {
    if (val) {
      selectedSemesterId.value = val.semesterId
      selectedTeacherId.value = val.teacherId
      if (val.specificDate) {
        repeatType.value = 'SPECIFIC_DATE'
        specificDate.value = new Date(val.specificDate)
      } else {
        repeatType.value = 'WEEKLY'
        dayOfWeek.value = val.dayOfWeek ?? 2
      }
      validFrom.value = new Date(val.validFrom)
      validTo.value = new Date(val.validTo)
      session.value = val.session
      selectedPeriods.value = val.periodIndexes
        .split(',')
        .map((s) => Number.parseInt(s.trim(), 10))
        .filter((n) => !Number.isNaN(n))
      note.value = val.note ?? ''
    } else {
      selectedSemesterId.value = props.semesters[0]?.id ?? null
      selectedTeacherId.value = props.currentTeacherId ?? props.teachers[0]?.id ?? null
      repeatType.value = 'WEEKLY'
      dayOfWeek.value = 2
      specificDate.value = null
      validFrom.value = new Date()
      validTo.value = new Date()
      session.value = 'MORNING'
      selectedPeriods.value = [1, 2]
      note.value = ''
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
  if (!selectedSemesterId.value) {
    errors.value.semester = 'Vui lòng chọn học kỳ'
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
  if (selectedPeriods.value.length === 0) {
    errors.value.periods = 'Vui lòng chọn ít nhất 1 tiết bận'
  }
  return Object.keys(errors.value).length === 0
}

function handleSave() {
  if (!validate()) return

  const sortedPeriods = [...selectedPeriods.value].sort((a, b) => a - b).join(',')
  const fromStr = formatDateStr(validFrom.value!)
  const toStr = formatDateStr(validTo.value!)
  const specDateStr =
    repeatType.value === 'SPECIFIC_DATE' && specificDate.value
      ? formatDateStr(specificDate.value)
      : null

  if (isEdit.value && props.unavailability) {
    emit('save', {
      isEdit: true,
      id: props.unavailability.id,
      updatePayload: {
        expectedVersion: props.unavailability.version,
        dayOfWeek: repeatType.value === 'WEEKLY' ? dayOfWeek.value : null,
        specificDate: specDateStr,
        validFrom: fromStr,
        validTo: toStr,
        session: session.value,
        periodIndexes: sortedPeriods,
        note: note.value.trim() || null,
      },
    })
  } else {
    emit('save', {
      isEdit: false,
      createPayload: {
        semesterId: selectedSemesterId.value!,
        teacherId: props.isTeacherRole ? undefined : (selectedTeacherId.value ?? undefined),
        dayOfWeek: repeatType.value === 'WEEKLY' ? dayOfWeek.value : null,
        specificDate: specDateStr,
        validFrom: fromStr,
        validTo: toStr,
        session: session.value,
        periodIndexes: sortedPeriods,
        note: note.value.trim() || null,
      },
    })
  }
}
</script>

<template>
  <Dialog
    :visible="visible"
    :header="isEdit ? 'Chỉnh sửa đăng ký lịch bận' : 'Đăng ký lịch bận dạy'"
    modal
    :style="{ width: '560px' }"
    @update:visible="emit('update:visible', $event)"
  >
    <div class="flex flex-col gap-4">
      <FormAlert v-if="errorMessage" :message="errorMessage" type="error" />

      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div class="flex flex-col gap-1">
          <label class="font-medium text-sm">Học kỳ <span class="text-red-500">*</span></label>
          <Select
            v-model="selectedSemesterId"
            :options="semesters"
            option-label="name"
            option-value="id"
            placeholder="Chọn học kỳ"
            :disabled="isEdit"
            :invalid="Boolean(errors.semester)"
          />
          <small v-if="errors.semester" class="text-red-500 text-xs">{{ errors.semester }}</small>
        </div>

        <div v-if="!isTeacherRole" class="flex flex-col gap-1">
          <label class="font-medium text-sm">Giáo viên <span class="text-red-500">*</span></label>
          <Select
            v-model="selectedTeacherId"
            :options="teachers"
            option-label="teacherName"
            option-value="id"
            placeholder="Chọn giáo viên"
            :disabled="isEdit"
          />
        </div>
      </div>

      <div class="flex gap-4 items-center">
        <label class="font-medium text-sm">Hình thức:</label>
        <div class="flex gap-4">
          <label class="flex items-center gap-2 cursor-pointer text-sm">
            <input v-model="repeatType" type="radio" value="WEEKLY"> Lặp theo thứ trong tuần
          </label>
          <label class="flex items-center gap-2 cursor-pointer text-sm">
            <input v-model="repeatType" type="radio" value="SPECIFIC_DATE"> Một ngày cụ thể
          </label>
        </div>
      </div>

      <div v-if="repeatType === 'WEEKLY'" class="flex flex-col gap-1">
        <label class="font-medium text-sm">Thứ trong tuần <span class="text-red-500">*</span></label>
        <Select
          v-model="dayOfWeek"
          :options="dayOfWeekOptions"
          option-label="label"
          option-value="value"
        />
      </div>

      <div v-else class="flex flex-col gap-1">
        <label class="font-medium text-sm">Ngày cụ thể <span class="text-red-500">*</span></label>
        <DatePicker v-model="specificDate" date-format="yy-mm-dd" show-icon />
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

      <div class="flex flex-col gap-1">
        <label class="font-medium text-sm">Buổi trong ngày <span class="text-red-500">*</span></label>
        <Select
          v-model="session"
          :options="sessionOptions"
          option-label="label"
          option-value="value"
        />
      </div>

      <div class="flex flex-col gap-2">
        <label class="font-medium text-sm">Các tiết bận <span class="text-red-500">*</span></label>
        <div class="flex gap-4">
          <div v-for="idx in [1, 2, 3, 4]" :key="idx" class="flex items-center gap-2">
            <Checkbox v-model="selectedPeriods" :input-id="`period-${idx}`" :value="idx" />
            <label :for="`period-${idx}`" class="cursor-pointer text-sm">Tiết {{ idx }}</label>
          </div>
        </div>
        <small v-if="errors.periods" class="text-red-500 text-xs">{{ errors.periods }}</small>
      </div>

      <div class="flex flex-col gap-1">
        <label class="font-medium text-sm">Ghi chú lý do</label>
        <Textarea v-model="note" rows="2" placeholder="Nhập lý do bận (tùy chọn)..." />
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-2 mt-4">
        <Button label="Hủy" severity="secondary" text @click="emit('update:visible', false)" />
        <Button :label="isEdit ? 'Cập nhật' : 'Gửi đăng ký'" :loading="loading" @click="handleSave" />
      </div>
    </template>
  </Dialog>
</template>
