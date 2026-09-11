<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'

import { PLACEMENT_RULE_VERSION, type CreatePlacementSessionRequest, type PlacementCandidateSource, type PlacementClassProfile } from '@/types/placement'
import type { AcademicYear, GradeLevel, SchoolClass } from '@/types/academic'
import type { UnassignedStudent } from '@/types/enrollment'

const props = withDefaults(defineProps<{
  academicYears: AcademicYear[]
  grades: GradeLevel[]
  classes: SchoolClass[]
  unassignedStudents: UnassignedStudent[]
  loadingContext?: boolean
  loadingClasses?: boolean
  loadingUnassigned?: boolean
  saving?: boolean
  errorMessage?: string
  forbidden?: boolean
  initialAcademicYearId?: number | null
  initialGradeId?: number | null
}>(), {
  loadingContext: false,
  loadingClasses: false,
  loadingUnassigned: false,
  saving: false,
  errorMessage: '',
  forbidden: false,
  initialAcademicYearId: null,
  initialGradeId: null,
})

const emit = defineEmits<{
  'update:academic-year-id': [yearId: number]
  'submit': [request: CreatePlacementSessionRequest]
  'cancel': []
}>()

const selectedAcademicYearId = ref<number | null>(props.initialAcademicYearId)
const selectedGradeId = ref<number | null>(props.initialGradeId)

const classProfiles = reactive<Record<number, { selected: boolean; profile: PlacementClassProfile }>>({})
const candidateConfigs = reactive<Record<number, {
  selected: boolean
  sourceType: PlacementCandidateSource
  eligibilityEvidence: string
  approvalReference: string
}>>({})

const validationError = ref<string>('')

const profileOptions = [
  { label: 'Thường (cân bằng học lực và nam nữ)', value: 'REGULAR' as PlacementClassProfile },
  { label: 'Nâng cao (ưu tiên điểm cao)', value: 'ADVANCED' as PlacementClassProfile },
  { label: 'Hỗ trợ học tập (ưu tiên điểm thấp)', value: 'SUPPORT' as PlacementClassProfile },
]

const candidateSourceOptions = [
  { label: 'Lên lớp', value: 'CONTINUING' as PlacementCandidateSource },
  { label: 'Nhập học mới', value: 'NEW_ADMISSION' as PlacementCandidateSource },
  { label: 'Học lại khối này', value: 'REPEAT' as PlacementCandidateSource },
]

// Initialize selection from props
watch(() => props.academicYears, (years) => {
  if (!selectedAcademicYearId.value && years.length > 0) {
    const active = years.find((y) => y.status === 'ACTIVE') ?? years[0]
    selectedAcademicYearId.value = active.id
    emit('update:academic-year-id', active.id)
  }
}, { immediate: true })

watch(() => props.grades, (gradesList) => {
  if (!selectedGradeId.value && gradesList.length > 0) {
    selectedGradeId.value = gradesList[0].id
  }
}, { immediate: true })

function onAcademicYearChange(newYearId: number | null): void {
  selectedAcademicYearId.value = newYearId
  if (newYearId !== null) {
    emit('update:academic-year-id', newYearId)
  }
}

// Available target classes: belonging to selected academic year & grade level, not CLOSED
const availableClasses = computed(() => {
  if (!selectedGradeId.value) return []
  return props.classes.filter((c) => c.gradeLevelId === selectedGradeId.value && c.status !== 'CLOSED')
})

// Sync class profiles when available classes change
watch(availableClasses, (classList) => {
  for (const c of classList) {
    if (!classProfiles[c.id]) {
      classProfiles[c.id] = { selected: true, profile: 'REGULAR' }
    }
  }
}, { immediate: true })

// Sync candidates when unassignedStudents change
const expandedCandidateRows = ref<Record<string | number, boolean>>({})

watch(() => props.unassignedStudents, (students) => {
  const map: Record<string | number, boolean> = {}
  for (const s of students) {
    map[s.studentId] = true
    if (!candidateConfigs[s.studentId]) {
      candidateConfigs[s.studentId] = {
        selected: true,
        sourceType: 'CONTINUING',
        eligibilityEvidence: '',
        approvalReference: '',
      }
    }
  }
  expandedCandidateRows.value = map
}, { immediate: true })

const selectedClassesCount = computed(() => {
  return availableClasses.value.filter((c) => classProfiles[c.id]?.selected).length
})

const selectedCandidatesCount = computed(() => {
  return props.unassignedStudents.filter((s) => candidateConfigs[s.studentId]?.selected).length
})

const allCandidatesSelected = computed({
  get: () => props.unassignedStudents.length > 0 && props.unassignedStudents.every((s) => candidateConfigs[s.studentId]?.selected),
  set: (val: boolean) => {
    for (const s of props.unassignedStudents) {
      if (candidateConfigs[s.studentId]) {
        candidateConfigs[s.studentId].selected = val
      }
    }
  },
})

function validateAndSubmit(): void {
  validationError.value = ''

  if (!selectedAcademicYearId.value || !selectedGradeId.value) {
    validationError.value = 'Vui lòng chọn năm học và khối đích.'
    return
  }

  const selectedTargetClasses = availableClasses.value
    .filter((c) => classProfiles[c.id]?.selected)
    .map((c) => ({
      classId: c.id,
      profile: classProfiles[c.id].profile,
      capacity: null,
    }))

  if (selectedTargetClasses.length === 0) {
    validationError.value = 'Vui lòng chọn ít nhất một lớp đích cho phiên xếp lớp.'
    return
  }

  const selectedCandidatesList = props.unassignedStudents.filter((s) => candidateConfigs[s.studentId]?.selected)
  if (selectedCandidatesList.length === 0) {
    validationError.value = 'Vui lòng chọn ít nhất một học sinh để xếp lớp.'
    return
  }

  // Validate sources & evidences
  for (const s of selectedCandidatesList) {
    const config = candidateConfigs[s.studentId]
    if (config.sourceType === 'NEW_ADMISSION' || config.sourceType === 'REPEAT') {
      if (!config.eligibilityEvidence?.trim() || !config.approvalReference?.trim()) {
        validationError.value = `Học sinh ${s.studentCode} (${s.studentName}) có nguồn "${config.sourceType === 'NEW_ADMISSION' ? 'Nhập học mới' : 'Học lại'}" bắt buộc phải có căn cứ xét xếp lớp và mã/phê duyệt tham chiếu.`
        return
      }
    }
  }

  const payload: CreatePlacementSessionRequest = {
    academicYearId: selectedAcademicYearId.value,
    targetGradeId: selectedGradeId.value,
    ruleVersion: PLACEMENT_RULE_VERSION,
    targetClasses: selectedTargetClasses,
    candidates: selectedCandidatesList.map((s) => {
      const config = candidateConfigs[s.studentId]
      const isManualSource = config.sourceType === 'NEW_ADMISSION' || config.sourceType === 'REPEAT'
      return {
        studentId: s.studentId,
        targetGradeId: selectedGradeId.value as number,
        sourceType: config.sourceType,
        score: null,
        scoreSourceReference: null,
        genderSnapshot: null,
        eligibilityEvidence: isManualSource ? config.eligibilityEvidence.trim() : null,
        approvalReference: isManualSource ? config.approvalReference.trim() : null,
      }
    }),
  }

  emit('submit', payload)
}
</script>

<template>
  <div class="placement-setup-form">
    <header class="page-heading">
      <div>
        <p class="eyebrow">XẾP LỚP TỰ ĐỘNG</p>
        <h1>Tạo phiên xếp lớp tự động</h1>
        <p class="section-caption">Cấu hình năm học, khối, lớp đích theo cách phân lớp và danh sách học sinh cần xếp lớp.</p>
      </div>
      <div class="page-heading-actions">
        <Button label="Hủy" severity="secondary" outlined :disabled="props.saving" @click="emit('cancel')" />
        <Button
          label="Tạo phiên nháp"
          icon="pi pi-check"
          :loading="props.saving"
          @click="validateAndSubmit"
        />
      </div>
    </header>

    <div v-if="validationError || props.errorMessage || props.forbidden" class="form-alert form-alert-error" role="alert">
      <span v-if="props.forbidden">{{ props.errorMessage || 'Bạn không có quyền thao tác xếp lớp này. Phiên đăng nhập vẫn được giữ.' }}</span>
      <span v-else>{{ validationError || props.errorMessage }}</span>
    </div>

    <!-- 1. Phạm vi năm học và khối -->
    <section class="content-surface placement-setup-section">
      <div class="section-heading">
        <div>
          <h2>1. Chọn phạm vi xếp lớp</h2>
          <p class="section-caption">Chọn năm học và khối học sinh sẽ được phân bổ vào.</p>
        </div>
      </div>
      <div class="scope-selectors">
        <div class="form-field">
          <label for="select-academic-year">Năm học</label>
          <Select
            id="select-academic-year"
            :model-value="selectedAcademicYearId"
            :options="props.academicYears"
            option-label="code"
            option-value="id"
            placeholder="Chọn năm học"
            :loading="props.loadingContext"
            :disabled="props.saving"
            @update:model-value="onAcademicYearChange"
          />
        </div>

        <div class="form-field">
          <label for="select-grade">Khối</label>
          <Select
            id="select-grade"
            v-model="selectedGradeId"
            :options="props.grades"
            option-label="name"
            option-value="id"
            placeholder="Chọn khối"
            :loading="props.loadingContext"
            :disabled="props.saving"
          />
        </div>
      </div>
    </section>

    <!-- 2. Lớp đích và cách phân lớp -->
    <section class="content-surface placement-setup-section">
      <div class="section-heading">
        <div>
          <h2>2. Chọn lớp đích và cách phân lớp (Đã chọn: {{ selectedClassesCount }})</h2>
          <p class="section-caption">
            Quy tắc: Nâng cao ưu tiên điểm cao; Hỗ trợ học tập ưu tiên nhóm điểm thấp; Thường cân bằng học lực và nam nữ.
          </p>
        </div>
      </div>

      <div v-if="props.loadingClasses" class="loading-hint">Đang tải danh sách lớp…</div>
      <div v-else-if="availableClasses.length === 0" class="empty-hint">
        Không có lớp nào khả dụng cho khối và năm học này.
      </div>
      <div v-else class="classes-grid">
        <article
          v-for="c in availableClasses"
          :key="c.id"
          class="class-card"
          :class="{ 'class-card--selected': classProfiles[c.id]?.selected }"
        >
          <div class="class-card-header">
            <div class="class-card-title">
              <Checkbox
                v-if="classProfiles[c.id]"
                v-model="classProfiles[c.id].selected"
                :binary="true"
                :input-id="`class-check-${c.id}`"
                :disabled="props.saving"
              />
              <label :for="`class-check-${c.id}`" class="class-name">
                <strong>{{ c.classCode }}</strong>
                <span v-if="c.className && c.className !== c.classCode" class="class-subname"> ({{ c.className }})</span>
              </label>
            </div>
            <Tag :value="`${c.capacity ?? '—'} chỗ`" severity="secondary" />
          </div>

          <div v-if="classProfiles[c.id]?.selected" class="class-card-body">
            <label class="profile-label">Cách phân lớp:</label>
            <Select
              v-model="classProfiles[c.id].profile"
              :options="profileOptions"
              option-label="label"
              option-value="value"
              class="w-full"
              :disabled="props.saving"
            />
          </div>
        </article>
      </div>
    </section>

    <!-- 3. Danh sách học sinh và nguồn candidate -->
    <section class="content-surface placement-setup-section">
      <div class="section-heading">
        <div>
          <h2>3. Danh sách học sinh chưa xếp lớp (Đã chọn: {{ selectedCandidatesCount }}/{{ props.unassignedStudents.length }})</h2>
          <p class="section-caption">
            Học sinh nhập học mới hoặc học lại cần có căn cứ và thông tin phê duyệt trước khi xếp lớp.
          </p>
        </div>
      </div>

      <div v-if="props.loadingUnassigned" class="loading-hint">Đang tải danh sách học sinh chưa xếp lớp…</div>
      <div v-else-if="props.unassignedStudents.length === 0" class="empty-hint">
        Năm học này không có học sinh chưa xếp lớp nào.
      </div>
      <div v-else class="table-container">
        <DataTable
          v-model:expanded-rows="expandedCandidateRows"
          :value="props.unassignedStudents"
          data-key="studentId"
          striped-rows
          responsive-layout="scroll"
          class="candidates-placement-table"
        >
          <Column header-style="width: 3rem">
            <template #header>
              <Checkbox v-model="allCandidatesSelected" :binary="true" :disabled="props.saving" />
            </template>
            <template #body="{ data }">
              <Checkbox
                v-if="candidateConfigs[data.studentId]"
                v-model="candidateConfigs[data.studentId].selected"
                :binary="true"
                :disabled="props.saving"
              />
            </template>
          </Column>

          <Column field="studentCode" header="Mã HS" style="width: 140px">
            <template #body="{ data }">
              <strong>{{ data.studentCode }}</strong>
            </template>
          </Column>
          <Column field="studentName" header="Họ và tên" style="min-width: 180px" />

          <Column header="Nguồn học sinh" style="min-width: 240px">
            <template #body="{ data }">
              <Select
                v-if="candidateConfigs[data.studentId]"
                v-model="candidateConfigs[data.studentId].sourceType"
                :options="candidateSourceOptions"
                option-label="label"
                option-value="value"
                size="small"
                class="w-full"
                :disabled="!candidateConfigs[data.studentId].selected || props.saving"
              />
            </template>
          </Column>

          <template #expansion="{ data }">
            <div
              v-if="candidateConfigs[data.studentId]"
              class="candidate-disclosure-panel"
              :class="{ 'candidate-disclosure-panel--inactive': !candidateConfigs[data.studentId].selected }"
            >
              <!-- CONTINUING: Trạng thái tinh gọn, không input -->
              <div
                v-if="candidateConfigs[data.studentId].sourceType === 'CONTINUING'"
                class="candidate-continuing-badge"
              >
                <i class="pi pi-check-circle continuing-icon" aria-hidden="true" />
                <span class="continuing-text">✓ Đủ điều kiện lên lớp theo kết quả năm học trước. Không cần bổ sung hồ sơ.</span>
              </div>

              <!-- NEW_ADMISSION: Section nhập bổ sung với label & placeholder thực tế -->
              <div
                v-else-if="candidateConfigs[data.studentId].sourceType === 'NEW_ADMISSION'"
                class="candidate-supplementary-form"
              >
                <div class="supplementary-field">
                  <label :for="`evidence-${data.studentId}`" class="field-label">
                    Căn cứ nhập học <span class="required-star">*</span>
                  </label>
                  <InputText
                    :id="`evidence-${data.studentId}`"
                    v-model="candidateConfigs[data.studentId].eligibilityEvidence"
                    placeholder="VD: Hồ sơ chuyển trường từ THCS ABC, Trúng tuyển đầu cấp..."
                    size="small"
                    class="w-full"
                    :disabled="!candidateConfigs[data.studentId].selected || props.saving"
                  />
                </div>
                <div class="supplementary-field">
                  <label :for="`ref-${data.studentId}`" class="field-label">
                    Mã hồ sơ / quyết định tiếp nhận <span class="required-star">*</span>
                  </label>
                  <InputText
                    :id="`ref-${data.studentId}`"
                    v-model="candidateConfigs[data.studentId].approvalReference"
                    placeholder="VD: QĐ-124/THCS-2026, HS-TS-2026-0045..."
                    size="small"
                    class="w-full"
                    :disabled="!candidateConfigs[data.studentId].selected || props.saving"
                  />
                </div>
              </div>

              <!-- REPEAT: Section nhập bổ sung với label & placeholder thực tế -->
              <div
                v-else-if="candidateConfigs[data.studentId].sourceType === 'REPEAT'"
                class="candidate-supplementary-form"
              >
                <div class="supplementary-field">
                  <label :for="`evidence-${data.studentId}`" class="field-label">
                    Căn cứ học lại <span class="required-star">*</span>
                  </label>
                  <InputText
                    :id="`evidence-${data.studentId}`"
                    v-model="candidateConfigs[data.studentId].eligibilityEvidence"
                    placeholder="VD: Học bạ năm trước chưa đủ điều kiện lên lớp..."
                    size="small"
                    class="w-full"
                    :disabled="!candidateConfigs[data.studentId].selected || props.saving"
                  />
                </div>
                <div class="supplementary-field">
                  <label :for="`ref-${data.studentId}`" class="field-label">
                    Mã quyết định / biên bản <span class="required-star">*</span>
                  </label>
                  <InputText
                    :id="`ref-${data.studentId}`"
                    v-model="candidateConfigs[data.studentId].approvalReference"
                    placeholder="VD: BB-HDXL-09/2026, QĐ-08/LƯU-BAN..."
                    size="small"
                    class="w-full"
                    :disabled="!candidateConfigs[data.studentId].selected || props.saving"
                  />
                </div>
              </div>
            </div>
          </template>
        </DataTable>
      </div>
    </section>

    <footer class="form-actions-footer">
      <Button label="Hủy" severity="secondary" outlined :disabled="props.saving" @click="emit('cancel')" />
      <Button
        label="Tạo phiên nháp"
        icon="pi pi-check"
        :loading="props.saving"
        @click="validateAndSubmit"
      />
    </footer>
  </div>
</template>

<style scoped>
.placement-setup-form {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.placement-setup-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.scope-selectors {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}
.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.form-field label {
  font-weight: 500;
  font-size: 0.9rem;
}
.classes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}
.class-card {
  border: 1px solid var(--p-content-border-color, #e5e7eb);
  border-radius: 8px;
  padding: 14px;
  background-color: var(--p-content-background, #fff);
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: border-color 0.2s;
}
.class-card--selected {
  border-color: var(--p-primary-color, #3b82f6);
  background-color: var(--p-surface-50, #f8fafc);
}
.class-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.class-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.class-name {
  cursor: pointer;
  user-select: none;
}
.class-subname {
  color: var(--p-text-muted-color, #6b7280);
  font-size: 0.85rem;
}
.class-card-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.profile-label {
  font-size: 0.8rem;
  color: var(--p-text-muted-color, #6b7280);
}
.table-container {
  overflow-x: auto;
}
.loading-hint, .empty-hint {
  padding: 20px;
  text-align: center;
  color: var(--p-text-muted-color, #6b7280);
  font-style: italic;
}
.form-actions-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}
.form-alert {
  padding: 12px 16px;
  border-radius: 6px;
  font-size: 0.9rem;
}
.form-alert-error {
  background-color: var(--p-red-50, #fef2f2);
  border: 1px solid var(--p-red-200, #fecaca);
  color: var(--p-red-800, #991b1b);
}
.candidate-disclosure-panel {
  padding: 10px 16px 12px 48px;
  background-color: var(--p-surface-50, #f8fafc);
  border-bottom: 1px solid var(--p-content-border-color, #e5e7eb);
  transition: opacity 0.2s;
}
.candidate-disclosure-panel--inactive {
  opacity: 0.5;
  pointer-events: none;
}
.candidate-continuing-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--p-green-700, #15803d);
  font-size: 0.875rem;
  font-weight: 500;
  padding: 4px 0;
}
.continuing-icon {
  font-size: 0.95rem;
  color: var(--p-green-600, #16a34a);
}
.candidate-supplementary-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  padding: 6px 0;
}
@media (max-width: 768px) {
  .candidate-supplementary-form {
    grid-template-columns: 1fr;
    gap: 10px;
  }
}
.supplementary-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.field-label {
  font-size: 0.825rem;
  font-weight: 600;
  color: var(--p-text-color, #374151);
}
.required-star {
  color: var(--p-red-600, #dc2626);
}
</style>
