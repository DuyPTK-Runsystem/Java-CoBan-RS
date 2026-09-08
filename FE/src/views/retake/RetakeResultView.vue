<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import Button from 'primevue/button'
import Select from 'primevue/select'

import EmptyState from '@/components/common/EmptyState.vue'
import FormAlert from '@/components/common/FormAlert.vue'
import PageState from '@/components/common/PageState.vue'
import RetakeResultDialog from '@/components/retake/RetakeResultDialog.vue'
import RetakeResultTable from '@/components/retake/RetakeResultTable.vue'
import ServerPagination from '@/components/common/ServerPagination.vue'
import { useRetakeDialogState } from '@/composables/useRetakeDialogState'
import { fetchAcademicYears, fetchSubjects } from '@/services/academicApi'
import { getAuthSession } from '@/services/authSession'
import {
  cancelRetakeExam,
  createRetakeExam,
  fetchRetakeExams,
  updateRetakeScore,
} from '@/services/retakeApi'
import { fetchStudents } from '@/services/studentApi'
import { fetchStudentAnnualTranscript } from '@/services/transcriptApi'
import type { AcademicYear, Subject } from '@/types/academic'
import { extractApiError, extractApiErrorMessage, isApiError } from '@/types/api'
import type {
  ReqCreateRetakeExamDTO,
  ReqUpdateRetakeScoreDTO,
  ResRetakeExamDTO,
  RetakeExamStatus,
  RetakeRowItem,
} from '@/types/retake'
import type { Student } from '@/types/student'

const token = computed(() => getAuthSession()?.accessToken ?? '')

// Filter states
const filterStudentId = ref<number | undefined>(undefined)
const filterAcademicYearId = ref<number | undefined>(undefined)
const filterSubjectId = ref<number | undefined>(undefined)
const filterStatus = ref<RetakeExamStatus | undefined>(undefined)

const statusOptions: Array<{ label: string; value: RetakeExamStatus | undefined }> = [
  { label: 'Tất cả trạng thái', value: undefined },
  { label: 'Chờ nhập điểm', value: 'PLANNED' },
  { label: 'Đã có điểm', value: 'SCORED' },
  { label: 'Đã hủy', value: 'CANCELLED' },
]

// Pagination & Data states
const page = ref(0)
const pageSize = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)
const rawItems = ref<ResRetakeExamDTO[]>([])
const enrichedRows = ref<RetakeRowItem[]>([])
const loading = ref(false)
const errorMessage = ref('')
const pageState = ref<'ready' | 'loading' | 'empty' | 'forbidden' | 'not-found' | 'error'>('ready')

// Lookups
const academicYears = ref<AcademicYear[]>([])
const subjects = ref<Subject[]>([])
const students = ref<Student[]>([])

const {
  visible: dialogVisible, mode: dialogMode, selectedItem,
  saving: dialogSaving, error: dialogError,
  openCreate: openCreateDialog, openScore: openScoreDialog, openCancel: openCancelDialog,
} = useRetakeDialogState()

// Computed counts for metrics
const countPlanned = computed(() =>
  rawItems.value.filter((item) => item.status === 'PLANNED').length,
)
const countScored = computed(() =>
  rawItems.value.filter((item) => item.status === 'SCORED').length,
)
const countCancelled = computed(() =>
  rawItems.value.filter((item) => item.status === 'CANCELLED').length,
)

const studentDropdownOptions = computed(() => [
  { id: undefined, label: 'Tất cả học sinh' },
  ...students.value.map((s) => {
    const student = s as unknown as { id?: number; studentId?: number; studentCode?: string; fullName?: string; studentName?: string }
    const id = student.id ?? student.studentId
    const name = student.fullName ?? student.studentName ?? ''
    return {
      id,
      label: student.studentCode ? `${student.studentCode} · ${name}` : name,
    }
  }),
])

const academicYearDropdownOptions = computed(() => [
  { id: undefined, label: 'Tất cả năm học' },
  ...academicYears.value.map((y) => ({
    id: y.id,
    label: y.code,
  })),
])

const subjectDropdownOptions = computed(() => [
  { id: undefined, label: 'Tất cả môn học' },
  ...subjects.value.map((s) => ({
    id: s.id,
    label: s.name,
  })),
])

const dialogStudentOptions = computed(() =>
  students.value.map((s) => {
    const student = s as unknown as { id?: number; studentId?: number; studentCode?: string; fullName?: string; studentName?: string }
    return {
      id: (student.id ?? student.studentId) as number,
      code: student.studentCode,
      name: student.fullName ?? student.studentName ?? '',
    }
  }),
)

const dialogYearOptions = computed(() =>
  academicYears.value.map((y) => ({
    id: y.id,
    code: y.code,
  })),
)

const dialogSubjectOptions = computed(() =>
  subjects.value.map((s) => ({
    id: s.id,
    name: s.name,
  })),
)

async function loadLookups(): Promise<void> {
  if (!token.value) return
  try {
    const [loadedYears, loadedSubjects, studentPage] = await Promise.allSettled([
      fetchAcademicYears(token.value),
      fetchSubjects(token.value, 'ACTIVE'),
      fetchStudents(token.value, {
        page: 0,
        pageSize: 100,
        sortField: 'studentCode',
        sortOrder: 1,
        search: { studentCode: '', studentName: '', dateOfBirth: null },
      }),
    ])

    if (loadedYears.status === 'fulfilled') academicYears.value = loadedYears.value
    if (loadedSubjects.status === 'fulfilled') subjects.value = loadedSubjects.value
    if (studentPage.status === 'fulfilled') students.value = studentPage.value.content
  } catch {
    // Lookup errors are non-fatal; ID displays are used as fallback
  }
}

async function enrichRow(item: ResRetakeExamDTO): Promise<RetakeRowItem> {
  const student = students.value.find(
    (s) => {
      const st = s as unknown as { id?: number; studentId?: number }
      return (st.id ?? st.studentId) === item.studentId
    },
  ) as unknown as { studentCode?: string; fullName?: string; studentName?: string } | undefined
  const subject = subjects.value.find((s) => s.id === item.subjectId)
  const year = academicYears.value.find((y) => y.id === item.academicYearId)

  let officialDtbmhCn: number | null = null
  let calculationStatus: 'IN_PROGRESS' | 'FINISH' | null = null
  let calculationSource: 'REGULAR' | 'RETAKE' | null = null
  let lastTaskId: number | null = null

  if (item.status !== 'CANCELLED' && token.value) {
    try {
      const annualTranscript = await fetchStudentAnnualTranscript(
        token.value,
        item.studentId,
        item.academicYearId,
      )
      calculationStatus = annualTranscript.calculationStatus
      lastTaskId = annualTranscript.lastCalculationTaskId
      const matchedSubject = annualTranscript.subjects.find(
        (s) => s.subjectId === item.subjectId,
      )
      if (matchedSubject) {
        officialDtbmhCn = matchedSubject.officialDtbmhCn
        calculationSource = matchedSubject.calculationSource
      }
    } catch {
      // Transcript read gap / blocked: display fallback gracefully
    }
  }

  return {
    ...item,
    studentCode: student?.studentCode,
    studentName: student?.fullName ?? student?.studentName,
    academicYearCode: year?.code,
    subjectName: subject?.name,
    officialDtbmhCn,
    calculationStatus,
    calculationSource,
    lastTaskId,
  }
}

async function loadRetakeExams(): Promise<void> {
  if (!token.value) return
  loading.value = true
  errorMessage.value = ''
  pageState.value = 'loading'

  try {
    const pageResult = await fetchRetakeExams(token.value, {
      studentId: filterStudentId.value,
      academicYearId: filterAcademicYearId.value,
      subjectId: filterSubjectId.value,
      status: filterStatus.value,
      page: page.value,
      size: pageSize.value,
    })

    rawItems.value = pageResult.content
    totalElements.value = pageResult.totalElements
    totalPages.value = pageResult.totalPages

    // Enrich rows with lookup and transcript calculation details
    enrichedRows.value = await Promise.all(rawItems.value.map(enrichRow))

    if (enrichedRows.value.length === 0) {
      pageState.value = 'empty'
    } else {
      pageState.value = 'ready'
    }
  } catch (error) {
    if (isApiError(error, 403)) {
      pageState.value = 'forbidden'
      errorMessage.value = extractApiErrorMessage(
        error,
        'Bạn không có quyền quản lý kỳ thi lại. Phiên đăng nhập vẫn được giữ.',
      )
    } else if (isApiError(error, 404)) {
      pageState.value = 'not-found'
      errorMessage.value = extractApiErrorMessage(
        error,
        'Kỳ thi lại không tồn tại hoặc đã không còn truy cập được.',
      )
    } else if (isApiError(error, 409)) {
      pageState.value = 'error'
      errorMessage.value = extractApiErrorMessage(
        error,
        'Bản ghi cùng học sinh/năm học/môn học đã tồn tại hoặc trạng thái không cho phép thao tác. Vui lòng tải lại.',
      )
    } else if (isApiError(error, 401)) {
      pageState.value = 'error'
      errorMessage.value = extractApiErrorMessage(
        error,
        'Phiên đăng nhập không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.',
      )
    } else {
      pageState.value = 'error'
      errorMessage.value = extractApiErrorMessage(
        error,
        'Không thể tải danh sách kỳ thi lại.',
      )
    }
  } finally {
    loading.value = false
  }
}

function handleFilter(): void {
  page.value = 0
  void loadRetakeExams()
}

function handleResetFilter(): void {
  filterStudentId.value = undefined
  filterAcademicYearId.value = undefined
  filterSubjectId.value = undefined
  filterStatus.value = undefined
  page.value = 0
  void loadRetakeExams()
}

function handlePageChange(nextPage: number, nextSize: number): void {
  page.value = nextPage
  pageSize.value = nextSize
  void loadRetakeExams()
}

async function handleDialogCreate(payload: ReqCreateRetakeExamDTO): Promise<void> {
  if (!token.value) return
  dialogSaving.value = true
  dialogError.value = ''
  try {
    await createRetakeExam(token.value, payload)
    dialogVisible.value = false
    page.value = 0
    await loadRetakeExams()
  } catch (error) {
    if (isApiError(error, 409)) {
      dialogError.value = extractApiError(
        error,
        'Học sinh đã có kỳ thi lại cho năm học và môn học này, hoặc trạng thái hiện tại không cho phép thao tác.',
      )
    } else {
      dialogError.value = extractApiError(error, 'Không thể tạo kỳ thi lại.')
    }
  } finally {
    dialogSaving.value = false
  }
}

async function handleDialogScore(
  retakeId: number,
  payload: ReqUpdateRetakeScoreDTO,
): Promise<void> {
  if (!token.value) return
  dialogSaving.value = true
  dialogError.value = ''
  try {
    await updateRetakeScore(token.value, retakeId, payload)
    dialogVisible.value = false
    await loadRetakeExams()
  } catch (error) {
    if (isApiError(error, 409)) {
      dialogError.value = extractApiError(
        error,
        'Dữ liệu đã thay đổi hoặc trạng thái hiện tại không cho phép cập nhật điểm.',
      )
    } else {
      dialogError.value = extractApiError(error, 'Không thể lưu điểm thi lại.')
    }
  } finally {
    dialogSaving.value = false
  }
}

async function handleDialogCancel(retakeId: number): Promise<void> {
  if (!token.value) return
  dialogSaving.value = true
  dialogError.value = ''
  try {
    await cancelRetakeExam(token.value, retakeId)
    dialogVisible.value = false
    await loadRetakeExams()
  } catch (error) {
    if (isApiError(error, 409)) {
      dialogError.value = extractApiError(
        error,
        'Kỳ thi lại đã bị hủy hoặc không thể hủy ở trạng thái hiện tại.',
      )
    } else {
      dialogError.value = extractApiError(error, 'Không thể hủy kỳ thi lại.')
    }
  } finally {
    dialogSaving.value = false
  }
}

onMounted(async () => {
  await loadLookups()
  await loadRetakeExams()
})
</script>

<template>
  <main class="page content-page" data-testid="retake-view">
    <div class="heading page-heading">
      <div>
        <h1>Kết quả thi lại</h1>
      </div>
      <div class="page-heading-actions">
        <Button
          label="Tạo kỳ thi lại"
          :disabled="pageState === 'forbidden'"
          data-testid="btn-open-create"
          @click="openCreateDialog"
        />
      </div>
    </div>

    <!-- Error / Status Notice Banner -->
    <FormAlert
      v-if="errorMessage"
      tone="error"
      :message="errorMessage"
      class="state-banner"
      data-testid="view-error-banner"
    />

    <!-- Filter Section -->
    <section class="surface context content-surface">
      <div class="section-head">
        <div>
          <h2>Bộ lọc</h2>
        </div>
      </div>
      <div class="context-grid">
        <div class="field">
          <label for="filter-student">Học sinh</label>
          <Select
            id="filter-student"
            v-model="filterStudentId"
            :options="studentDropdownOptions"
            option-value="id"
            option-label="label"
            placeholder="Tất cả học sinh"
            fluid
            data-testid="filter-student"
          />
        </div>
        <div class="field">
          <label for="filter-year">Năm học</label>
          <Select
            id="filter-year"
            v-model="filterAcademicYearId"
            :options="academicYearDropdownOptions"
            option-value="id"
            option-label="label"
            placeholder="Tất cả năm học"
            fluid
            data-testid="filter-year"
          />
        </div>
        <div class="field">
          <label for="filter-subject">Môn học</label>
          <Select
            id="filter-subject"
            v-model="filterSubjectId"
            :options="subjectDropdownOptions"
            option-value="id"
            option-label="label"
            placeholder="Tất cả môn"
            fluid
            data-testid="filter-subject"
          />
        </div>
        <div class="field">
          <label for="filter-status">Trạng thái</label>
          <Select
            id="filter-status"
            v-model="filterStatus"
            :options="statusOptions"
            option-value="value"
            option-label="label"
            placeholder="Tất cả"
            fluid
            data-testid="filter-status"
          />
        </div>
        <div class="filter-actions">
          <Button
            label="Tìm kiếm"
            severity="secondary"
            :loading="loading"
            data-testid="btn-filter"
            @click="handleFilter"
          />
          <Button
            label="Xóa bộ lọc"
            severity="secondary"
            icon="pi pi-refresh"
            :loading="loading"
            data-testid="btn-refresh"
            @click="loadRetakeExams"
          />
        </div>
      </div>
    </section>

    <!-- Summary metrics -->
    <div class="summary-grid">
      <div class="surface metric">
        <div class="label">Tổng số</div>
        <div class="value" data-testid="metric-total">{{ totalElements }}</div>
      </div>
      <div class="surface metric">
        <div class="label">Chờ nhập điểm</div>
        <div class="value" data-testid="metric-planned">{{ countPlanned }}</div>
      </div>
      <div class="surface metric">
        <div class="label">Đã có điểm</div>
        <div class="value" data-testid="metric-scored">{{ countScored }}</div>
      </div>
      <div class="surface metric">
        <div class="label">Đã hủy</div>
        <div class="value" data-testid="metric-cancelled">{{ countCancelled }}</div>
      </div>
    </div>

    <!-- List panel -->
    <section v-if="pageState === 'loading' && enrichedRows.length === 0" class="surface pad content-surface">
      <PageState state="loading" />
    </section>

    <section
      v-else-if="pageState === 'forbidden'"
      class="surface pad content-surface"
      data-testid="panel-forbidden"
    >
      <div class="notice warn">
        <strong>403 Forbidden:</strong>
        <span>{{ errorMessage || 'Bạn không có quyền quản lý kỳ thi lại. Phiên đăng nhập vẫn được giữ; backend là nơi quyết định quyền.' }}</span>
      </div>
    </section>

    <section
      v-else-if="pageState === 'not-found'"
      class="surface pad content-surface"
      data-testid="panel-not-found"
    >
      <div class="notice warn">
        <strong>404 Không tìm thấy:</strong>
        <span>{{ errorMessage || 'Kỳ thi lại không tồn tại hoặc đã không còn truy cập được.' }}</span>
      </div>
      <Button label="Tải lại danh sách" severity="secondary" icon="pi pi-refresh" @click="loadRetakeExams" />
    </section>

    <section
      v-else-if="pageState === 'error'"
      class="surface pad content-surface"
      data-testid="panel-error"
    >
      <div class="notice error">
        <strong>Lỗi:</strong>
        <span>{{ errorMessage || 'Không thể tải danh sách kỳ thi lại.' }}</span>
      </div>
      <Button label="Thử lại" severity="secondary" icon="pi pi-refresh" @click="loadRetakeExams" />
    </section>

    <section
      v-else-if="pageState === 'empty'"
      class="surface empty content-surface"
      data-testid="panel-empty"
    >
      <EmptyState
        heading="Không có kỳ thi lại phù hợp"
        message="Thử bỏ bớt filter hoặc chọn một năm học khác."
        action-label="Xóa bộ lọc"
        @action="handleResetFilter"
      />
    </section>

    <section v-else class="surface pad content-surface" data-testid="panel-list">
      <div class="toolbar">
        <div>
          <h2>Danh sách kỳ thi lại</h2>
          <p class="caption">
            Trang {{ page + 1 }}/{{ Math.max(totalPages, 1) }} · {{ totalElements }} kết quả.
          </p>
        </div>
      </div>

      <RetakeResultTable
        :items="enrichedRows"
        :loading="loading"
        @edit-score="openScoreDialog"
        @cancel="openCancelDialog"
      />

      <ServerPagination
        v-if="totalElements > 0"
        :page="page"
        :page-size="pageSize"
        :total-records="totalElements"
        data-testid="retake-pagination"
        @page-change="handlePageChange"
      />
    </section>

    <!-- Unified Dialog -->
    <RetakeResultDialog
      v-model:visible="dialogVisible"
      :mode="dialogMode"
      :item="selectedItem"
      :students="dialogStudentOptions"
      :academic-years="dialogYearOptions"
      :subjects="dialogSubjectOptions"
      :saving="dialogSaving"
      :error-message="dialogError"
      @submit-create="handleDialogCreate"
      @submit-score="handleDialogScore"
      @submit-cancel="handleDialogCancel"
    />
  </main>
</template>

<style scoped src="@/styles/retake-results.css"></style>
