<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'
import Dropdown from 'primevue/dropdown'
import InputText from 'primevue/inputtext'
import { useAuthSession } from '@/composables/useAuthSession'
import { fetchNotificationClassAudiences, fetchNotificationIndividualAudiences } from '@/services/notificationApi'
import { extractApiErrorMessage } from '@/types/api'
import type { NotificationAudienceClassContext, NotificationAudienceType, NotificationClassAudience, NotificationIndividualAudience, NotificationRoleCode } from '@/types/notification'

const props = withDefaults(defineProps<{ audienceType: NotificationAudienceType; targetReference?: string; recipientUserIds?: number[]; disabled?: boolean }>(), { targetReference: '', recipientUserIds: () => [], disabled: false })
const emit = defineEmits<{ (e: 'update:audienceType', value: NotificationAudienceType): void; (e: 'update:targetReference', value: string): void; (e: 'update:recipientUserIds', value: number[]): void }>()
const { requireAccessToken } = useAuthSession()
const audienceOptions = [{ label: 'Toàn trường', value: 'SCHOOL' as const }, { label: 'Theo lớp học', value: 'CLASS' as const }, { label: 'Chọn người nhận', value: 'INDIVIDUAL' as const }]
const roleOptions = [{ label: 'Tất cả vai trò', value: undefined }, { label: 'Học sinh', value: 'STUDENT' as const }, { label: 'Giáo viên', value: 'TEACHER' as const }, { label: 'Văn phòng đào tạo', value: 'ACADEMIC_OFFICE' as const }, { label: 'Quản trị viên', value: 'ADMIN' as const }]
const pageSize = 20
const classOptions = ref<NotificationClassAudience[]>([])
const classLoading = ref(false)
const classError = ref('')
const classPage = ref(0)
const classTotalItems = ref(0)
const classQuery = ref('')
const individualOptions = ref<NotificationIndividualAudience[]>([])
const individualLoading = ref(false)
const selectAllLoading = ref(false)
const individualError = ref('')
const individualPage = ref(0)
const individualTotalItems = ref(0)
const individualTotalPages = ref(1)
const individualQuery = ref('')
const roleCode = ref<NotificationRoleCode | undefined>(undefined)
const studentClassId = ref<number | undefined>(undefined)
const teacherClassId = ref<number | undefined>(undefined)
let classRequestId = 0
let individualRequestId = 0
let classSearchTimer: ReturnType<typeof setTimeout> | undefined
let individualSearchTimer: ReturnType<typeof setTimeout> | undefined

const currentAudienceType = computed({ get: () => props.audienceType, set: (value: NotificationAudienceType) => { emit('update:audienceType', value); if (value !== 'CLASS') emit('update:targetReference', ''); if (value !== 'INDIVIDUAL') emit('update:recipientUserIds', []) } })
const selectedClassId = computed(() => { const value = Number(props.targetReference); return Number.isSafeInteger(value) && value > 0 ? value : null })
const selectedClass = computed(() => classOptions.value.find((item) => item.classId === selectedClassId.value) ?? null)
const selectedUserIds = computed(() => new Set(props.recipientUserIds))
const currentIndividualFilter = computed(() => JSON.stringify({ q: individualQuery.value, roleCode: roleCode.value, studentClassId: studentClassId.value, teacherClassId: teacherClassId.value }))
const bulkSelectionFilter = ref('')
const bulkSelectionIds = ref<number[]>([])
const isAllFilteredSelected = computed(() => bulkSelectionFilter.value === currentIndividualFilter.value && bulkSelectionIds.value.length > 0 && bulkSelectionIds.value.every((id) => selectedUserIds.value.has(id)))
const classFilterOptions = computed(() => classOptions.value.map((item) => ({ label: `${item.classCode} — ${item.className}`, value: item.classId })))
const hasIndividualFilters = computed(() => Boolean(individualQuery.value.trim() || roleCode.value || studentClassId.value || teacherClassId.value))

function lookupError(error: unknown, fallback: string): string { return extractApiErrorMessage(error, fallback) }

async function loadClasses(reset = true, query = classQuery.value): Promise<void> {
  const token = requireAccessToken(); if (!token) return
  const requestId = ++classRequestId; classLoading.value = true; classError.value = ''
  try {
    const response = await fetchNotificationClassAudiences(token, { q: query, page: reset ? 0 : classPage.value + 1, pageSize })
    if (requestId !== classRequestId) return
    classOptions.value = reset ? response.result : [...classOptions.value, ...response.result]
    classPage.value = response.meta.page; classTotalItems.value = response.meta.totalItems
  } catch (error) { if (requestId === classRequestId) classError.value = lookupError(error, 'Không thể tải danh sách lớp học.') } finally { if (requestId === classRequestId) classLoading.value = false }
}

async function loadIndividuals(page = 0): Promise<void> {
  const token = requireAccessToken(); if (!token) return
  const requestId = ++individualRequestId; individualLoading.value = true; individualError.value = ''
  try {
    const response = await fetchNotificationIndividualAudiences(token, { q: individualQuery.value, roleCode: roleCode.value, studentClassId: studentClassId.value, teacherClassId: teacherClassId.value, page, pageSize })
    if (requestId !== individualRequestId) return
    individualOptions.value = response.result
    individualPage.value = response.meta.page; individualTotalItems.value = response.meta.totalItems; individualTotalPages.value = Math.max(response.meta.totalPages, 1)
  } catch (error) { if (requestId === individualRequestId) individualError.value = lookupError(error, 'Không thể tải danh sách người nhận.') } finally { if (requestId === individualRequestId) individualLoading.value = false }
}

function scheduleIndividualSearch(): void { if (individualSearchTimer) clearTimeout(individualSearchTimer); individualSearchTimer = setTimeout(() => void loadIndividuals(0), 300) }
function handleClassFilter(event: { value?: string }): void { classQuery.value = event.value ?? ''; if (classSearchTimer) clearTimeout(classSearchTimer); classSearchTimer = setTimeout(() => void loadClasses(true, classQuery.value), 300) }
function handleIndividualFilterChange(): void { void loadIndividuals(0) }
function clearIndividualFilters(): void { individualQuery.value = ''; roleCode.value = undefined; studentClassId.value = undefined; teacherClassId.value = undefined; void loadIndividuals(0) }
function selectClass(item: NotificationClassAudience | null): void { emit('update:targetReference', item ? String(item.classId) : '') }
function toggleUser(user: NotificationIndividualAudience): void { const ids = new Set(props.recipientUserIds); if (ids.has(user.userId)) ids.delete(user.userId); else ids.add(user.userId); emit('update:recipientUserIds', [...ids]) }
async function selectAllFilteredUsers(): Promise<void> {
  const token = requireAccessToken(); if (!token || selectAllLoading.value) return
  selectAllLoading.value = true
  const allUsers: NotificationIndividualAudience[] = []
  const query = { q: individualQuery.value, roleCode: roleCode.value, studentClassId: studentClassId.value, teacherClassId: teacherClassId.value }
  try {
    individualError.value = ''
    const firstPage = await fetchNotificationIndividualAudiences(token, { ...query, page: 0, pageSize: 200 })
    allUsers.push(...firstPage.result)
    for (let page = 1; page < firstPage.meta.totalPages; page += 1) {
      const response = await fetchNotificationIndividualAudiences(token, { ...query, page, pageSize: 200 })
      allUsers.push(...response.result)
    }
    const matchingIds = [...new Set(allUsers.map((user) => user.userId))]
    const ids = new Set(props.recipientUserIds)
    const removeMatching = matchingIds.length > 0 && matchingIds.every((id) => ids.has(id))
    if (removeMatching) matchingIds.forEach((id) => ids.delete(id))
    else matchingIds.forEach((id) => ids.add(id))
    bulkSelectionIds.value = matchingIds
    bulkSelectionFilter.value = JSON.stringify(query)
    emit('update:recipientUserIds', [...ids])
  } catch (error) {
    individualError.value = lookupError(error, 'Không thể tải toàn bộ người nhận phù hợp.')
  } finally {
    selectAllLoading.value = false
  }
}
function displayClass(context: NotificationAudienceClassContext): string { return `${context.classCode} — ${context.className}` }
function roleLabel(role: NotificationRoleCode): string { return roleOptions.find((item) => item.value === role)?.label ?? role }

watch(() => props.audienceType, (type) => { if (type === 'CLASS' && classOptions.value.length === 0) void loadClasses(); if (type === 'INDIVIDUAL' && individualOptions.value.length === 0) { void loadClasses(); void loadIndividuals() } })
onMounted(() => { if (props.audienceType === 'CLASS') void loadClasses(); if (props.audienceType === 'INDIVIDUAL') { void loadClasses(); void loadIndividuals() } })
onBeforeUnmount(() => { if (classSearchTimer) clearTimeout(classSearchTimer); if (individualSearchTimer) clearTimeout(individualSearchTimer) })
</script>

<template>
  <div class="notification-audience-selector" data-testid="audience-selector">
    <div class="field-group"><label for="notification-audience-type">Phạm vi đối tượng nhận</label><Dropdown id="notification-audience-type" v-model="currentAudienceType" :options="audienceOptions" option-label="label" option-value="value" placeholder="Chọn đối tượng nhận" :disabled="props.disabled || selectAllLoading" fluid /></div>

    <div v-if="currentAudienceType === 'CLASS'" class="field-group">
      <label for="notification-class-picker">Lớp học nhận thông báo <span class="required-mark" aria-hidden="true">*</span></label>
      <Dropdown id="notification-class-picker" :model-value="selectedClassId" :options="classOptions" option-value="classId" placeholder="Chọn lớp học" :loading="classLoading" :disabled="props.disabled" :aria-required="true" filter filter-placeholder="Tìm theo mã hoặc tên lớp" show-clear fluid @filter="handleClassFilter" @update:model-value="(value) => selectClass(classOptions.find((item) => item.classId === value) ?? null)">
        <template #option="slotProps"><div class="audience-option"><strong>{{ slotProps.option.classCode }} — {{ slotProps.option.className }}</strong><small>{{ slotProps.option.eligibleRecipientCount }} người nhận đủ điều kiện</small></div></template>
        <template #value="slotProps"><span v-if="selectedClass">{{ selectedClass.classCode }} — {{ selectedClass.className }} ({{ selectedClass.eligibleRecipientCount }})</span><span v-else-if="slotProps.value">Lớp đã chọn (#{{ slotProps.value }})</span><span v-else>{{ slotProps.placeholder }}</span></template>
      </Dropdown>
      <small v-if="classLoading" class="field-hint" role="status">Đang tải danh sách lớp...</small><small v-else-if="classError" class="field-error" role="alert">{{ classError }} <button type="button" class="inline-retry" @click="loadClasses()">Thử lại</button></small><small v-else-if="!classOptions.length" class="field-hint">Không tìm thấy lớp phù hợp.</small><small v-else class="field-hint">{{ classTotalItems }} lớp đủ điều kiện.</small>
    </div>

    <div v-if="currentAudienceType === 'INDIVIDUAL'" class="field-group individual-audience">
      <div class="field-group-heading"><div><label for="notification-user-search">Người nhận <span class="required-mark" aria-hidden="true">*</span></label></div><div class="audience-actions"><Button :label="isAllFilteredSelected ? 'Bỏ chọn tất cả' : 'Chọn tất cả'" :icon="isAllFilteredSelected ? 'pi pi-times' : 'pi pi-check-square'" severity="secondary" text size="small" :disabled="props.disabled || individualLoading || individualTotalItems === 0" :loading="selectAllLoading" @click="selectAllFilteredUsers" /><Button v-if="hasIndividualFilters" label="Xóa bộ lọc" icon="pi pi-filter-slash" severity="secondary" text size="small" :disabled="props.disabled || individualLoading || selectAllLoading" @click="clearIndividualFilters" /></div></div>
      <InputText id="notification-user-search" v-model="individualQuery" placeholder="Tìm theo tên, mã học sinh, mã giáo viên hoặc tài khoản..." :disabled="props.disabled || selectAllLoading" fluid @input="scheduleIndividualSearch" />
      <div class="audience-filter-grid"><div class="field-group"><label for="notification-role-filter">Vai trò</label><Dropdown id="notification-role-filter" v-model="roleCode" :options="roleOptions" option-label="label" option-value="value" placeholder="Tất cả vai trò" :disabled="props.disabled || individualLoading || selectAllLoading" show-clear fluid @update:model-value="handleIndividualFilterChange" /></div><div class="field-group"><label for="notification-student-class-filter">Lớp của học sinh</label><Dropdown id="notification-student-class-filter" v-model="studentClassId" :options="classFilterOptions" option-label="label" option-value="value" placeholder="Tất cả lớp học sinh" :disabled="props.disabled || individualLoading || selectAllLoading" show-clear fluid @update:model-value="handleIndividualFilterChange" /></div><div class="field-group"><label for="notification-teacher-class-filter">Lớp giáo viên dạy/GVCN</label><Dropdown id="notification-teacher-class-filter" v-model="teacherClassId" :options="classFilterOptions" option-label="label" option-value="value" placeholder="Tất cả lớp giáo viên" :disabled="props.disabled || individualLoading || selectAllLoading" show-clear fluid @update:model-value="handleIndividualFilterChange" /></div></div>
      <div class="audience-results" role="listbox" aria-multiselectable="true" aria-label="Kết quả người nhận"><div v-if="individualLoading" class="lookup-state" role="status">Đang tải người nhận...</div><div v-else-if="individualError" class="lookup-state field-error" role="alert">{{ individualError }} <button type="button" class="inline-retry" @click="loadIndividuals(individualPage)">Thử lại</button></div><div v-else-if="!individualOptions.length" class="lookup-state">Không tìm thấy người nhận phù hợp.</div><button v-for="user in individualOptions" :key="user.userId" type="button" class="audience-result" :class="{ 'audience-result-selected': selectedUserIds.has(user.userId) }" :disabled="props.disabled || selectAllLoading" role="option" :aria-selected="selectedUserIds.has(user.userId)" @click="toggleUser(user)"><Checkbox :model-value="selectedUserIds.has(user.userId)" binary tabindex="-1" aria-hidden="true" /><span class="result-copy"><strong>{{ user.displayName }}</strong><small>{{ [user.studentCode, user.username].filter(Boolean).join(' · ') }}</small><small v-if="user.roleCodes?.length">{{ user.roleCodes.map(roleLabel).join(', ') }}</small><small v-if="user.studentClass">Lớp học sinh: {{ displayClass(user.studentClass) }}</small><small v-if="user.teacherClasses?.length">Lớp giảng dạy: {{ user.teacherClasses.map(displayClass).join(', ') }}</small></span></button></div>
      <div v-if="individualTotalItems > 0" class="lookup-footer"><small class="field-hint">{{ individualTotalItems }} người phù hợp · Trang {{ individualPage + 1 }} / {{ individualTotalPages }}</small><div class="pagination-actions"><Button label="Trước" icon="pi pi-chevron-left" text size="small" :disabled="individualPage <= 0 || individualLoading || props.disabled || selectAllLoading" @click="loadIndividuals(individualPage - 1)" /><Button label="Sau" icon="pi pi-chevron-right" icon-pos="right" text size="small" :disabled="individualPage >= individualTotalPages - 1 || individualLoading || props.disabled || selectAllLoading" @click="loadIndividuals(individualPage + 1)" /></div></div>
    </div>
    <div v-if="currentAudienceType === 'SCHOOL'" class="form-alert form-alert-info">Thông báo sẽ được gửi tới toàn bộ thành viên trong trường học.</div>
  </div>
</template>

<style scoped>
.notification-audience-selector { display: grid; gap: 18px; }.notification-audience-selector :deep(.p-dropdown), .notification-audience-selector :deep(.p-inputtext) { width: 100%; }.notification-audience-selector label { color: #334155; font-size: 14px; font-weight: 600; }.required-mark { color: #ba1a1a; }.notification-audience-selector small { display: block; }.field-hint { color: #64748b; }.field-error { color: #b91c1c; }.inline-retry { padding: 0; border: 0; background: transparent; color: #b91c1c; text-decoration: underline; cursor: pointer; }.audience-option { display: grid; gap: 3px; }.audience-option small, .result-copy small { color: #64748b; }.field-group-heading, .lookup-footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; }.field-group-heading > div, .audience-actions { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }.audience-filter-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }.audience-filter-grid .field-group { gap: 6px; }.audience-results { display: grid; max-height: 300px; overflow-y: auto; border: 1px solid #cbd5e1; border-radius: 6px; }.lookup-state { padding: 14px; color: #64748b; }.audience-result { display: flex; align-items: flex-start; gap: 10px; padding: 10px 12px; border: 0; border-bottom: 1px solid #e2e8f0; background: #fff; color: #1e293b; text-align: left; cursor: pointer; }.audience-result:last-child { border-bottom: 0; }.audience-result:hover, .audience-result-selected { background: #eff6ff; }.audience-result:disabled { cursor: not-allowed; opacity: .6; }.result-copy { display: grid; gap: 3px; }.pagination-actions { display: flex; gap: 4px; }
@media (max-width: 760px) { .audience-filter-grid { grid-template-columns: 1fr; }.field-group-heading, .lookup-footer { align-items: stretch; flex-direction: column; } }
</style>
