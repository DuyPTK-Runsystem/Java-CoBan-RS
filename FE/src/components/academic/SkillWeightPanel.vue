<script setup lang="ts">
import { ref, watch } from 'vue'
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'

import type { SkillWeightConfig, UpsertSkillWeightRequest } from '@/types/scorebook'

const props = defineProps<{
  config: SkillWeightConfig | null
  readOnly?: boolean
  saving?: boolean
  errorMessage?: string
}>()
const emit = defineEmits<{ save: [request: UpsertSkillWeightRequest] }>()

const kttt = ref<number | null>(null)
const ktdk = ref<number | null>(null)
const ktck = ref<number | null>(null)
const validationMessage = ref('')

watch(() => props.config, (config) => {
  kttt.value = config?.ktttWeightPercent ?? null
  ktdk.value = config?.ktdkWeightPercent ?? null
  ktck.value = config?.ktckWeightPercent ?? null
  validationMessage.value = ''
}, { immediate: true })

function save(): void {
  validationMessage.value = ''
  const weights = [kttt.value, ktdk.value, ktck.value]
  if (weights.some((weight) => weight === null || weight < 0 || weight > 100)) {
    validationMessage.value = 'Mỗi trọng số phải nằm trong khoảng 0–100.'
    return
  }
  const request = {
    ktttWeightPercent: kttt.value as number,
    ktdkWeightPercent: ktdk.value as number,
    ktckWeightPercent: ktck.value as number,
  }
  if (Math.abs(request.ktttWeightPercent + request.ktdkWeightPercent + request.ktckWeightPercent - 100) > 0.001) {
    validationMessage.value = 'Tổng ba trọng số phải bằng 100.'
    return
  }
  if (request.ktckWeightPercent < request.ktttWeightPercent
    || request.ktckWeightPercent < request.ktdkWeightPercent) {
    validationMessage.value = 'Trọng số KTCK phải lớn hơn hoặc bằng KTTT và KTĐK.'
    return
  }
  emit('save', request)
}
</script>

<template>
  <section class="content-surface skill-weight-panel">
    <div class="section-heading">
      <div>
        <h2>Trọng số môn kỹ năng</h2>
        <p class="section-caption">Tổng bằng 100%; KTCK không nhỏ hơn KTTT hoặc KTĐK.</p>
      </div>
      <span v-if="props.config?.lockedAt" class="field-hint">Đã khóa</span>
    </div>
    <div v-if="validationMessage || props.errorMessage" class="form-alert form-alert-error" role="alert">{{ validationMessage || props.errorMessage }}</div>
    <div class="form-grid-three">
      <div class="field-group">
        <label for="skill-weight-kttt">KTTT (%)</label>
        <InputNumber id="skill-weight-kttt" v-model="kttt" :min="0" :max="100" :max-fraction-digits="2" :disabled="props.readOnly" fluid />
      </div>
      <div class="field-group">
        <label for="skill-weight-ktdk">KTĐK (%)</label>
        <InputNumber id="skill-weight-ktdk" v-model="ktdk" :min="0" :max="100" :max-fraction-digits="2" :disabled="props.readOnly" fluid />
      </div>
      <div class="field-group">
        <label for="skill-weight-ktck">KTCK (%)</label>
        <InputNumber id="skill-weight-ktck" v-model="ktck" :min="0" :max="100" :max-fraction-digits="2" :disabled="props.readOnly" fluid />
      </div>
    </div>
    <div class="form-actions">
      <Button label="Lưu trọng số" icon="pi pi-check" :loading="props.saving" :disabled="props.readOnly || props.saving" @click="save" />
    </div>
  </section>
</template>

<style scoped>
.skill-weight-panel { margin-top: 20px; }
.form-grid-three { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
@media (max-width: 680px) { .form-grid-three { grid-template-columns: 1fr; } }
</style>

