<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'

import FormAlert from '@/components/common/FormAlert.vue'
import type { LibraryPatronSummary } from '@/types/library/patron'
import { expiryDateAfterMonths, formatLibraryDate, libraryToday } from '@/utils/libraryCardDates'

const props = withDefaults(defineProps<{
  visible?: boolean
  mode: 'issue' | 'reissue'
  patron: LibraryPatronSummary | null
  saving?: boolean
  errorMessage?: string
}>(), { visible: false, saving: false, errorMessage: '' })
const emit = defineEmits<{
  'update:visible': [visible: boolean]
  submit: [request: { expiresAt: string; reason?: string }]
}>()

const modeOptions = [
  { label: 'Thời hạn theo tháng', value: 'months' },
  { label: 'Chọn ngày hết hạn', value: 'date' },
]
const expiryMode = ref<'months' | 'date'>('months')
const months = ref<number | null>(12)
const directExpiryDate = ref('')
const reason = ref('')
const validationError = ref('')

const calculatedExpiry = computed(() => {
  if (!months.value || !Number.isInteger(months.value) || months.value < 1) return ''
  try { return expiryDateAfterMonths(months.value) } catch { return '' }
})
const selectedExpiry = computed(() => expiryMode.value === 'months' ? calculatedExpiry.value : directExpiryDate.value)
const heading = computed(() => props.mode === 'reissue' ? 'Cấp lại thẻ thư viện' : 'Phát hành thẻ thư viện')

watch(() => props.visible, (visible) => {
  if (!visible) return
  expiryMode.value = 'months'
  months.value = 12
  directExpiryDate.value = ''
  reason.value = ''
  validationError.value = ''
})

function close(): void {
  if (!props.saving) emit('update:visible', false)
}

function submit(): void {
  validationError.value = ''
  if (!selectedExpiry.value || selectedExpiry.value < libraryToday()) {
    validationError.value = 'Ngày hết hạn phải là hôm nay hoặc một ngày trong tương lai.'
    return
  }
  const cleanReason = reason.value.trim()
  if (props.mode === 'reissue' && !cleanReason) {
    validationError.value = 'Vui lòng nhập lý do cấp lại thẻ.'
    return
  }
  emit('submit', { expiresAt: selectedExpiry.value, ...(props.mode === 'reissue' ? { reason: cleanReason } : {}) })
}
</script>

<template>
  <Dialog :visible="props.visible" modal :header="heading" :style="{ width: 'min(100% - 2rem, 620px)' }" :closable="!props.saving" @update:visible="emit('update:visible', $event)">
    <div v-if="props.patron" class="issue-dialog-content">
      <p class="dialog-caption"><strong>{{ props.patron.displayName }}</strong><span>Mã độc giả {{ props.patron.patronId }}</span></p>
      <FormAlert v-if="props.errorMessage" tone="error" :message="props.errorMessage" />
      <FormAlert v-if="validationError" tone="warning" :message="validationError" />
      <div class="field-group">
        <label for="card-expiry-mode">Cách chọn thời hạn <span class="required-mark" aria-hidden="true">*</span></label>
        <Select id="card-expiry-mode" v-model="expiryMode" :options="modeOptions" option-label="label" option-value="value" fluid />
      </div>
      <div v-if="expiryMode === 'months'" class="field-group">
        <label for="card-validity-months">Thời hạn (tháng) <span class="required-mark" aria-hidden="true">*</span></label>
        <InputNumber id="card-validity-months" v-model="months" :min="1" :use-grouping="false" show-buttons fluid :invalid="!calculatedExpiry" />
        <small class="field-hint">Ngày cấp tính theo múi giờ Asia/Ho_Chi_Minh; ngày cuối tháng được giới hạn hợp lệ và ngày hết hạn cuối cùng vẫn có hiệu lực.</small>
      </div>
      <div v-else class="field-group">
        <label for="card-expiry-date">Ngày hết hạn <span class="required-mark" aria-hidden="true">*</span></label>
        <InputText id="card-expiry-date" v-model="directExpiryDate" type="date" :min="libraryToday()" fluid />
      </div>
      <p class="expiry-preview"><i class="pi pi-calendar" aria-hidden="true" />Ngày hết hạn gửi đến máy chủ: <strong>{{ formatLibraryDate(selectedExpiry) }}</strong></p>
      <div v-if="props.mode === 'reissue'" class="field-group">
        <label for="card-reissue-reason">Lý do cấp lại <span class="required-mark" aria-hidden="true">*</span></label>
        <InputText id="card-reissue-reason" v-model="reason" maxlength="500" placeholder="Ví dụ: thẻ bị mất hoặc hư hỏng" fluid />
      </div>
      <div class="form-actions">
        <Button type="button" label="Hủy" icon="pi pi-times" severity="secondary" outlined :disabled="props.saving" @click="close" />
        <Button type="button" :label="props.mode === 'reissue' ? 'Cấp lại thẻ' : 'Phát hành thẻ'" icon="pi pi-check" :loading="props.saving" @click="submit" />
      </div>
    </div>
  </Dialog>
</template>

<style scoped>
.issue-dialog-content { display: grid; gap: 1rem; }
.dialog-caption { display: flex; justify-content: space-between; gap: 1rem; margin: 0; color: var(--text-color-secondary); }
.dialog-caption strong { color: var(--text-color); }
.field-hint { color: var(--text-color-secondary); line-height: 1.5; }
.expiry-preview { display: flex; align-items: center; gap: .45rem; margin: 0; padding: .7rem .85rem; border-radius: .6rem; background: #eef2ff; color: #3730a3; }
.form-actions { display: flex; justify-content: flex-end; gap: .7rem; margin-top: .35rem; }
@media (max-width: 520px) { .dialog-caption { flex-direction: column; gap: .2rem; } .form-actions { flex-wrap: wrap; } }
</style>
