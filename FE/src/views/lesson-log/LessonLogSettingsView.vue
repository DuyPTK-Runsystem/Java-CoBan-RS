<script setup lang="ts">
import { onMounted, ref } from 'vue'
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'

import LessonLogSubtabs from '@/components/lesson-log/LessonLogSubtabs.vue'
import FormAlert from '@/components/common/FormAlert.vue'

import { getLessonLogPolicy, updateLessonLogPolicy } from '@/services/lessonLogApi'
import { useAuthSession } from '@/composables/useAuthSession'
import { extractApiErrorMessage } from '@/types/api'
import type { DeadlineMode, LessonLogPolicy } from '@/types/lessonLog'

const { requireAccessToken } = useAuthSession()

const policy = ref<LessonLogPolicy | null>(null)
const mode = ref<DeadlineMode>('FIXED_HOURS')
const hours = ref<number | null>(48)
const effectiveFrom = ref('')
const timezone = ref('Asia/Ho_Chi_Minh')
const requireReview = ref(true)
const reason = ref('')
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const notice = ref('')

const modes = [
  { label: 'Số giờ từ khi kết thúc tiết', value: 'FIXED_HOURS' },
  { label: 'Đến hết tuần', value: 'END_OF_WEEK' },
]

function hcmToday(): string {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Ho_Chi_Minh',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date())
  const part = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((value) => value.type === type)?.value ?? ''
  return `${part('year')}-${part('month')}-${part('day')}`
}

function nextEffectiveFrom(latestEffectiveFrom: string): string {
  const today = hcmToday()
  const latestDate = latestEffectiveFrom > today ? latestEffectiveFrom : today
  const [year, month, day] = latestDate.split('-').map(Number)
  const nextDate = new Date(Date.UTC(year, month - 1, day + 1))
  return `${nextDate.getUTCFullYear()}-${String(nextDate.getUTCMonth() + 1).padStart(2, '0')}-${String(nextDate.getUTCDate()).padStart(2, '0')}`
}

async function load(): Promise<void> {
  const token = requireAccessToken()
  if (!token) return
  loading.value = true
  error.value = ''
  try {
    policy.value = await getLessonLogPolicy(undefined, token)
    mode.value = policy.value.deadlineMode
    hours.value = policy.value.editWindowHours
    effectiveFrom.value = nextEffectiveFrom(policy.value.effectiveFrom)
    timezone.value = policy.value.timezone
    requireReview.value = policy.value.requireHomeroomReview
  } catch (e) {
    error.value = extractApiErrorMessage(e, 'Không thể tải chính sách')
  } finally {
    loading.value = false
  }
}

async function save(): Promise<void> {
  const token = requireAccessToken()
  if (!token || !policy.value) return
  if (!reason.value.trim()) {
    error.value = 'Lý do thay đổi là bắt buộc.'
    notice.value = ''
    return
  }
  saving.value = true
  error.value = ''
  try {
    policy.value = await updateLessonLogPolicy(
      {
        expectedVersion: policy.value.version,
        effectiveFrom: effectiveFrom.value,
        timezone: timezone.value,
        deadlineMode: mode.value,
        editWindowHours: mode.value === 'FIXED_HOURS' ? hours.value : null,
        requireHomeroomReview: requireReview.value,
        rubric: policy.value.rubric,
        reason: reason.value.trim(),
      },
      token,
    )
    effectiveFrom.value = nextEffectiveFrom(policy.value.effectiveFrom)
    notice.value = 'Đã tạo phiên bản chính sách mới.'
    reason.value = ''
  } catch (e) {
    error.value = extractApiErrorMessage(e, 'Không thể lưu chính sách')
  } finally {
    saving.value = false
  }
}

onMounted(() => void load())
</script>

<template>
  <div class="page-heading lesson-log-page-heading">
    <div>
      <h1>Chính sách sổ đầu bài</h1>
      <p class="section-caption">Cấu hình thời hạn ghi, sửa và quy định ký duyệt sổ đầu bài</p>
    </div>
    <div class="page-heading-actions">
      <Button
        label="Làm mới"
        icon="pi pi-refresh"
        severity="secondary"
        outlined
        :loading="loading"
        @click="load"
      />
    </div>
  </div>

  <LessonLogSubtabs show-policy />

  <FormAlert v-if="error" tone="error" :message="error" />
  <FormAlert v-if="notice" tone="success" :message="notice" />

  <!-- Loading State -->
  <div v-if="loading" class="page-state page-state-loading" role="status">
    <i class="pi pi-spin pi-spinner" aria-hidden="true" />
    <span>Đang tải chính sách…</span>
  </div>

  <!-- Policy Form Surface -->
  <section v-else-if="policy" class="content-surface lesson-log-settings-surface">
    <form class="lesson-log-settings-form" @submit.prevent="save">
      <div class="field-group">
        <label for="policy-deadline-mode">Kiểu thời hạn cho phép ghi/sửa</label>
        <Select
          id="policy-deadline-mode"
          v-model="mode"
          :options="modes"
          option-label="label"
          option-value="value"
          fluid
        />
      </div>

      <div v-if="mode === 'FIXED_HOURS'" class="field-group">
        <label for="policy-hours">Số giờ kể từ khi kết thúc tiết học</label>
        <InputNumber
          id="policy-hours"
          v-model="hours"
          :min="1"
          :max="168"
          suffix=" giờ"
          fluid
        />
      </div>

      <div class="field-group">
        <label for="policy-effective-date">Ngày bắt đầu hiệu lực</label>
        <InputText
          id="policy-effective-date"
          v-model="effectiveFrom"
          type="date"
          fluid
        />
      </div>

      <div class="field-group">
        <label for="policy-timezone">Múi giờ hệ thống</label>
        <InputText
          id="policy-timezone"
          v-model="timezone"
          readonly
          disabled
          fluid
        />
      </div>

      <div class="field-group">
        <label class="lesson-log-checkbox-label">
          <input v-model="requireReview" type="checkbox" />
          <span>Bắt buộc giáo viên chủ nhiệm ký tổng kết tuần</span>
        </label>
      </div>

      <div class="field-group">
        <label for="policy-reason">
          Lý do thay đổi chính sách <span class="required-mark" aria-hidden="true">*</span>
        </label>
        <InputText
          id="policy-reason"
          v-model="reason"
          required
          aria-required="true"
          maxlength="500"
          placeholder="Nêu lý do tạo phiên bản chính sách mới"
          fluid
        />
      </div>

      <div class="pt-2">
        <Button
          type="submit"
          label="Tạo phiên bản chính sách mới"
          icon="pi pi-save"
          :loading="saving"
          :disabled="!reason.trim()"
        />
      </div>
    </form>
  </section>
</template>

<style scoped>
.required-mark {
  color: var(--error, #ba1a1a);
}

.pt-2 {
  padding-top: 8px;
}
</style>
