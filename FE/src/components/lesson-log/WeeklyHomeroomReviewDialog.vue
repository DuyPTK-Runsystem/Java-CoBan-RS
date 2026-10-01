<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Textarea from 'primevue/textarea'
import Select from 'primevue/select'
import LessonLogStatusBadge from './LessonLogStatusBadge.vue'
import type { LessonGrade, WeeklyReview } from '@/types/lessonLog'

const props = defineProps<{
  visible: boolean
  review: WeeklyReview
}>()

const emit = defineEmits<{
  'update:visible': [boolean]
  sign: [string, LessonGrade | null, string]
}>()

const comment = ref('')
const reason = ref('')
const grade = ref<LessonGrade | null>(null)

const grades = [
  { label: 'A · Tốt', value: 'A' },
  { label: 'B · Khá', value: 'B' },
  { label: 'C · Trung bình', value: 'C' },
  { label: 'D · Yếu', value: 'D' },
]

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      comment.value = props.review.comment ?? ''
      grade.value = props.review.grade
      reason.value = ''
    }
  },
)

const requiresReason = computed(
  () => props.review.requiresReason || props.review.status === 'STALE',
)

function sign(): void {
  if (requiresReason.value && !reason.value.trim()) return
  emit('sign', comment.value.trim(), grade.value, reason.value.trim())
}
</script>

<template>
  <Dialog
    :visible="props.visible"
    modal
    header="Ký tổng kết tuần"
    :style="{ width: 'min(580px, 96vw)' }"
    @update:visible="emit('update:visible', $event)"
  >
    <div class="review-form">
      <div class="review-status-bar">
        <LessonLogStatusBadge :status="props.review.status" />
        <span v-if="props.review.signedBy" class="review-signer">
          {{ props.review.signedBy }} · {{ props.review.signedAt }}
        </span>
      </div>

      <div v-if="props.review.blockedReasons.length" class="review-blocked-reasons">
        <strong>Chưa đủ điều kiện ký:</strong>
        <ul>
          <li v-for="item in props.review.blockedReasons" :key="item">{{ item }}</li>
        </ul>
      </div>

      <div class="field-group">
        <label for="weekly-comment">Nhận xét tổng kết tuần của GVCN</label>
        <Textarea
          id="weekly-comment"
          v-model="comment"
          rows="4"
          maxlength="4000"
          placeholder="Nhận xét chung về nề nếp, học tập của lớp trong tuần..."
        />
      </div>

      <div class="field-group">
        <label for="weekly-grade">Xếp loại tuần của lớp</label>
        <Select
          id="weekly-grade"
          v-model="grade"
          :options="grades"
          option-label="label"
          option-value="value"
          placeholder="Chọn xếp loại tuần"
        />
      </div>

      <div v-if="requiresReason" class="field-group">
        <label for="weekly-reason">
          Lý do ký lại tuần <span class="required-mark" aria-hidden="true">*</span>
        </label>
        <Textarea
          id="weekly-reason"
          v-model="reason"
          rows="3"
          maxlength="500"
          required
          aria-required="true"
          placeholder="Nêu rõ lý do ký lại tuần sau khi có thay đổi tiết học"
        />
      </div>

      <div class="dialog-actions">
        <Button
          label="Đóng"
          severity="secondary"
          text
          @click="emit('update:visible', false)"
        />
        <Button
          label="Ký tuần"
          icon="pi pi-check"
          :disabled="!props.review.canSignWeek || (requiresReason && !reason.trim())"
          @click="sign"
        />
      </div>
    </div>
  </Dialog>
</template>

<style scoped>
.review-form {
  display: grid;
  gap: 16px;
}

.review-status-bar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.review-signer {
  font-size: 13px;
  color: #64748b;
}

.review-blocked-reasons {
  padding: 10px 14px;
  border-radius: 8px;
  background: #fef2f2;
  border-left: 3px solid var(--error, #ba1a1a);
  color: var(--error, #ba1a1a);
  font-size: 13px;
}

.review-blocked-reasons ul {
  margin: 6px 0 0;
  padding-left: 18px;
}

.required-mark {
  color: var(--error, #ba1a1a);
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}
</style>
