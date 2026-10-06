<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'
import InputNumber from 'primevue/inputnumber'
import MultiSelect from 'primevue/multiselect'
import Textarea from 'primevue/textarea'
import FormAlert from '@/components/common/FormAlert.vue'
import { getIsoWeekdayLabel } from '@/utils/isoWeekday'
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
const editedDemands = new Set<number>()
const lockedEntryIds = ref<number[]>([])
const preferences = ref('')
const userRequest = ref('')
const demandConfirmed = ref(false)
const formError = ref('')
const scopedAssignments = computed(() => props.assignments.filter((a) => classIds.value.includes(a.classId)))
const scopedEntries = computed(() => props.existingEntries.filter((e) => classIds.value.includes(e.classId)
  && e.validFrom >= validFrom.value && e.validTo <= validTo.value
  && (e.entryId ?? e.id) !== undefined))
const lockedOptions = computed(() => scopedEntries.value.map((e) => {
  const weekdayLabel = e.dayOfWeek ? getIsoWeekdayLabel(e.dayOfWeek) : ''
  const weekdayPart = weekdayLabel && weekdayLabel !== 'Không xác định' ? `${weekdayLabel} · ` : ''
  const sessionPart = e.session === 'MORNING' ? 'Sáng' : 'Chiều'
  return {
    id: e.entryId ?? e.id,
    label: `${e.className} · ${e.subjectName} · ${weekdayPart}${sessionPart} tiết ${e.periodIndex} · ${e.validFrom} → ${e.validTo}`,
  }
}))
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

function editDemand(assignmentId: number, value: number | null) {
  editedDemands.add(assignmentId)
  demands.value[assignmentId] = value
}

const demandClasses = computed(() => {
  return classIds.value.map((id) => {
    const fromProps = props.classes.find((c) => c.id === id)
    if (fromProps) return fromProps
    const fromAssignments = scopedAssignments.value.find((a) => a.classId === id)
    return { id, name: fromAssignments?.className ?? `Lớp #${id}` }
  })
})

const demandSubjects = computed(() => {
  const set = new Set<string>()
  const list: string[] = []
  for (const a of scopedAssignments.value) {
    if (a.subjectName && !set.has(a.subjectName)) {
      set.add(a.subjectName)
      list.push(a.subjectName)
    }
  }
  return list
})

const assignmentGrid = computed(() => {
  const map = new Map<string, TimetableAgentAssignmentOption[]>()
  for (const a of scopedAssignments.value) {
    const key = `${a.classId}:${a.subjectName}`
    const list = map.get(key) ?? []
    list.push(a)
    map.set(key, list)
  }
  return map
})

function getCellAssignments(classId: number, subjectName: string): TimetableAgentAssignmentOption[] {
  return assignmentGrid.value.get(`${classId}:${subjectName}`) ?? []
}

function handleHeaderToggleAll(event: MouseEvent) {
  const target = event.target as HTMLElement | null
  if (target?.closest('.p-checkbox')) return
  const current = event.currentTarget as HTMLElement | null
  const input = current?.querySelector<HTMLInputElement>('input[type="checkbox"]')
  input?.click()
}

watch([scopedAssignments, () => props.existingEntries, validFrom, validTo], () => {
  const next = { ...demands.value }
  for (const assignment of scopedAssignments.value) {
    if (editedDemands.has(assignment.id)) continue
    const rows = props.existingEntries.filter((entry) => entry.revisionId === props.targetRevisionId
      && entry.assignmentId === assignment.id && entry.classId === assignment.classId
      && entry.validTo >= validFrom.value && (!validTo.value || entry.validFrom <= validTo.value))
    // Count one weekly pattern rather than adding successive effective schedules.
    const firstDate = rows.reduce((date, entry) => {
      const start = entry.validFrom > validFrom.value ? entry.validFrom : validFrom.value
      return date === '' || start < date ? start : date
    }, '')
    const slots = new Set(rows.filter((entry) => entry.validFrom <= firstDate && entry.validTo >= firstDate)
      .map((entry) => `${entry.dayOfWeek}:${entry.session}:${entry.periodIndex}`))
    next[assignment.id] = slots.size || null
  }
  if (Object.keys(next).some((id) => next[Number(id)] !== demands.value[Number(id)])) demands.value = next
}, { immediate: true, deep: true })

watch(() => props.targetRevisionId, () => {
  editedDemands.clear()
  demands.value = {}
  classIds.value = []
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
      <MultiSelect
        v-model="classIds"
        input-id="agent-classes"
        :options="classes"
        option-label="name"
        option-value="id"
        :disabled="busy"
        placeholder="Chọn lớp"
        display="chip"
        panel-class="agent-multiselect-panel"
        overlay-class="agent-multiselect-panel"
        :pt="{ header: { onClick: handleHeaderToggleAll } }"
      />
      <div class="agent-dates">
        <div><label for="agent-from">Áp dụng từ</label><input id="agent-from" v-model="validFrom" type="date" :min="defaultValidFrom" :max="defaultValidTo || undefined" required></div>
        <div><label for="agent-to">Đến ngày</label><input id="agent-to" v-model="validTo" type="date" :min="validFrom || defaultValidFrom" :max="defaultValidTo || undefined" required></div>
      </div>
      <h4>Số tiết yêu cầu mỗi tuần</h4>
      <p v-if="assignmentsLoading" role="status">Đang tải phân công…</p>
      <p v-else-if="!scopedAssignments.length">Chọn lớp có phân công đang hoạt động.</p>
      <div v-else class="agent-demand-table-wrap">
        <table class="agent-demand-table">
          <caption class="sr-only">Bảng số tiết yêu cầu mỗi tuần theo lớp và môn học</caption>
          <thead>
            <tr>
              <th scope="col" class="agent-demand-class-th">Lớp</th>
              <th v-for="s in demandSubjects" :key="s" scope="col">{{ s }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in demandClasses" :key="c.id">
              <td class="agent-demand-class-td">{{ c.name }}</td>
              <td v-for="s in demandSubjects" :key="`${c.id}-${s}`">
                <div v-for="a in getCellAssignments(c.id, s)" :key="a.id" class="agent-demand-cell">
                  <label :for="`agent-demand-${a.id}`" class="sr-only">{{ c.name }} · {{ s }} · {{ a.teacherName }}</label>
                  <InputNumber
                    :model-value="demands[a.id]"
                    :input-id="`agent-demand-${a.id}`"
                    :min="1"
                    :use-grouping="false"
                    :disabled="busy"
                    @update:model-value="editDemand(a.id, $event)"
                  />
                  <small class="agent-demand-teacher" :title="a.teacherName">{{ a.teacherName }}</small>
                </div>
                <span v-if="!getCellAssignments(c.id, s).length" class="agent-demand-empty">—</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <label for="agent-locked">Tiết giữ nguyên</label>
      <MultiSelect
        v-model="lockedEntryIds"
        input-id="agent-locked"
        :options="lockedOptions"
        option-label="label"
        option-value="id"
        :disabled="busy"
        placeholder="Chọn tiết cần giữ"
        panel-class="agent-multiselect-panel"
        overlay-class="agent-multiselect-panel"
        :pt="{ header: { onClick: handleHeaderToggleAll } }"
      />
      <label for="agent-preferences">Ưu tiên</label><Textarea id="agent-preferences" v-model="preferences" input-id="agent-preferences" :disabled="busy" rows="3" />
      <label for="agent-request">Yêu cầu bổ sung</label><Textarea id="agent-request" v-model="userRequest" :disabled="busy" rows="2" />
      <div class="agent-confirm"><Checkbox v-model="demandConfirmed" input-id="agent-demand-confirm" binary :disabled="busy || !scopedAssignments.length" /><label for="agent-demand-confirm">Tôi xác nhận bảng số tiết trên.</label></div>
    </fieldset>
    <p class="agent-help">Lịch bận đã duyệt, phân công, phòng và định mức được kiểm tra theo dữ liệu của trường. Mọi thay đổi yêu cầu cần duyệt lại phương án.</p>
    <Button type="submit" label="Tạo gợi ý" :loading="busy" :disabled="busy || assignmentsLoading || !canGenerate || !valid" />
  </form>
</template>

<style scoped>
.agent-input{min-width:0;padding:1.25rem;border:1px solid #dce4e9;border-radius:12px;background:white}.agent-input h3{margin-top:0}.agent-fields{box-sizing:border-box;width:100%;max-width:100%;border:0;padding:0;min-width:0;display:grid;gap:.7rem}.agent-fields label{font-size:.875rem;font-weight:600}.agent-fields :deep(.p-multiselect),.agent-fields :deep(.p-inputnumber),.agent-fields :deep(.p-textarea){box-sizing:border-box;width:100%;max-width:100%;min-width:0}.agent-dates{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:.75rem;min-width:0}.agent-dates>div{min-width:0}.agent-dates label{display:block;margin-bottom:.5rem}.agent-dates input{box-sizing:border-box;width:100%;min-width:0;padding:.55rem;border:1px solid #b8c8d1;border-radius:6px;font:inherit}.agent-confirm{display:flex;gap:.6rem;align-items:center}.agent-help{font-size:.875rem;color:#536773;line-height:1.5}.agent-fields :deep(.p-inputnumber-input){box-sizing:border-box;width:100%;min-width:0}
.agent-demand-table-wrap{overflow-x:auto;border:1px solid #dce4e9;border-radius:8px;background:white}
.agent-demand-table{width:100%;border-collapse:collapse;font-size:.875rem}
.agent-demand-table th,.agent-demand-table td{border:1px solid #dce4e9;padding:.5rem .6rem;text-align:center;vertical-align:middle}
.agent-demand-table th{background:#f5f8fa;font-weight:600;white-space:nowrap}
.agent-demand-table th.agent-demand-class-th{text-align:left;min-width:100px;position:sticky;left:0;z-index:2;background:#f5f8fa}
.agent-demand-table td.agent-demand-class-td{text-align:left;font-weight:600;background:#fafbfc;white-space:nowrap;position:sticky;left:0;z-index:1}
.agent-demand-cell{display:flex;flex-direction:column;align-items:center;gap:.25rem;min-width:70px}
.agent-demand-cell :deep(.p-inputnumber){width:100%;max-width:75px}
.agent-demand-cell :deep(.p-inputnumber-input){text-align:center;padding:.35rem .5rem}
.agent-demand-teacher{font-size:.75rem;color:#536773;line-height:1.2;text-align:center;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:110px;display:block}
.agent-demand-empty{color:#94a3b8;font-size:1rem}
.sr-only{position:absolute;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;border:0}
:deep(.agent-multiselect-panel .p-multiselect-header){display:flex;align-items:center;gap:.5rem;cursor:pointer;user-select:none}
:deep(.agent-multiselect-panel .p-multiselect-header::after){content:'Tất cả';font-size:.875rem;font-weight:500;color:#374151;transition:color .15s ease}
:deep(.agent-multiselect-panel .p-multiselect-header:hover::after){color:#111827}
</style>
