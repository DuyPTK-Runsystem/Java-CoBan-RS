<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'
import InputNumber from 'primevue/inputnumber'
import MultiSelect from 'primevue/multiselect'
import Textarea from 'primevue/textarea'
import FormAlert from '@/components/common/FormAlert.vue'
import type { TimetableEntry } from '@/types/timetable'
import type { TimetableAgentAssignmentOption, TimetableAgentRequest } from '@/types/timetableAgent'

const props = defineProps<{
  targetRevisionId: number
  expectedVersion: number
  defaultValidFrom: string
  defaultValidTo: string
  classes: { id: number; name: string }[]
  assignments: TimetableAgentAssignmentOption[]
  existingEntries: TimetableEntry[]
  canGenerate: boolean
  busy: boolean
  assignmentsLoading?: boolean
}>()
const emit = defineEmits<{
  generate: [request: TimetableAgentRequest]
  inputChanged: []
  classesChanged: [classIds: number[]]
}>()
const classIds = ref<number[]>([])
const validFrom = ref(props.defaultValidFrom)
const validTo = ref(props.defaultValidTo)
const demands = ref<Record<number, number | null>>({})
const lockedEntryIds = ref<number[]>([])
const preferences = ref('')
const userRequest = ref('')
const demandConfirmed = ref(false)
const formError = ref('')
const scopedAssignments = computed(() => props.assignments.filter((a) => classIds.value.includes(a.classId)))
const scopedEntries = computed(() => props.existingEntries.filter((e) => classIds.value.includes(e.classId)
  && e.validFrom >= validFrom.value && e.validTo <= validTo.value
  && (e.entryId ?? e.id) !== undefined))
const lockedOptions = computed(() => scopedEntries.value.map((e) => ({
  id: e.entryId ?? e.id,
  label: `${e.className} · ${e.subjectName} · ${e.session === 'MORNING' ? 'Sáng' : 'Chiều'} tiết ${e.periodIndex} · ${e.validFrom} → ${e.validTo}`,
})))
const valid = computed(() => classIds.value.length > 0 && validFrom.value && validTo.value >= validFrom.value
  && scopedAssignments.value.length > 0 && demandConfirmed.value
  && scopedAssignments.value.every((a) => Number.isInteger(demands.value[a.id]) && (demands.value[a.id] ?? 0) > 0))

watch(classIds, (ids) => emit('classesChanged', [...ids]), { deep: true })
watch([classIds, validFrom, validTo, demands, lockedEntryIds, preferences, userRequest], () => {
  demandConfirmed.value = false
  formError.value = ''
  emit('inputChanged')
}, { deep: true, flush: 'sync' })
watch(demandConfirmed, () => emit('inputChanged'), { flush: 'sync' })
watch([scopedAssignments, scopedEntries], () => {
  lockedEntryIds.value = lockedEntryIds.value.filter((id) => scopedEntries.value.some((e) => (e.entryId ?? e.id) === id))
})

function submit() {
  if (props.busy || props.assignmentsLoading || !props.canGenerate) return
  if (!valid.value) {
    formError.value = 'Chọn lớp, khoảng ngày và xác nhận số tiết cho từng phân công.'
    return
  }
  emit('generate', {
    targetRevisionId: props.targetRevisionId,
    expectedVersion: props.expectedVersion,
    classIds: [...classIds.value], validFrom: validFrom.value, validTo: validTo.value,
    demands: scopedAssignments.value.map((a) => ({ assignmentId: a.id, periodsPerWeek: demands.value[a.id]! })),
    lockedEntryIds: [...lockedEntryIds.value], preferences: preferences.value, userRequest: userRequest.value,
  })
}
</script>

<template>
  <form class="agent-input" @submit.prevent="submit">
    <h3>Yêu cầu xếp lịch</h3>
    <FormAlert v-if="!canGenerate" tone="info" message="Gợi ý thời khoá biểu hiện chưa khả dụng. Bạn vẫn có thể xếp lịch thủ công." />
    <FormAlert v-if="formError" :message="formError" />
    <fieldset :disabled="busy" class="agent-fields">
      <label for="agent-classes">Lớp cần xếp</label>
      <MultiSelect v-model="classIds" input-id="agent-classes" :options="classes" option-label="name" option-value="id" :disabled="busy" placeholder="Chọn lớp" display="chip" />
      <div class="agent-dates">
        <div><label for="agent-from">Áp dụng từ</label><input id="agent-from" v-model="validFrom" type="date" :min="defaultValidFrom" :max="defaultValidTo || undefined" required></div>
        <div><label for="agent-to">Đến ngày</label><input id="agent-to" v-model="validTo" type="date" :min="validFrom || defaultValidFrom" :max="defaultValidTo || undefined" required></div>
      </div>
      <h4>Số tiết yêu cầu mỗi tuần</h4>
      <p v-if="assignmentsLoading" role="status">Đang tải phân công…</p>
      <p v-else-if="!scopedAssignments.length">Chọn lớp có phân công đang hoạt động.</p>
      <div v-for="a in scopedAssignments" :key="a.id" class="agent-demand">
        <label :for="`agent-demand-${a.id}`">{{ a.className }} · {{ a.subjectName }} · {{ a.teacherName }}</label>
        <InputNumber v-model="demands[a.id]" :input-id="`agent-demand-${a.id}`" :min="1" :use-grouping="false" :disabled="busy" />
      </div>
      <div class="agent-confirm"><Checkbox v-model="demandConfirmed" input-id="agent-demand-confirm" binary :disabled="busy || !scopedAssignments.length" /><label for="agent-demand-confirm">Tôi xác nhận bảng số tiết trên.</label></div>
      <label for="agent-locked">Tiết giữ nguyên</label>
      <MultiSelect v-model="lockedEntryIds" input-id="agent-locked" :options="lockedOptions" option-label="label" option-value="id" :disabled="busy" placeholder="Chọn tiết cần giữ" />
      <label for="agent-preferences">Ưu tiên</label><Textarea id="agent-preferences" v-model="preferences" input-id="agent-preferences" :disabled="busy" rows="3" />
      <label for="agent-request">Yêu cầu bổ sung</label><Textarea id="agent-request" v-model="userRequest" :disabled="busy" rows="2" />
    </fieldset>
    <p class="agent-help">Lịch bận đã duyệt, phân công, phòng và định mức được kiểm tra theo dữ liệu của trường. Mọi thay đổi yêu cầu cần duyệt lại phương án.</p>
    <Button type="submit" label="Tạo gợi ý" :loading="busy" :disabled="busy || assignmentsLoading || !canGenerate || !valid" />
  </form>
</template>

<style scoped>
.agent-input{min-width:0;padding:1.25rem;border:1px solid #dce4e9;border-radius:12px;background:white}.agent-input h3{margin-top:0}.agent-fields{box-sizing:border-box;width:100%;max-width:100%;border:0;padding:0;min-width:0;display:grid;gap:.7rem}.agent-fields label{font-size:.875rem;font-weight:600}.agent-fields :deep(.p-multiselect),.agent-fields :deep(.p-inputnumber),.agent-fields :deep(.p-textarea){box-sizing:border-box;width:100%;max-width:100%;min-width:0}.agent-dates{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:.75rem;min-width:0}.agent-dates>div{min-width:0}.agent-dates label{display:block;margin-bottom:.5rem}.agent-dates input{box-sizing:border-box;width:100%;min-width:0;padding:.55rem;border:1px solid #b8c8d1;border-radius:6px;font:inherit}.agent-demand{display:grid;gap:.35rem;min-width:0}.agent-confirm{display:flex;gap:.6rem;align-items:center}.agent-help{font-size:.875rem;color:#536773;line-height:1.5}.agent-fields :deep(.p-inputnumber-input){box-sizing:border-box;width:100%;min-width:0}
</style>
