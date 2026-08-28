<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import Button from 'primevue/button'
import Select from 'primevue/select'
import { useRouter } from 'vue-router'

import ClassSubjectDialog from '@/components/ClassSubjectDialog.vue'
import ClassSubjectTable from '@/components/ClassSubjectTable.vue'
import FormAlert from '@/components/FormAlert.vue'
import PageState from '@/components/PageState.vue'
import { clearAuthSession, getAuthSession } from '@/services/authSession'
import { createClassSubject, fetchAcademicYears, fetchClassSubjects, fetchSchoolClasses, fetchSemesters, fetchSubjects, updateClassSubject } from '@/services/academicApi'
import { isApiError } from '@/types/api'
import type { AcademicYear, ClassSubject, ClassSubjectFormValues, SchoolClass, Semester, Subject } from '@/types/academic'
import type { LoadingState } from '@/types/ui'

const router = useRouter()
const academicYears = ref<AcademicYear[]>([])
const schoolClasses = ref<SchoolClass[]>([])
const semesters = ref<Semester[]>([])
const subjects = ref<Subject[]>([])
const classSubjects = ref<ClassSubject[]>([])
const selectedAcademicYearId = ref<number | null>(null)
const selectedClassId = ref<number | null>(null)
const selectedSemesterId = ref<number | null>(null)
const loadingState = ref<LoadingState>('loading')
const contextLoading = ref(true)
const forbidden = ref(false)
const errorMessage = ref('')
const statusMessage = ref('')
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const selectedClassSubject = ref<ClassSubject | null>(null)
const saving = ref(false)
const dialogErrorMessage = ref('')
const conflictMessage = ref('')

const selectedClass = computed(() => schoolClasses.value.find((schoolClass) => schoolClass.id === selectedClassId.value) ?? null)
const selectedSemester = computed(() => semesters.value.find((semester) => semester.id === selectedSemesterId.value) ?? null)
const classClosed = computed(() => selectedClass.value?.status === 'CLOSED')
const semesterClosed = computed(() => selectedSemester.value?.status === 'CLOSED' || selectedSemester.value?.status === 'LOCKED')
const availableSubjects = computed(() => subjects.value.filter((subject) => subject.status === 'ACTIVE' && !classSubjects.value.some((item) => item.subjectId === subject.id)))
const pageState = computed<LoadingState>(() => loadingState.value === 'success' && classSubjects.value.length === 0 ? 'empty' : loadingState.value)

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

async function loadClassSubjects(): Promise<void> {
  const accessToken = token()
  if (!accessToken || selectedClassId.value === null || selectedSemesterId.value === null) {
    classSubjects.value = []
    loadingState.value = 'success'
    return
  }
  loadingState.value = 'loading'
  forbidden.value = false
  errorMessage.value = ''
  try {
    classSubjects.value = await fetchClassSubjects(accessToken, selectedClassId.value, selectedSemesterId.value)
    loadingState.value = 'success'
  } catch (error) {
    if (isApiError(error, 401)) return
    forbidden.value = isApiError(error, 403)
    errorMessage.value = messageFor(error, 'Không thể tải danh sách lớp-môn.')
    loadingState.value = 'error'
  }
}

async function loadYearContext(): Promise<void> {
  const accessToken = token()
  if (!accessToken || selectedAcademicYearId.value === null) {
    schoolClasses.value = []
    semesters.value = []
    selectedClassId.value = null
    selectedSemesterId.value = null
    classSubjects.value = []
    loadingState.value = 'success'
    return
  }
  contextLoading.value = true
  try {
    const [classList, semesterList] = await Promise.all([
      fetchSchoolClasses(accessToken, selectedAcademicYearId.value),
      fetchSemesters(accessToken, selectedAcademicYearId.value),
    ])
    schoolClasses.value = classList
    semesters.value = semesterList
    selectedClassId.value = classList[0]?.id ?? null
    selectedSemesterId.value = semesterList[0]?.id ?? null
  } catch (error) {
    if (isApiError(error, 401)) return
    forbidden.value = isApiError(error, 403)
    errorMessage.value = messageFor(error, 'Không thể tải lớp và học kỳ theo năm học.')
    loadingState.value = 'error'
  } finally {
    contextLoading.value = false
  }
}

async function loadContext(): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  contextLoading.value = true
  try {
    const [years, subjectList] = await Promise.all([fetchAcademicYears(accessToken), fetchSubjects(accessToken)])
    academicYears.value = years
    subjects.value = subjectList
    selectedAcademicYearId.value = years.find((year) => year.status === 'ACTIVE')?.id ?? years[0]?.id ?? null
  } catch (error) {
    if (isApiError(error, 401)) return
    forbidden.value = isApiError(error, 403)
    errorMessage.value = messageFor(error, 'Không thể tải context năm học và môn học.')
    loadingState.value = 'error'
  } finally {
    contextLoading.value = false
  }
}

function openCreate(): void {
  selectedClassSubject.value = null
  dialogMode.value = 'create'
  dialogErrorMessage.value = ''
  conflictMessage.value = ''
  dialogVisible.value = true
}

function openEdit(classSubject: ClassSubject): void {
  selectedClassSubject.value = classSubject
  dialogMode.value = 'edit'
  dialogErrorMessage.value = ''
  conflictMessage.value = ''
  dialogVisible.value = true
}

function closeDialog(): void {
  dialogVisible.value = false
  dialogErrorMessage.value = ''
  conflictMessage.value = ''
}

async function save(values: ClassSubjectFormValues): Promise<void> {
  const accessToken = token()
  if (!accessToken || selectedClassId.value === null || selectedSemesterId.value === null) {
    dialogErrorMessage.value = 'Năm học, lớp và học kỳ là bắt buộc.'
    return
  }
  saving.value = true
  dialogErrorMessage.value = ''
  conflictMessage.value = ''
  try {
    if (dialogMode.value === 'edit' && selectedClassSubject.value) {
      await updateClassSubject(accessToken, selectedClassSubject.value.id, { status: values.status })
      statusMessage.value = 'Đã cập nhật trạng thái lớp-môn.'
    } else if (values.subjectId !== null) {
      await createClassSubject(accessToken, { classId: selectedClassId.value, subjectId: values.subjectId, semesterId: selectedSemesterId.value, status: 'ACTIVE' })
      statusMessage.value = 'Đã gán môn cho lớp.'
    } else {
      dialogErrorMessage.value = 'Môn học là bắt buộc.'
      return
    }
    await loadClassSubjects()
    closeDialog()
  } catch (error) {
    if (isApiError(error, 401)) return
    if (isApiError(error, 409)) conflictMessage.value = messageFor(error, 'Môn chưa được cấu hình applicability hoặc đã tồn tại trong lớp-môn.')
    else dialogErrorMessage.value = messageFor(error, 'Không thể lưu lớp-môn.')
  } finally {
    saving.value = false
  }
}

function configureApplicability(): void {
  closeDialog()
  void router.push({ name: 'v2-academic-subjects' })
}

watch(selectedAcademicYearId, () => { void loadYearContext() })
watch([selectedClassId, selectedSemesterId], () => { void loadClassSubjects() })
onMounted(() => { void loadContext() })
</script>

<template>
  <div class="page-heading">
    <div>
      <p class="eyebrow">Academic catalog</p>
      <h1>Quản lí môn học các lớp</h1>
      <p>Gán môn học và cập nhật trạng thái theo lớp, năm học và học kỳ.</p>
    </div>
    <div class="page-heading-actions">
      <Button label="Môn học" icon="pi pi-book" severity="secondary" outlined @click="router.push({ name: 'v2-academic-subjects' })" />
      <Button label="Thêm môn cho lớp" icon="pi pi-plus" :disabled="!selectedClassId || !selectedSemesterId || classClosed || semesterClosed" @click="openCreate" />
    </div>
  </div>
  <FormAlert v-if="statusMessage" tone="success" :message="statusMessage" />
  <FormAlert v-if="errorMessage && !forbidden" tone="error" :message="errorMessage" />
  <section class="content-surface">
    <div class="catalog-context-form">
      <div class="field-group"><label for="class-subject-year">Năm học</label><Select id="class-subject-year" v-model="selectedAcademicYearId" :options="academicYears" option-label="code" option-value="id" placeholder="Chọn năm học" :loading="contextLoading" fluid /></div>
      <div class="field-group"><label for="class-subject-class">Lớp</label><Select id="class-subject-class" v-model="selectedClassId" :options="schoolClasses" option-label="classCode" option-value="id" placeholder="Chọn lớp" :disabled="!selectedAcademicYearId" fluid /></div>
      <div class="field-group"><label for="class-subject-semester">Học kỳ</label><Select id="class-subject-semester" v-model="selectedSemesterId" :options="semesters" option-label="name" option-value="id" placeholder="Chọn học kỳ" :disabled="!selectedAcademicYearId" fluid /></div>
    </div>
    <div v-if="selectedClass || selectedSemester" class="catalog-context-summary"><span>{{ selectedClass?.classCode ?? 'Chưa chọn lớp' }}</span><span>{{ selectedSemester?.name ?? 'Chưa chọn học kỳ' }}</span><span>{{ classClosed || semesterClosed ? 'Chỉ xem' : 'Có thể chỉnh sửa' }}</span></div>
  </section>
  <section class="content-surface">
    <PageState :state="pageState" :forbidden="forbidden" forbidden-message="Bạn không có quyền xem danh sách lớp-môn." :error-message="errorMessage" empty-heading="Chưa có môn trong lớp" empty-message="Chọn đủ context hoặc gán môn đầu tiên cho lớp này." @retry="loadClassSubjects">
      <ClassSubjectTable :class-subjects="classSubjects" :subjects="subjects" :semesters="semesters" :read-only="classClosed || semesterClosed" @change-status="openEdit" />
    </PageState>
  </section>
  <ClassSubjectDialog v-model:visible="dialogVisible" :mode="dialogMode" :initial-value="selectedClassSubject" :available-subjects="availableSubjects" :class-label="selectedClass?.classCode" :semester-label="selectedSemester?.name" :class-closed="classClosed" :semester-closed="semesterClosed" :saving="saving" :error-message="dialogErrorMessage" :conflict-message="conflictMessage" @save="save" @cancel="closeDialog" @configure-applicability="configureApplicability" />
</template>
