<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
import Button from 'primevue/button'
import ConfirmDialog from 'primevue/confirmdialog'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import { useRouter } from 'vue-router'

import CapacityWarningBanner from '@/components/CapacityWarningBanner.vue'
import SchoolClassDialog from '@/components/SchoolClassDialog.vue'
import SchoolClassTable from '@/components/SchoolClassTable.vue'
import FormAlert from '@/components/FormAlert.vue'
import PageState from '@/components/PageState.vue'
import { clearAuthSession, getAuthSession } from '@/services/authSession'
import { closeSchoolClass, createSchoolClass, deleteSchoolClass, fetchAcademicYears, fetchAcademicYearStatistics, fetchGrades, fetchSchoolClasses, updateSchoolClass } from '@/services/academicApi'
import { isApiError } from '@/types/api'
import type { AcademicYear, AcademicYearStatistics, ClassStatistic, GradeLevel, SchoolClass, SchoolClassFormValues, SchoolClassStatus } from '@/types/academic'
import type { LoadingState } from '@/types/ui'

type StatusFilter = 'ALL' | SchoolClassStatus
type GradeFilter = number | 'ALL'

const router = useRouter()
const confirm = useConfirm()
const academicYears = ref<AcademicYear[]>([])
const grades = ref<GradeLevel[]>([])
const schoolClasses = ref<SchoolClass[]>([])
const selectedAcademicYearId = ref<number | null>(null)
const statusFilter = ref<StatusFilter>('ALL')
const gradeFilter = ref<GradeFilter>('ALL')
const searchText = ref('')
const loadingState = ref<LoadingState>('loading')
const contextLoading = ref(true)
const forbidden = ref(false)
const errorMessage = ref('')
const statusMessage = ref('')
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const selectedClass = ref<SchoolClass | null>(null)
const saving = ref(false)
const dialogErrorMessage = ref('')

const statusOptions: Array<{ label: string; value: StatusFilter }> = [
  { label: 'Tất cả trạng thái', value: 'ALL' },
  { label: 'Đã khởi tạo', value: 'PLANNED' },
  { label: 'Đang hoạt động', value: 'ACTIVE' },
  { label: 'Đã đóng', value: 'CLOSED' },
]
const gradeOptions = computed<Array<{ label: string; value: GradeFilter }>>(() => [
  { label: 'Tất cả khối', value: 'ALL' },
  ...grades.value.map((grade) => ({ label: grade.name, value: grade.id })),
])
const filteredClasses = computed(() => {
  const query = searchText.value.trim().toLocaleLowerCase()
  return schoolClasses.value.filter((schoolClass) => {
    const matchesSearch = !query || schoolClass.classCode.toLocaleLowerCase().includes(query) || schoolClass.className?.toLocaleLowerCase().includes(query)
    const matchesStatus = statusFilter.value === 'ALL' || schoolClass.status === statusFilter.value
    const matchesGrade = gradeFilter.value === 'ALL' || schoolClass.gradeLevelId === gradeFilter.value
    return matchesSearch && matchesStatus && matchesGrade
  })
})
const pageState = computed<LoadingState>(() => loadingState.value === 'success' && filteredClasses.value.length === 0 ? 'empty' : loadingState.value)
const selectedAcademicYear = computed(() => academicYears.value.find((year) => year.id === selectedAcademicYearId.value) ?? null)
const yearStatistics = ref<AcademicYearStatistics | null>(null)

const classStatisticsMap = computed<Record<number, ClassStatistic>>(() => {
  const map: Record<number, ClassStatistic> = {}
  for (const item of yearStatistics.value?.classStatistics ?? []) {
    map[item.classId] = item
  }
  return map
})

const activeWarnings = computed(() => {
  return (yearStatistics.value?.classStatistics ?? [])
    .map((item) => item.warning)
    .filter((warning): warning is NonNullable<typeof warning> => Boolean(warning))
})

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

async function loadClasses(): Promise<void> {
  const accessToken = token()
  if (!accessToken || selectedAcademicYearId.value === null) {
    schoolClasses.value = []
    yearStatistics.value = null
    loadingState.value = 'success'
    return
  }
  loadingState.value = 'loading'
  forbidden.value = false
  errorMessage.value = ''
  try {
    const [classes, stats] = await Promise.all([
      fetchSchoolClasses(accessToken, selectedAcademicYearId.value),
      fetchAcademicYearStatistics(accessToken, selectedAcademicYearId.value).catch(() => null),
    ])
    schoolClasses.value = classes
    yearStatistics.value = stats
    loadingState.value = 'success'
  } catch (error) {
    if (isApiError(error, 401)) return
    forbidden.value = isApiError(error, 403)
    errorMessage.value = messageFor(error, 'Không thể tải danh sách lớp.')
    loadingState.value = 'error'
  }
}

async function loadContext(): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  contextLoading.value = true
  forbidden.value = false
  errorMessage.value = ''
  try {
    const [years, gradeLevels] = await Promise.all([fetchAcademicYears(accessToken), fetchGrades(accessToken)])
    academicYears.value = years
    grades.value = gradeLevels
    selectedAcademicYearId.value = years.find((year) => year.status === 'ACTIVE')?.id ?? years[0]?.id ?? null
  } catch (error) {
    if (isApiError(error, 401)) return
    forbidden.value = isApiError(error, 403)
    errorMessage.value = messageFor(error, 'Không thể tải context năm học và khối.')
    loadingState.value = 'error'
  } finally {
    contextLoading.value = false
  }
}

function openCreate(): void {
  selectedClass.value = null
  dialogMode.value = 'create'
  dialogErrorMessage.value = ''
  dialogVisible.value = true
}

function openEdit(schoolClass: SchoolClass): void {
  selectedClass.value = schoolClass
  dialogMode.value = 'edit'
  dialogErrorMessage.value = ''
  dialogVisible.value = true
}

function closeDialog(): void {
  dialogVisible.value = false
  dialogErrorMessage.value = ''
}

async function save(values: SchoolClassFormValues): Promise<void> {
  const accessToken = token()
  if (!accessToken || values.academicYearId === null || values.gradeLevelId === null) {
    dialogErrorMessage.value = 'Năm học và khối là bắt buộc.'
    return
  }
  saving.value = true
  dialogErrorMessage.value = ''
  try {
    if (dialogMode.value === 'edit' && selectedClass.value) {
      await updateSchoolClass(accessToken, selectedClass.value.id, {
        gradeLevelId: values.gradeLevelId,
        classCode: values.classCode.trim(),
        className: values.className.trim() || null,
        capacity: values.capacity,
        status: values.status,
      })
      statusMessage.value = 'Đã cập nhật lớp.'
    } else {
      await createSchoolClass(accessToken, {
        academicYearId: values.academicYearId,
        gradeLevelId: values.gradeLevelId,
        classCode: values.classCode.trim(),
        className: values.className.trim() || null,
        capacity: values.capacity,
        status: values.status,
      })
      statusMessage.value = 'Đã tạo lớp.'
    }
    await loadClasses()
    closeDialog()
  } catch (error) {
    if (isApiError(error, 401)) return
    dialogErrorMessage.value = messageFor(error, 'Không thể lưu lớp.')
  } finally {
    saving.value = false
  }
}

function confirmClose(schoolClass: SchoolClass): void {
  confirm.require({
    message: `Bạn có chắc muốn đóng lớp ${schoolClass.classCode}? Dữ liệu lịch sử sẽ được giữ nguyên.`,
    header: 'Đóng lớp',
    icon: 'pi pi-lock',
    acceptLabel: 'Đóng lớp',
    rejectLabel: 'Hủy',
    accept: () => { void close(schoolClass) },
  })
}

async function close(schoolClass: SchoolClass): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  errorMessage.value = ''
  try {
    await closeSchoolClass(accessToken, schoolClass.id)
    statusMessage.value = 'Đã đóng lớp.'
    await loadClasses()
  } catch (error) {
    if (isApiError(error, 401)) return
    errorMessage.value = messageFor(error, 'Không thể đóng lớp.')
  }
}

function confirmDelete(schoolClass: SchoolClass): void {
  confirm.require({
    message: `Bạn có chắc muốn xóa lớp ${schoolClass.classCode}? Chỉ lớp chưa phát sinh dữ liệu mới có thể xóa.`,
    header: 'Xóa lớp',
    icon: 'pi pi-trash',
    acceptLabel: 'Xóa lớp',
    rejectLabel: 'Hủy',
    accept: () => { void remove(schoolClass) },
  })
}

async function remove(schoolClass: SchoolClass): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  errorMessage.value = ''
  try {
    await deleteSchoolClass(accessToken, schoolClass.id)
    statusMessage.value = 'Đã xóa lớp.'
    await loadClasses()
  } catch (error) {
    if (isApiError(error, 401)) return
    errorMessage.value = messageFor(error, 'Không thể xóa lớp.')
  }
}

watch(selectedAcademicYearId, () => { void loadClasses() })
onMounted(() => { void loadContext() })
</script>

<template>
  <ConfirmDialog />
  <div class="page-heading">
    <div>
      <p class="eyebrow">Academic structure</p>
      <h1>Lớp học</h1>
      <p v-if="selectedAcademicYear">Năm học {{ selectedAcademicYear.code }} · quản lý danh sách lớp và theo dõi cảnh báo sĩ số.</p>
      <p v-else>Chọn năm học để tải danh sách lớp.</p>
    </div>
    <div class="page-heading-actions">
      <Button label="Danh sách khối" icon="pi pi-sitemap" severity="secondary" outlined @click="router.push({ name: 'v2-academic-grades' })" />
      <Button label="Tạo lớp" icon="pi pi-plus" :disabled="!selectedAcademicYearId" @click="openCreate" />
    </div>
  </div>
  <CapacityWarningBanner
    v-if="selectedAcademicYearId"
    :available="yearStatistics !== null"
    :warning-count="yearStatistics?.totalWarnings ?? 0"
    :warnings="activeWarnings"
    :classes="schoolClasses"
  />
  <FormAlert v-if="statusMessage" tone="success" :message="statusMessage" />
  <FormAlert v-if="errorMessage && !forbidden" tone="error" :message="errorMessage" />
  <section class="content-surface">
    <div class="search-grid">
      <div class="field-group"><label for="school-class-year">Năm học</label><Select id="school-class-year" v-model="selectedAcademicYearId" :options="academicYears" option-label="code" option-value="id" placeholder="Chọn năm học" :loading="contextLoading" fluid /></div>
      <div class="field-group"><label for="school-class-search">Tìm lớp</label><InputText id="school-class-search" v-model="searchText" placeholder="Mã hoặc tên lớp" /></div>
      <div class="field-group"><label for="school-class-grade-filter">Khối</label><Select id="school-class-grade-filter" v-model="gradeFilter" :options="gradeOptions" option-label="label" option-value="value" fluid /></div>
      <div class="field-group"><label for="school-class-status-filter">Trạng thái</label><Select id="school-class-status-filter" v-model="statusFilter" :options="statusOptions" option-label="label" option-value="value" fluid /></div>
    </div>
  </section>
  <section class="content-surface">
    <PageState
      :state="pageState"
      :forbidden="forbidden"
      forbidden-message="Bạn không có quyền xem danh sách lớp."
      :error-message="errorMessage"
      empty-heading="Chưa có lớp"
      empty-message="Năm học hoặc bộ lọc hiện tại chưa có lớp nào."
      @retry="loadClasses"
    >
      <SchoolClassTable
        :school-classes="filteredClasses"
        :grades="grades"
        :class-statistics="classStatisticsMap"
        @edit="openEdit"
        @close="confirmClose"
        @delete="confirmDelete"
      />
    </PageState>
  </section>
  <SchoolClassDialog v-model:visible="dialogVisible" :mode="dialogMode" :initial-value="selectedClass" :academic-years="academicYears" :grades="grades" :saving="saving" :error-message="dialogErrorMessage" @save="save" @cancel="closeDialog" />
</template>
