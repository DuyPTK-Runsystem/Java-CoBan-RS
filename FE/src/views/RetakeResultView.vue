<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import Button from 'primevue/button'
import Select from 'primevue/select'
import Tag from 'primevue/tag'

import EmptyState from '@/components/EmptyState.vue'
import FormAlert from '@/components/FormAlert.vue'
import PageState from '@/components/PageState.vue'
import RetakeResultDialog from '@/components/retake/RetakeResultDialog.vue'
import RetakeResultTable from '@/components/retake/RetakeResultTable.vue'
import ServerPagination from '@/components/ServerPagination.vue'
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
  { label: 'PLANNED', value: 'PLANNED' },
  { label: 'SCORED', value: 'SCORED' },
  { label: 'CANCELLED', value: 'CANCELLED' },
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

// Dialog state
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'score' | 'cancel'>('create')
const selectedItem = ref<RetakeRowItem | null>(null)
const dialogSaving = ref(false)
const dialogError = ref<string | string[]>('')

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
const hasInProgressCalculation = computed(() =>
  enrichedRows.value.some((row) => row.calculationStatus === 'IN_PROGRESS'),
)
const hasFinishedCalculation = computed(() =>
  enrichedRows.value.some((row) => row.calculationStatus === 'FINISH'),
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

function openCreateDialog(): void {
  selectedItem.value = null
  dialogMode.value = 'create'
  dialogError.value = ''
  dialogVisible.value = true
}

function openScoreDialog(item: RetakeRowItem): void {
  selectedItem.value = item
  dialogMode.value = 'score'
  dialogError.value = ''
  dialogVisible.value = true
}

function openCancelDialog(item: RetakeRowItem): void {
  selectedItem.value = item
  dialogMode.value = 'cancel'
  dialogError.value = ''
  dialogVisible.value = true
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
        '409 Conflict: Record cùng student/year/subject đã tồn tại hoặc lifecycle không cho phép thao tác.',
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
        '409 Conflict: Dữ liệu đã thay đổi hoặc lifecycle không cho phép cập nhật điểm.',
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
        '409 Conflict: Record đã bị hủy hoặc không được phép hủy ở trạng thái hiện tại.',
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
        <div class="eyebrow">Retake result workspace · v2</div>
        <h1>Kết quả thi lại</h1>
        <p class="caption">
          Tra cứu, ghi nhận và theo dõi tác động của điểm thi lại lên bảng điểm năm học.
        </p>
      </div>
      <div class="page-heading-actions">
        <Button
          label="+ Tạo kỳ thi lại"
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
          <p class="caption">Filter gửi server-side; mặc định 10 dòng/trang.</p>
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
            label="Lọc"
            severity="secondary"
            :loading="loading"
            data-testid="btn-filter"
            @click="handleFilter"
          />
          <Button
            label="Làm mới"
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
        <div class="label">Tổng record</div>
        <div class="value" data-testid="metric-total">{{ totalElements }}</div>
      </div>
      <div class="surface metric">
        <div class="label">PLANNED</div>
        <div class="value" data-testid="metric-planned">{{ countPlanned }}</div>
      </div>
      <div class="surface metric">
        <div class="label">SCORED</div>
        <div class="value" data-testid="metric-scored">{{ countScored }}</div>
      </div>
      <div class="surface metric">
        <div class="label">CANCELLED</div>
        <div class="value" data-testid="metric-cancelled">{{ countCancelled }}</div>
      </div>
    </div>

    <!-- Contract info note -->
    <div class="notice info">
      <strong>Contract:</strong>
      <span>
        Tên học sinh/môn trong bảng là display fixture/lookup; API hiện trả numeric IDs. Official after-score đọc từ Transcript API.
      </span>
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
            Trang {{ page + 1 }}/{{ Math.max(totalPages, 1) }} · {{ totalElements }} record.
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

    <!-- Calculation Notes Section -->
    <section class="surface pad content-surface calculation-section">
      <div class="section-head">
        <div>
          <h2>Ghi chú calculation</h2>
          <p class="caption">Thông tin minh họa từ Transcript API, không tính tại FE.</p>
        </div>
        <Tag
          v-if="hasInProgressCalculation"
          value="IN_PROGRESS"
          severity="warn"
          data-testid="tag-calculation-progress"
        />
        <Tag
          v-else-if="hasFinishedCalculation"
          value="FINISH"
          severity="success"
          data-testid="tag-calculation-finish"
        />
        <Tag
          v-else
          value="—"
          severity="secondary"
          data-testid="tag-calculation-none"
        />
      </div>
      <div v-if="hasInProgressCalculation" class="notice warn" data-testid="notice-calculation-in-progress">
        <strong>Đang xử lý:</strong>
        <span>
          Kết quả cũ không được đánh dấu là official mới nhất cho tới khi worker hoàn tất. UI có thể refresh status theo read API.
        </span>
      </div>
      <div v-else-if="hasFinishedCalculation" class="notice info" data-testid="notice-calculation-finish">
        <strong>Hoàn tất:</strong>
        <span>Điểm chính thức đã được đồng bộ từ bảng điểm cả năm.</span>
      </div>
      <div v-else class="notice info">
        <span>Chưa có tác vụ tính toán nào được ghi nhận cho danh sách hiện tại.</span>
      </div>
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

<style scoped>
.page {
  max-width: 1440px;
  margin: 0 auto;
  padding: 24px 20px;
}
.heading {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
  margin-bottom: 22px;
}
.eyebrow {
  color: #697790;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-weight: 700;
  margin-bottom: 6px;
}
.caption {
  color: #697790;
  font-size: 13px;
  margin: 0;
}
.surface {
  background: #fff;
  border: 1px solid #e3e8f0;
  border-radius: 12px;
  box-shadow: 0 4px 18px rgba(39, 60, 93, 0.04);
}
.surface.pad {
  padding: 20px;
}
.context {
  padding: 16px 20px;
  margin-bottom: 18px;
}
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.context-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1fr 1fr auto;
  gap: 12px;
  align-items: end;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.field label {
  font-size: 12px;
  font-weight: 700;
  color: #4d5c75;
}
.filter-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
.summary-grid {
  margin: 18px 0;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
.metric {
  padding: 15px 18px;
}
.metric .label {
  font-size: 12px;
  color: #6c7890;
}
.metric .value {
  font-size: 24px;
  font-weight: 800;
  margin-top: 4px;
  font-variant-numeric: tabular-nums;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.notice {
  border-radius: 9px;
  padding: 11px 14px;
  display: flex;
  gap: 8px;
  font-size: 13px;
  margin: 14px 0;
  align-items: flex-start;
}
.notice.info {
  background: #edf5ff;
  color: #24538d;
  border: 1px solid #cfe3fb;
}
.notice.warn {
  background: #fff7e5;
  color: #79530a;
  border: 1px solid #f2dfaa;
}
.calculation-section {
  margin-top: 20px;
}
.state-banner {
  margin-bottom: 16px;
}

@media (max-width: 900px) {
  .context-grid {
    grid-template-columns: 1fr 1fr;
  }
  .summary-grid {
    grid-template-columns: 1fr 1fr;
  }
  .filter-actions {
    grid-column: 1 / -1;
  }
}
@media (max-width: 560px) {
  .context-grid {
    grid-template-columns: 1fr;
  }
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
