<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import Button from 'primevue/button'
import ConfirmDialog from 'primevue/confirmdialog'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import { useConfirm } from 'primevue/useconfirm'
import { useRouter } from 'vue-router'

import SubjectApplicabilityDialog from '@/components/SubjectApplicabilityDialog.vue'
import SubjectDialog from '@/components/SubjectDialog.vue'
import SubjectTable from '@/components/SubjectTable.vue'
import FormAlert from '@/components/FormAlert.vue'
import PageState from '@/components/PageState.vue'
import { clearAuthSession, getAuthSession } from '@/services/authSession'
import { createSubject, createSubjectApplicability, deactivateSubjectApplicability, fetchAcademicYears, fetchGrades, fetchSchoolClasses, fetchSemesters, fetchSubjects, fetchSubjectApplicabilities, updateSubject, updateSubjectApplicability } from '@/services/academicApi'
import { isApiError } from '@/types/api'
import type { AcademicYear, ApplicationScope, GradeLevel, SchoolClass, Semester, Subject, SubjectApplicability, SubjectApplicabilityFormValues, SubjectFormValues, SubjectStatus, SubjectType } from '@/types/academic'
import type { LoadingState } from '@/types/ui'

type StatusFilter = 'ALL' | SubjectStatus
type TypeFilter = 'ALL' | SubjectType
type ScopeFilter = 'ALL' | ApplicationScope

const router = useRouter()
const confirm = useConfirm()
const subjects = ref<Subject[]>([])
const academicYears = ref<AcademicYear[]>([])
const semesters = ref<Semester[]>([])
const grades = ref<GradeLevel[]>([])
const schoolClasses = ref<SchoolClass[]>([])
const selectedAcademicYearId = ref<number | null>(null)
const loadingState = ref<LoadingState>('loading')
const contextLoading = ref(true)
const forbidden = ref(false)
const errorMessage = ref('')
const statusMessage = ref('')
const searchText = ref('')
const statusFilter = ref<StatusFilter>('ALL')
const typeFilter = ref<TypeFilter>('ALL')
const scopeFilter = ref<ScopeFilter>('ALL')
const dialogVisible = ref(false)
const applicabilityVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const selectedSubject = ref<Subject | null>(null)
const applicabilityMode = ref<'create' | 'edit'>('create')
const selectedApplicability = ref<SubjectApplicability | null>(null)
const applicabilities = ref<SubjectApplicability[]>([])
const applicabilityLoading = ref(false)
const saving = ref(false)
const dialogErrorMessage = ref('')
const applicabilityErrorMessage = ref('')

const statusOptions: Array<{ label: string; value: StatusFilter }> = [
  { label: 'Tất cả trạng thái', value: 'ALL' },
  { label: 'Đang giảng dạy', value: 'ACTIVE' },
  { label: 'Tạm ngưng giảng dạy', value: 'INACTIVE' },
]
const typeOptions: Array<{ label: string; value: TypeFilter }> = [
  { label: 'Tất cả loại môn', value: 'ALL' },
  { label: 'CHÍNH KHÓA', value: 'ACADEMIC' },
  { label: 'KỸ NĂNG', value: 'SKILL' },
]
const scopeOptions: Array<{ label: string; value: ScopeFilter }> = [
  { label: 'Tất cả phạm vi', value: 'ALL' },
  { label: 'Theo khối', value: 'GRADE' },
  { label: 'Theo lớp', value: 'CLASS' },
]
const filteredSubjects = computed(() => {
  const query = searchText.value.trim().toLocaleLowerCase()
  return subjects.value.filter((subject) => {
    const matchesSearch = !query || subject.code.toLocaleLowerCase().includes(query) || subject.name.toLocaleLowerCase().includes(query)
    const matchesStatus = statusFilter.value === 'ALL' || subject.status === statusFilter.value
    const matchesType = typeFilter.value === 'ALL' || subject.subjectType === typeFilter.value
    const matchesScope = scopeFilter.value === 'ALL' || subject.applicationScope === scopeFilter.value
    return matchesSearch && matchesStatus && matchesType && matchesScope
  })
})
const pageState = computed<LoadingState>(() => loadingState.value === 'success' && filteredSubjects.value.length === 0 ? 'empty' : loadingState.value)
const selectedAcademicYear = computed(() => academicYears.value.find((year) => year.id === selectedAcademicYearId.value) ?? null)

function token(): string | null {
  const session = getAuthSession()
  if (session) return session.accessToken
  clearAuthSession()
  void router.replace({ name: 'login' })
  return null
}

function messageFor(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

async function loadSubjects(): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  loadingState.value = 'loading'
  forbidden.value = false
  errorMessage.value = ''
  try {
    subjects.value = await fetchSubjects(accessToken)
    loadingState.value = 'success'
  } catch (error) {
    if (isApiError(error, 401)) return
    forbidden.value = isApiError(error, 403)
    errorMessage.value = messageFor(error, 'Không thể tải danh sách môn học.')
    loadingState.value = 'error'
  }
}

async function loadYearContext(): Promise<void> {
  const accessToken = token()
  if (!accessToken || selectedAcademicYearId.value === null) {
    semesters.value = []
    schoolClasses.value = []
    return
  }
  try {
    const [semesterList, classList] = await Promise.all([
      fetchSemesters(accessToken, selectedAcademicYearId.value),
      fetchSchoolClasses(accessToken, selectedAcademicYearId.value),
    ])
    semesters.value = semesterList
    schoolClasses.value = classList
  } catch (error) {
    if (isApiError(error, 401)) return
    forbidden.value = isApiError(error, 403)
    errorMessage.value = messageFor(error, 'Không thể tải học kỳ và lớp theo năm học.')
  }
}

async function loadContext(): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  contextLoading.value = true
  try {
    const [years, gradeLevels] = await Promise.all([fetchAcademicYears(accessToken), fetchGrades(accessToken)])
    academicYears.value = years
    grades.value = gradeLevels
    selectedAcademicYearId.value = years.find((year) => year.status === 'ACTIVE')?.id ?? years[0]?.id ?? null
  } catch (error) {
    if (isApiError(error, 401)) return
    forbidden.value = isApiError(error, 403)
    errorMessage.value = messageFor(error, 'Không thể tải context năm học và khối.')
  } finally {
    contextLoading.value = false
  }
}

function openCreate(): void {
  selectedSubject.value = null
  dialogMode.value = 'create'
  dialogErrorMessage.value = ''
  dialogVisible.value = true
}

function openEdit(subject: Subject): void {
  selectedSubject.value = subject
  dialogMode.value = 'edit'
  dialogErrorMessage.value = ''
  dialogVisible.value = true
}

function closeDialog(): void {
  dialogVisible.value = false
  dialogErrorMessage.value = ''
}

async function saveSubject(values: SubjectFormValues): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  saving.value = true
  dialogErrorMessage.value = ''
  try {
    const request = { code: values.code.trim(), name: values.name.trim(), subjectType: values.subjectType, applicationScope: values.applicationScope, status: values.status }
    if (dialogMode.value === 'edit' && selectedSubject.value) {
      await updateSubject(accessToken, selectedSubject.value.id, request)
      statusMessage.value = 'Đã cập nhật môn học.'
    } else {
      await createSubject(accessToken, request)
      statusMessage.value = 'Đã tạo môn học.'
    }
    await loadSubjects()
    closeDialog()
  } catch (error) {
    if (isApiError(error, 401)) return
    dialogErrorMessage.value = messageFor(error, 'Không thể lưu môn học.')
  } finally {
    saving.value = false
  }
}

function openApplicability(subject: Subject): void {
  selectedSubject.value = subject
  selectedApplicability.value = null
  applicabilityMode.value = 'create'
  applicabilityErrorMessage.value = ''
  applicabilityVisible.value = true
  void loadApplicabilities()
}

function closeApplicability(): void {
  applicabilityVisible.value = false
  selectedApplicability.value = null
  applicabilityMode.value = 'create'
  applicabilityErrorMessage.value = ''
}

async function loadApplicabilities(): Promise<void> {
  const accessToken = token()
  if (!accessToken || !selectedSubject.value) return
  applicabilityLoading.value = true
  applicabilityErrorMessage.value = ''
  try {
    applicabilities.value = await fetchSubjectApplicabilities(accessToken, selectedSubject.value.id)
  } catch (error) {
    if (isApiError(error, 401)) return
    applicabilityErrorMessage.value = messageFor(error, 'Không thể tải danh sách phạm vi áp dụng.')
  } finally {
    applicabilityLoading.value = false
  }
}

function openApplicabilityCreate(): void {
  selectedApplicability.value = null
  applicabilityMode.value = 'create'
  applicabilityErrorMessage.value = ''
}

function openApplicabilityEdit(applicability: SubjectApplicability): void {
  selectedApplicability.value = applicability
  applicabilityMode.value = 'edit'
  applicabilityErrorMessage.value = ''
}

async function saveApplicability(values: SubjectApplicabilityFormValues): Promise<void> {
  const accessToken = token()
  if (!accessToken || !selectedSubject.value || values.semesterId === null) {
    applicabilityErrorMessage.value = 'Môn học và học kỳ là bắt buộc.'
    return
  }
  const targetId = values.scopeType === 'GRADE' ? values.gradeLevelId : values.classId
  if (targetId === null) {
    applicabilityErrorMessage.value = 'Phạm vi áp dụng là bắt buộc.'
    return
  }
  saving.value = true
  applicabilityErrorMessage.value = ''
  try {
    const request = {
      semesterId: values.semesterId,
      scopeType: values.scopeType,
      gradeLevelId: values.scopeType === 'GRADE' ? values.gradeLevelId : null,
      classId: values.scopeType === 'CLASS' ? values.classId : null,
    }
    if (applicabilityMode.value === 'edit' && selectedApplicability.value) {
      await updateSubjectApplicability(accessToken, selectedSubject.value.id, selectedApplicability.value.id, {
        ...request,
        status: selectedApplicability.value.status,
      })
      statusMessage.value = 'Đã cập nhật cấu hình phạm vi áp dụng.'
    } else {
      await createSubjectApplicability(accessToken, selectedSubject.value.id, request)
      statusMessage.value = 'Đã tạo cấu hình phạm vi áp dụng.'
    }
    await loadApplicabilities()
    openApplicabilityCreate()
  } catch (error) {
    if (isApiError(error, 401)) return
    applicabilityErrorMessage.value = messageFor(error, 'Không thể lưu cấu hình phạm vi áp dụng.')
  } finally {
    saving.value = false
  }
}

function confirmDeactivateApplicability(applicability: SubjectApplicability): void {
  confirm.require({
    message: 'Cấu hình sẽ chuyển sang trạng thái ngừng áp dụng và không bị xóa khỏi lịch sử.',
    header: 'Ngừng áp dụng môn?',
    icon: 'pi pi-exclamation-triangle',
    accept: () => { void deactivateApplicability(applicability) },
  })
}

async function deactivateApplicability(applicability: SubjectApplicability): Promise<void> {
  const accessToken = token()
  if (!accessToken || !selectedSubject.value) return
  saving.value = true
  applicabilityErrorMessage.value = ''
  try {
    await deactivateSubjectApplicability(accessToken, selectedSubject.value.id, applicability.id)
    statusMessage.value = 'Đã ngừng áp dụng cấu hình.'
    await loadApplicabilities()
  } catch (error) {
    if (isApiError(error, 401)) return
    applicabilityErrorMessage.value = messageFor(error, 'Không thể ngừng áp dụng cấu hình.')
  } finally {
    saving.value = false
  }
}

async function reactivateApplicability(applicability: SubjectApplicability): Promise<void> {
  const accessToken = token()
  if (!accessToken || !selectedSubject.value) return
  saving.value = true
  applicabilityErrorMessage.value = ''
  try {
    await updateSubjectApplicability(accessToken, selectedSubject.value.id, applicability.id, {
      semesterId: applicability.semesterId,
      scopeType: applicability.scopeType,
      gradeLevelId: applicability.gradeLevelId,
      classId: applicability.classId,
      status: 'ACTIVE',
    })
    statusMessage.value = 'Đã kích hoạt lại cấu hình.'
    await loadApplicabilities()
  } catch (error) {
    if (isApiError(error, 401)) return
    applicabilityErrorMessage.value = messageFor(error, 'Không thể kích hoạt lại cấu hình.')
  } finally {
    saving.value = false
  }
}

watch(selectedAcademicYearId, () => { void loadYearContext() })
onMounted(() => { void loadSubjects(); void loadContext() })
</script>

<template>
  <ConfirmDialog />
  <div class="page-heading">
    <div>
      <p class="eyebrow">Academic catalog</p>
      <h1>Môn học</h1>
      <p v-if="selectedAcademicYear">Năm học {{ selectedAcademicYear.code }} · cấu hình phạm vi áp dụng theo học kỳ.</p>
      <p v-else>Chọn năm học khi cần cấu hình phạm vi áp dụng.</p>
    </div>
    <div class="page-heading-actions">
      <Button label="Lớp-môn" icon="pi pi-link" severity="secondary" outlined @click="router.push({ name: 'v2-academic-class-subjects' })" />
      <Button label="Tạo môn" icon="pi pi-plus" @click="openCreate" />
    </div>
  </div>
  <FormAlert v-if="statusMessage" tone="success" :message="statusMessage" />
  <FormAlert v-if="errorMessage && !forbidden" tone="error" :message="errorMessage" />
  <section class="content-surface">
    <div class="search-grid">
      <div class="field-group"><label for="subject-year">Năm học cấu hình</label><Select id="subject-year" v-model="selectedAcademicYearId" :options="academicYears" option-label="code" option-value="id" placeholder="Chọn năm học" :loading="contextLoading" fluid /></div>
      <div class="field-group"><label for="subject-search">Tìm môn</label><InputText id="subject-search" v-model="searchText" placeholder="Mã hoặc tên môn" /></div>
      <div class="field-group"><label for="subject-type-filter">Loại môn</label><Select id="subject-type-filter" v-model="typeFilter" :options="typeOptions" option-label="label" option-value="value" fluid /></div>
      <div class="field-group"><label for="subject-scope-filter">Phạm vi</label><Select id="subject-scope-filter" v-model="scopeFilter" :options="scopeOptions" option-label="label" option-value="value" fluid /></div>
      <div class="field-group"><label for="subject-status-filter">Trạng thái</label><Select id="subject-status-filter" v-model="statusFilter" :options="statusOptions" option-label="label" option-value="value" fluid /></div>
    </div>
  </section>
  <section class="content-surface">
    <PageState :state="pageState" :forbidden="forbidden" forbidden-message="Bạn không có quyền xem danh sách môn học." :error-message="errorMessage" empty-heading="Chưa có môn học" empty-message="Chưa có môn học phù hợp với bộ lọc hiện tại." @retry="loadSubjects">
      <SubjectTable :subjects="filteredSubjects" @edit="openEdit" @configure-applicability="openApplicability" />
    </PageState>
  </section>
  <SubjectDialog v-model:visible="dialogVisible" :mode="dialogMode" :initial-value="selectedSubject" :saving="saving" :error-message="dialogErrorMessage" @save="saveSubject" @cancel="closeDialog" />
  <SubjectApplicabilityDialog v-model:visible="applicabilityVisible" :mode="applicabilityMode" :subject="selectedSubject" :initial-value="selectedApplicability" :applicabilities="applicabilities" :applicability-loading="applicabilityLoading" :semesters="semesters" :grades="grades" :school-classes="schoolClasses" :saving="saving" :error-message="applicabilityErrorMessage" @save="saveApplicability" @create="openApplicabilityCreate" @edit="openApplicabilityEdit" @deactivate="confirmDeactivateApplicability" @reactivate="reactivateApplicability" @cancel="closeApplicability" />
</template>
