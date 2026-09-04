<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'

import CalculationTaskDetailModal from '@/components/CalculationTaskDetailModal.vue'
import CalculationTaskTable from '@/components/CalculationTaskTable.vue'
import RetryConfirmationModal from '@/components/RetryConfirmationModal.vue'
import ScoreAuditLogTable from '@/components/ScoreAuditLogTable.vue'
import TranscriptStatusCard from '@/components/TranscriptStatusCard.vue'
import { fetchAcademicYears } from '@/services/academicApi'
import { getAuthSession } from '@/services/authSession'
import {
  fetchCalculationTasks,
  fetchFailedCalculationTasks,
  retryAllFailedCalculationTasks,
  retryCalculationTask,
} from '@/services/calculationTaskApi'
import { fetchScoreAuditLogs } from '@/services/scoreAuditApi'
import { fetchStudents } from '@/services/studentApi'
import { fetchStudentAnnualStatus } from '@/services/transcriptApi'
import type { AcademicYear } from '@/types/academic'
import { extractApiErrorMessage, isApiError } from '@/types/api'
import type { CalculationTaskStatus, ResCalculationTaskDTO } from '@/types/calculationTask'
import type { ResScoreAuditLogDTO } from '@/types/scoreAudit'
import type { ResTranscriptCalculationStatusDTO } from '@/types/transcript'

type OperationsTab = 'tasks' | 'status' | 'audit'

const route = (() => {
  try {
    return useRoute()
  } catch {
    return undefined
  }
})()

const router = (() => {
  try {
    return useRouter()
  } catch {
    return undefined
  }
})()

const session = computed(() => getAuthSession())
const token = computed(() => session.value?.accessToken ?? '')
const userRoles = computed(() => session.value?.user.roles ?? [])

const canOperateTasks = computed(() =>
  !userRoles.value.length || userRoles.value.some((r) => r === 'ADMIN' || r === 'ACADEMIC_OFFICE'),
)

const activeTab = ref<OperationsTab>('tasks')

// Context selectors
const academicYears = ref<AcademicYear[]>([])
const selectedAcademicYearId = ref<number | null>(null)
const contextStudentCode = ref('')

// Task states
const tasks = ref<ResCalculationTaskDTO[]>([])
const taskStatusFilter = ref<CalculationTaskStatus | ''>('FAILED')
const taskStudentCodeFilter = ref('')
const tasksLoading = ref(false)
const tasksPage = ref(0)
const tasksSize = ref(10)
const tasksTotalElements = ref(0)
const tasksTotalPages = ref(0)
const tasksForbidden = ref(false)

// Detail modal
const selectedDetailTask = ref<ResCalculationTaskDTO | null>(null)
const isDetailModalVisible = ref(false)

// Retry modal & action state
const isRetryModalVisible = ref(false)
const retryMode = ref<'single' | 'bulk'>('single')
const targetRetryTask = ref<ResCalculationTaskDTO | null>(null)
const retryingTaskId = ref<number | null>(null)
const retrying = ref(false)

// Transcript status states
const transcriptStatus = ref<ResTranscriptCalculationStatusDTO | null>(null)
const transcriptStudentName = ref('')
const transcriptLoading = ref(false)

// Audit log states
const auditLogs = ref<ResScoreAuditLogDTO[]>([])
const auditLoading = ref(false)
const auditPage = ref(0)
const auditSize = ref(10)
const auditTotalElements = ref(0)
const auditTotalPages = ref(0)
const auditEntityTypeFilter = ref('')
const auditActionFilter = ref('')
const auditStudentCodeFilter = ref('')
const auditForbidden = ref(false)

// Alert banners
const conflictMessage = ref<string | null>(null)
const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)

// Stats
const failedCount = computed(() => tasks.value.filter((t) => t.status === 'FAILED').length)
const runningCount = computed(() => tasks.value.filter((t) => t.status === 'RUNNING').length)
const isInProgressCount = computed(() => (transcriptStatus.value?.calculationStatus === 'IN_PROGRESS' ? 1 : 0))

async function loadAcademicYears(): Promise<void> {
  if (!token.value) return
  try {
    const data = await fetchAcademicYears(token.value)
    academicYears.value = data
    if (data.length > 0 && !selectedAcademicYearId.value) {
      const activeYear = data.find((y) => y.status === 'ACTIVE') ?? data[0]
      selectedAcademicYearId.value = activeYear?.id ?? null
    }
  } catch (error) {
    if (!isApiError(error, 401)) {
      errorMessage.value = extractApiErrorMessage(error, 'Không thể tải danh sách năm học.')
    }
  }
}

async function loadTasks(): Promise<void> {
  if (!token.value) return
  if (!canOperateTasks.value) {
    tasksForbidden.value = true
    tasks.value = []
    return
  }

  tasksLoading.value = true
  tasksForbidden.value = false
  try {
    const filter = {
      status: taskStatusFilter.value || undefined,
      studentCode: taskStudentCodeFilter.value.trim() || undefined,
      academicYearId: selectedAcademicYearId.value ?? undefined,
      page: tasksPage.value,
      size: tasksSize.value,
    }

    let response
    if (taskStatusFilter.value === 'FAILED') {
      response = await fetchFailedCalculationTasks(token.value, filter)
    } else {
      response = await fetchCalculationTasks(token.value, filter)
    }

    tasks.value = response.content
    tasksTotalElements.value = response.totalElements
    tasksTotalPages.value = response.totalPages
  } catch (error) {
    if (isApiError(error, 403)) {
      tasksForbidden.value = true
      tasks.value = []
    } else if (!isApiError(error, 401)) {
      errorMessage.value = extractApiErrorMessage(error, 'Không thể tải danh sách calculation task.')
    }
  } finally {
    tasksLoading.value = false
  }
}

async function loadTranscriptStatus(): Promise<void> {
  if (!token.value || !selectedAcademicYearId.value) return
  const code = contextStudentCode.value.trim() || taskStudentCodeFilter.value.trim()
  if (!code) {
    transcriptStatus.value = null
    return
  }

  transcriptLoading.value = true
  try {
    // Find student ID
    let studentId: number | null = null
    const matchedTask = tasks.value.find((t) => t.studentCode === code)
    if (matchedTask) {
      studentId = matchedTask.studentId
    } else {
      const studentRes = await fetchStudents(token.value, {
        page: 0,
        pageSize: 1,
        search: { studentCode: code, studentName: '' },
      })
      if (studentRes.content && studentRes.content.length > 0) {
        studentId = studentRes.content[0]?.id ?? null
        transcriptStudentName.value = studentRes.content[0]?.fullName ?? ''
      }
    }

    if (studentId) {
      const statusRes = await fetchStudentAnnualStatus(token.value, studentId, selectedAcademicYearId.value)
      transcriptStatus.value = statusRes
    } else {
      transcriptStatus.value = null
    }
  } catch {
    // Transcript status load failure should not disrupt other panels
    transcriptStatus.value = null
  } finally {
    transcriptLoading.value = false
  }
}

async function loadAuditLogs(): Promise<void> {
  if (!token.value) return

  auditLoading.value = true
  auditForbidden.value = false
  try {
    const filter = {
      entityType: auditEntityTypeFilter.value.trim() || undefined,
      action: auditActionFilter.value.trim() || undefined,
      studentCode: auditStudentCodeFilter.value.trim() || undefined,
      page: auditPage.value,
      size: auditSize.value,
    }

    const response = await fetchScoreAuditLogs(token.value, filter)
    auditLogs.value = response.content
    auditTotalElements.value = response.totalElements
    auditTotalPages.value = response.totalPages
  } catch (error) {
    if (isApiError(error, 403)) {
      auditForbidden.value = true
      auditLogs.value = []
    } else if (!isApiError(error, 401)) {
      errorMessage.value = extractApiErrorMessage(error, 'Không thể tải nhật ký audit.')
    }
  } finally {
    auditLoading.value = false
  }
}

async function reloadAll(): Promise<void> {
  conflictMessage.value = null
  errorMessage.value = null
  successMessage.value = null

  if (activeTab.value === 'tasks') {
    await loadTasks()
  } else if (activeTab.value === 'status') {
    await loadTranscriptStatus()
  } else if (activeTab.value === 'audit') {
    await loadAuditLogs()
  }
}

function handleOpenDetail(task: ResCalculationTaskDTO): void {
  selectedDetailTask.value = task
  isDetailModalVisible.value = true
}

function handleOpenSingleRetry(task: ResCalculationTaskDTO): void {
  targetRetryTask.value = task
  retryMode.value = 'single'
  isRetryModalVisible.value = true
}

function handleOpenBulkRetry(): void {
  targetRetryTask.value = null
  retryMode.value = 'bulk'
  isRetryModalVisible.value = true
}

async function handleConfirmRetry(): Promise<void> {
  if (!token.value) return

  retrying.value = true
  conflictMessage.value = null
  errorMessage.value = null
  successMessage.value = null

  try {
    if (retryMode.value === 'single' && targetRetryTask.value) {
      retryingTaskId.value = targetRetryTask.value.taskId
      await retryCalculationTask(token.value, targetRetryTask.value.taskId)
      successMessage.value = `Đã yêu cầu retry task #CT-${targetRetryTask.value.taskId}. Trạng thái đã chuyển về PENDING.`
    } else {
      const retriedList = await retryAllFailedCalculationTasks(token.value)
      successMessage.value = `Đã yêu cầu retry toàn bộ ${retriedList.length} task FAILED. Các task đã chuyển về PENDING.`
    }

    isRetryModalVisible.value = false
    isDetailModalVisible.value = false

    // Refresh affected data
    await loadTasks()
    await loadTranscriptStatus()
    await loadAuditLogs()
  } catch (error) {
    if (isApiError(error, 409)) {
      conflictMessage.value = 'Task đã đổi trạng thái hoặc không còn FAILED. Dữ liệu mới nhất đã được tải; hãy kiểm tra trước khi retry lại.'
      isRetryModalVisible.value = false
      await loadTasks()
      await loadTranscriptStatus()
    } else {
      errorMessage.value = extractApiErrorMessage(error, 'Thao tác retry thất bại. Vui lòng thử lại.')
    }
  } finally {
    retrying.value = false
    retryingTaskId.value = null
  }
}

function handleTaskPageChange(newPage: number, newSize: number): void {
  tasksPage.value = newPage
  tasksSize.value = newSize
  void loadTasks()
}

function handleAuditPageChange(newPage: number, newSize: number): void {
  auditPage.value = newPage
  auditSize.value = newSize
  void loadAuditLogs()
}

// Watch filters
watch(taskStatusFilter, () => {
  tasksPage.value = 0
  void loadTasks()
})

watch(taskStudentCodeFilter, () => {
  tasksPage.value = 0
  void loadTasks()
  if (taskStudentCodeFilter.value) {
    contextStudentCode.value = taskStudentCodeFilter.value
    void loadTranscriptStatus()
  }
})

watch(selectedAcademicYearId, () => {
  tasksPage.value = 0
  void loadTasks()
  void loadTranscriptStatus()
})

watch(activeTab, (tab) => {
  if (router && route) {
    void router.replace({ query: { ...route.query, tab } }).catch(() => undefined)
  }
  if (tab === 'tasks' && tasks.value.length === 0) {
    void loadTasks()
  } else if (tab === 'status' && !transcriptStatus.value) {
    void loadTranscriptStatus()
  } else if (tab === 'audit' && auditLogs.value.length === 0) {
    void loadAuditLogs()
  }
})

onMounted(async () => {
  if (route?.query?.tab) {
    const qTab = String(route.query.tab)
    if (qTab === 'tasks' || qTab === 'status' || qTab === 'audit') {
      activeTab.value = qTab
    }
  }
  if (route?.query?.studentCode) {
    contextStudentCode.value = String(route.query.studentCode)
    taskStudentCodeFilter.value = String(route.query.studentCode)
  }

  await loadAcademicYears()
  await loadTasks()
  if (contextStudentCode.value) {
    await loadTranscriptStatus()
  }
  await loadAuditLogs()
})
</script>

<template>
  <div class="calculation-operations-view">
    <header class="operations-heading">
      <div>
        <p class="eyebrow">Scorebook operations</p>
        <h1 class="page-title">Calculation Task & Audit</h1>
        <p class="page-caption">Theo dõi xử lý nền, retry task lỗi và tra cứu lịch sử thay đổi.</p>
      </div>
    </header>

    <!-- Context bar -->
    <section class="surface context-bar" aria-label="Bộ lọc ngữ cảnh vận hành">
      <div class="field-item">
        <label for="ops-academic-year" class="field-label">Năm học</label>
        <Select
          id="ops-academic-year"
          v-model="selectedAcademicYearId"
          :options="academicYears"
          option-label="code"
          option-value="id"
          placeholder="Chọn năm học"
          class="context-select"
        />
      </div>

      <div class="field-item">
        <label for="ops-student-code" class="field-label">Học sinh / Mã</label>
        <InputText
          id="ops-student-code"
          v-model="contextStudentCode"
          placeholder="Ví dụ: HS0001"
          class="context-input"
          @keyup.enter="reloadAll"
        />
      </div>

      <div class="context-action">
        <Button label="Tải dữ liệu" icon="pi pi-search" @click="reloadAll" />
      </div>
    </section>

    <!-- State Banners -->
    <div class="notice-box notice-info">
      <i class="pi pi-info-circle" />
      <div>
        <strong>HTTP states:</strong>
        <span>
          <strong>401</strong> yêu cầu xử lý session theo policy hiện có ·
          <strong>403</strong> giữ session nhưng thiếu quyền/scope ·
          <strong>404</strong> task hoặc transcript không tồn tại ·
          <strong>409</strong> state đã đổi, refresh trước khi retry.
        </span>
      </div>
    </div>

    <div v-if="conflictMessage" class="notice-box notice-error" data-testid="conflict-banner">
      <i class="pi pi-exclamation-circle" />
      <div>
        <strong>409 Xung đột:</strong>
        <span>{{ conflictMessage }}</span>
      </div>
    </div>

    <div v-if="errorMessage" class="notice-box notice-error">
      <i class="pi pi-times-circle" />
      <div>
        <strong>Lỗi:</strong>
        <span>{{ errorMessage }}</span>
      </div>
    </div>

    <div v-if="successMessage" class="notice-box notice-success">
      <i class="pi pi-check-circle" />
      <div>
        <strong>Thành công:</strong>
        <span>{{ successMessage }}</span>
      </div>
    </div>

    <!-- Quick Stats Cards -->
    <div class="stats-grid">
      <div class="stat-card">
        <span class="stat-title">FAILED</span>
        <strong class="stat-count count-failed">{{ failedCount }}</strong>
        <span class="stat-subtitle">cần kiểm tra</span>
      </div>
      <div class="stat-card">
        <span class="stat-title">RUNNING</span>
        <strong class="stat-count count-running">{{ runningCount }}</strong>
        <span class="stat-subtitle">đang xử lý</span>
      </div>
      <div class="stat-card">
        <span class="stat-title">IN_PROGRESS</span>
        <strong class="stat-count count-progress">{{ isInProgressCount }}</strong>
        <span class="stat-subtitle">transcript</span>
      </div>
      <div class="stat-card">
        <span class="stat-title">Audit events</span>
        <strong class="stat-count">{{ auditTotalElements }}</strong>
        <span class="stat-subtitle">ghi nhận</span>
      </div>
    </div>

    <!-- Tabs Navigation -->
    <div class="tabs-container" role="tablist" aria-label="Điều hướng tab vận hành">
      <button
        class="tab-button"
        :class="{ active: activeTab === 'tasks' }"
        role="tab"
        :aria-selected="activeTab === 'tasks'"
        data-testid="tab-tasks"
        @click="activeTab = 'tasks'"
      >
        Calculation tasks
      </button>
      <button
        class="tab-button"
        :class="{ active: activeTab === 'status' }"
        role="tab"
        :aria-selected="activeTab === 'status'"
        data-testid="tab-status"
        @click="activeTab = 'status'"
      >
        Transcript status
      </button>
      <button
        class="tab-button"
        :class="{ active: activeTab === 'audit' }"
        role="tab"
        :aria-selected="activeTab === 'audit'"
        data-testid="tab-audit"
        @click="activeTab = 'audit'"
      >
        Score audit log
      </button>
    </div>

    <!-- Tab Panels -->
    <section v-if="activeTab === 'tasks'" class="panel" role="tabpanel">
      <div v-if="tasksForbidden" class="forbidden-card" data-testid="tasks-forbidden">
        <div class="forbidden-icon">!</div>
        <h2>403 — Bạn không có quyền vận hành calculation task</h2>
        <p class="text-muted">
          Chỉ có Quản trị viên (ADMIN) và Phòng Giáo vụ (ACADEMIC_OFFICE) mới có quyền quản lý và retry task tính điểm.
        </p>
      </div>
      <CalculationTaskTable
        v-else
        :tasks="tasks"
        :loading="tasksLoading"
        :can-retry="canOperateTasks"
        :retrying-task-id="retryingTaskId"
        :page="tasksPage"
        :size="tasksSize"
        :total-elements="tasksTotalElements"
        :total-pages="tasksTotalPages"
        :status-filter="taskStatusFilter"
        :student-code-filter="taskStudentCodeFilter"
        @view-detail="handleOpenDetail"
        @retry="handleOpenSingleRetry"
        @retry-all-failed="handleOpenBulkRetry"
        @refresh="loadTasks"
        @page-change="handleTaskPageChange"
        @update:status-filter="taskStatusFilter = $event"
        @update:student-code-filter="taskStudentCodeFilter = $event"
      />
    </section>

    <section v-if="activeTab === 'status'" class="panel" role="tabpanel">
      <TranscriptStatusCard
        :status="transcriptStatus"
        :loading="transcriptLoading"
        :student-name="transcriptStudentName"
        @refresh="loadTranscriptStatus"
      />
    </section>

    <section v-if="activeTab === 'audit'" class="panel" role="tabpanel">
      <div v-if="auditForbidden" class="forbidden-card">
        <div class="forbidden-icon">!</div>
        <h2>403 — Bạn không có quyền xem nhật ký kiểm toán</h2>
        <p class="text-muted">Backend scope hoặc vai trò hiện tại không cho phép xem lịch sử thay đổi.</p>
      </div>
      <ScoreAuditLogTable
        v-else
        :logs="auditLogs"
        :loading="auditLoading"
        :page="auditPage"
        :size="auditSize"
        :total-elements="auditTotalElements"
        :total-pages="auditTotalPages"
        :entity-type-filter="auditEntityTypeFilter"
        :action-filter="auditActionFilter"
        :student-code-filter="auditStudentCodeFilter"
        @refresh="loadAuditLogs"
        @page-change="handleAuditPageChange"
        @update:entity-type-filter="auditEntityTypeFilter = $event"
        @update:action-filter="auditActionFilter = $event"
        @update:student-code-filter="auditStudentCodeFilter = $event"
      />
    </section>

    <!-- Modals -->
    <CalculationTaskDetailModal
      v-model:visible="isDetailModalVisible"
      :task="selectedDetailTask"
      :can-retry="canOperateTasks"
      :retrying="retrying"
      @retry="handleOpenSingleRetry"
    />

    <RetryConfirmationModal
      v-model:visible="isRetryModalVisible"
      :mode="retryMode"
      :task="targetRetryTask"
      :failed-count="failedCount"
      :loading="retrying"
      @confirm="handleConfirmRetry"
      @cancel="isRetryModalVisible = false"
    />
  </div>
</template>

<style scoped>
.calculation-operations-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
  color: #182230;
}

.operations-heading {
  margin-bottom: 4px;
}

.eyebrow {
  margin: 0 0 4px;
  color: #176b87;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.page-title {
  margin: 0;
  font-size: 26px;
  font-weight: 800;
}

.page-caption {
  margin: 4px 0 0;
  color: #667085;
  font-size: 14px;
}

.context-bar {
  display: grid;
  grid-template-columns: 240px 240px auto;
  gap: 16px;
  align-items: flex-end;
  padding: 16px 20px;
  background: #ffffff;
  border: 1px solid #dfe4ea;
  border-radius: 12px;
}

.field-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-label {
  font-size: 12px;
  font-weight: 700;
  color: #475467;
}

.notice-box {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 9px;
  font-size: 13px;
  line-height: 1.45;
}

.notice-info {
  background: #e6f4f8;
  border: 1px solid #a8d6e3;
  color: #17556b;
}

.notice-error {
  background: #ffefed;
  border: 1px solid #f5b8b2;
  color: #8f1d14;
}

.notice-success {
  background: #e9f7ef;
  border: 1px solid #a6e2c3;
  color: #18794e;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  padding: 16px 20px;
  background: #ffffff;
  border: 1px solid #dfe4ea;
  border-radius: 12px;
  box-shadow: 0 2px 6px rgba(20, 32, 50, 0.04);
}

.stat-title {
  font-size: 12px;
  font-weight: 700;
  color: #667085;
}

.stat-count {
  font-size: 24px;
  font-weight: 800;
  margin: 4px 0 2px;
  color: #182230;
}

.count-failed {
  color: #b42318;
}

.count-running {
  color: #176b87;
}

.count-progress {
  color: #9a6700;
}

.stat-subtitle {
  font-size: 12px;
  color: #667085;
}

.tabs-container {
  display: flex;
  gap: 6px;
  background: #e9edf1;
  padding: 5px;
  border-radius: 10px;
  width: max-content;
}

.tab-button {
  padding: 9px 16px;
  border: 0;
  border-radius: 7px;
  color: #475467;
  background: transparent;
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.tab-button.active {
  color: #176b87;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(20, 32, 50, 0.09);
}

.forbidden-card {
  padding: 48px 24px;
  text-align: center;
  background: #ffffff;
  border: 1px solid #dfe4ea;
  border-radius: 12px;
}

.forbidden-icon {
  width: 48px;
  height: 48px;
  margin: 0 auto 12px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #fee4e2;
  color: #b42318;
  font-size: 20px;
  font-weight: 800;
}

.text-muted {
  color: #667085;
}

@media (max-width: 900px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .context-bar {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 600px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  .context-bar {
    grid-template-columns: 1fr;
  }
  .tabs-container {
    width: 100%;
    overflow-x: auto;
  }
}
</style>
