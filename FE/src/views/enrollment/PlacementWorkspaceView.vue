<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'

import PlacementResultDetailDialog from '@/components/enrollment/PlacementResultDetailDialog.vue'
import PlacementSessionSetup from '@/components/enrollment/PlacementSessionSetup.vue'
import PlacementWorkspaceReview from '@/components/enrollment/PlacementWorkspaceReview.vue'
import { useAuthSession } from '@/composables/useAuthSession'
import { fetchAcademicYears, fetchGrades, fetchSchoolClasses } from '@/services/academicApi'
import { fetchUnassignedStudents } from '@/services/enrollmentApi'
import {
  cancelPlacementSession,
  confirmPlacementSession,
  createPlacementSession,
  fetchPlacementResults,
  getPlacementSession,
  simulatePlacementSession,
  updatePlacementSession,
} from '@/services/placementApi'
import { getStudent } from '@/services/studentApi'
import { isApiError } from '@/types/api'
import type { AcademicYear, GradeLevel, SchoolClass } from '@/types/academic'
import type { UnassignedStudent } from '@/types/enrollment'
import type {
  CreatePlacementSessionRequest,
  PlacementClassProfile,
  PlacementResult,
  PlacementResultsMeta,
  PlacementSession,
} from '@/types/placement'

const router = useRouter()
const route = useRoute()
const { requireAccessToken: token } = useAuthSession()

// Route mode
const isNewSessionRoute = computed(() => {
  return route.name === 'v2-placement-new' || route.path.endsWith('/new')
})

const placementSessionId = computed<number | null>(() => {
  const param = route.params.placementSessionId
  if (!param) return null
  const parsed = Number(param)
  return Number.isNaN(parsed) || parsed <= 0 ? null : parsed
})

// Setup state
const academicYears = ref<AcademicYear[]>([])
const grades = ref<GradeLevel[]>([])
const classes = ref<SchoolClass[]>([])
const unassignedStudents = ref<UnassignedStudent[]>([])

const loadingContext = ref(false)
const loadingClasses = ref(false)
const loadingUnassigned = ref(false)
const setupSaving = ref(false)
const setupErrorMessage = ref('')
const setupForbidden = ref(false)

// Review / Session state
const session = ref<PlacementSession | null>(null)
const results = ref<PlacementResult[]>([])
const resultsMeta = ref<PlacementResultsMeta | null>(null)
const studentsCache = ref<Record<number, { studentCode: string; studentName: string }>>({})
const loadingResults = ref(false)
const reviewState = ref<'ready' | 'loading' | 'empty' | 'forbidden' | 'conflict' | 'error'>('ready')
const reviewSaving = ref(false)
const reviewErrorMessage = ref('')
const reviewForbiddenMessage = ref('Bạn không có quyền xem hoặc thao tác phiên xếp lớp này. Phiên đăng nhập vẫn được giữ.')

// Dialogs
const detailResult = ref<PlacementResult | null>(null)
const detailDialogVisible = ref(false)

const confirmDialogVisible = ref(false)
const confirmIntentKey = ref<string>('')

const cancelDialogVisible = ref(false)

// Academic labels
const currentAcademicYearLabel = computed(() => {
  if (!session.value) return ''
  const year = academicYears.value.find((y) => y.id === session.value?.academicYearId)
  return year?.code ? `Năm học ${year.code}` : `Năm học #${session.value.academicYearId}`
})

const currentGradeLabel = computed(() => {
  if (!session.value) return ''
  const grade = grades.value.find((g) => g.id === session.value?.targetGradeId)
  return grade?.name || `Khối #${session.value.targetGradeId}`
})

const detailStudentLabel = computed(() => {
  if (!detailResult.value) return ''
  const cached = studentsCache.value[detailResult.value.studentId]
  if (cached) return `${cached.studentCode} - ${cached.studentName}`
  return `Học sinh #${detailResult.value.studentId}`
})

const detailTargetClassLabel = computed(() => {
  if (!detailResult.value || !detailResult.value.targetClassId) return '—'
  const target = session.value?.targetClasses.find((c) => c.classId === detailResult.value?.targetClassId)
  return target?.className || target?.classCode || `Lớp #${detailResult.value.targetClassId}`
})

// Load context for setup or label lookup
async function loadAcademicContext(): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  loadingContext.value = true
  try {
    const [years, gradesList] = await Promise.all([
      fetchAcademicYears(accessToken),
      fetchGrades(accessToken),
    ])
    academicYears.value = years
    grades.value = gradesList
  } catch (error) {
    if (isApiError(error, 401)) return
    if (isApiError(error, 403)) {
      setupForbidden.value = true
      reviewState.value = 'forbidden'
    }
  } finally {
    loadingContext.value = false
  }
}

// Load classes and unassigned students for a specific academic year
async function loadYearDetails(yearId: number): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  loadingClasses.value = true
  loadingUnassigned.value = true
  try {
    const [classesList, unassignedList] = await Promise.all([
      fetchSchoolClasses(accessToken, yearId),
      fetchUnassignedStudents(accessToken, yearId),
    ])
    classes.value = classesList
    unassignedStudents.value = unassignedList
  } catch (error) {
    if (isApiError(error, 401)) return
    if (isApiError(error, 403)) {
      setupForbidden.value = true
      reviewState.value = 'forbidden'
    }
  } finally {
    loadingClasses.value = false
    loadingUnassigned.value = false
  }
}

// Hydrate student names for the current results page
async function hydrateStudents(studentIds: number[]): Promise<void> {
  const accessToken = token()
  if (!accessToken) return

  const neededIds = Array.from(new Set(studentIds)).filter((id) => !studentsCache.value[id])
  if (neededIds.length === 0) return

  await Promise.allSettled(
    neededIds.map(async (studentId) => {
      try {
        const student = await getStudent(accessToken, studentId)
        studentsCache.value[studentId] = {
          studentCode: student.studentCode,
          studentName: student.fullName,
        }
      } catch {
        // Leave unmapped, fallback to "Học sinh #id"
      }
    }),
  )
}

// Load placement results with zero-based pagination
async function loadResults(sessionId: number, page = 0, pageSize = 20): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  loadingResults.value = true
  try {
    const resPage = await fetchPlacementResults(accessToken, sessionId, page, pageSize)
    results.value = resPage.result
    resultsMeta.value = resPage.meta
    const studentIds = resPage.result.map((r) => r.studentId)
    void hydrateStudents(studentIds)
  } catch (error) {
    if (isApiError(error, 401)) return
    if (isApiError(error, 403)) {
      reviewState.value = 'forbidden'
      reviewForbiddenMessage.value = 'Bạn không có quyền xem kết quả của phiên xếp lớp này. Phiên đăng nhập vẫn được giữ.'
    } else if (isApiError(error, 404)) reviewState.value = 'empty'
    else if (isApiError(error, 409)) reviewState.value = 'conflict'
    else reviewState.value = 'error'
  } finally {
    loadingResults.value = false
  }
}

// Load placement session
async function loadSession(sessionId: number): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  reviewState.value = 'loading'
  reviewErrorMessage.value = ''
  reviewForbiddenMessage.value = 'Bạn không có quyền xem hoặc thao tác phiên xếp lớp này. Phiên đăng nhập vẫn được giữ.'
  try {
    session.value = await getPlacementSession(accessToken, sessionId)
    reviewState.value = 'ready'
    await loadResults(sessionId, 0, 20)
  } catch (error) {
    if (isApiError(error, 401)) return
    if (isApiError(error, 403)) {
      reviewState.value = 'forbidden'
      reviewForbiddenMessage.value = 'Bạn không có quyền xem phiên xếp lớp này. Phiên đăng nhập vẫn được giữ.'
    } else if (isApiError(error, 404)) {
      reviewState.value = 'empty'
    } else if (isApiError(error, 409)) {
      reviewState.value = 'conflict'
    } else {
      reviewState.value = 'error'
      reviewErrorMessage.value = error instanceof Error ? error.message : 'Không thể tải phiên xếp lớp.'
    }
  }
}

// Setup handlers
async function handleCreateSession(payload: CreatePlacementSessionRequest): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  setupSaving.value = true
  setupErrorMessage.value = ''
  setupForbidden.value = false
  try {
    const createdSession = await createPlacementSession(accessToken, payload)
    await router.replace({
      name: 'v2-placement-session',
      params: { placementSessionId: String(createdSession.id) },
    })
  } catch (error) {
    if (isApiError(error, 401)) return
    if (isApiError(error, 403)) {
      setupForbidden.value = true
      setupErrorMessage.value = 'Bạn không có quyền tạo phiên xếp lớp này. Phiên đăng nhập vẫn được giữ.'
    } else if (isApiError(error, 409)) {
      setupErrorMessage.value = 'Xung đột dữ liệu phiên xếp lớp. Vui lòng tải lại trang và thử lại.'
    } else {
      setupErrorMessage.value = error instanceof Error ? error.message : 'Không thể tạo phiên xếp lớp.'
    }
  } finally {
    setupSaving.value = false
  }
}

function handleCancelSetup(): void {
  void router.push({ name: 'v2-enrollments' })
}

// Review handlers
async function handleUpdateProfile(classId: number, newProfile: PlacementClassProfile): Promise<void> {
  const accessToken = token()
  if (!accessToken || !session.value) return
  if (session.value.status !== 'DRAFT') return

  reviewSaving.value = true
  try {
    const updatedTargetClasses = session.value.targetClasses.map((target) => ({
      classId: target.classId,
      profile: target.classId === classId ? newProfile : target.profile,
      capacity: null,
    }))
    session.value = await updatePlacementSession(accessToken, session.value.id, {
      expectedVersion: session.value.version,
      targetClasses: updatedTargetClasses,
    })
  } catch (error) {
    if (isApiError(error, 401)) return
    if (isApiError(error, 403)) {
      reviewState.value = 'forbidden'
      reviewForbiddenMessage.value = 'Bạn không có quyền thay đổi cách phân lớp của phiên này. Phiên đăng nhập vẫn được giữ.'
    } else if (isApiError(error, 409)) {
      reviewState.value = 'conflict'
    } else {
      reviewErrorMessage.value = error instanceof Error ? error.message : 'Không thể cập nhật cách phân lớp.'
    }
  } finally {
    reviewSaving.value = false
  }
}

async function handleSimulate(): Promise<void> {
  const accessToken = token()
  if (!accessToken || !session.value) return

  reviewSaving.value = true
  try {
    session.value = await simulatePlacementSession(accessToken, session.value.id, session.value.version)
    await loadResults(session.value.id, 0, resultsMeta.value?.pageSize ?? 20)
  } catch (error) {
    if (isApiError(error, 401)) return
    if (isApiError(error, 403)) {
      reviewState.value = 'forbidden'
      reviewForbiddenMessage.value = 'Bạn không có quyền mô phỏng phiên xếp lớp này. Phiên đăng nhập vẫn được giữ.'
    } else if (isApiError(error, 409)) {
      reviewState.value = 'conflict'
    } else {
      reviewErrorMessage.value = error instanceof Error ? error.message : 'Không thể mô phỏng phiên xếp lớp.'
    }
  } finally {
    reviewSaving.value = false
  }
}

function handlePageChange(page: number, pageSize: number): void {
  if (!session.value) return
  void loadResults(session.value.id, page, pageSize)
}

function openConfirmDialog(): void {
  confirmIntentKey.value = crypto.randomUUID()
  confirmDialogVisible.value = true
}

async function executeConfirm(): Promise<void> {
  const accessToken = token()
  if (!accessToken || !session.value) return

  reviewSaving.value = true
  confirmDialogVisible.value = false
  try {
    session.value = await confirmPlacementSession(accessToken, session.value.id, {
      expectedVersion: session.value.version,
      idempotencyKey: confirmIntentKey.value,
    })
    await loadResults(session.value.id, 0, resultsMeta.value?.pageSize ?? 20)
  } catch (error) {
    if (isApiError(error, 401)) return
    if (isApiError(error, 403)) {
      reviewState.value = 'forbidden'
      reviewForbiddenMessage.value = 'Bạn không có quyền xác nhận phiên xếp lớp này. Phiên đăng nhập vẫn được giữ.'
    } else if (isApiError(error, 409)) {
      reviewState.value = 'conflict'
    } else {
      reviewErrorMessage.value = error instanceof Error ? error.message : 'Không thể xác nhận xếp lớp.'
    }
  } finally {
    reviewSaving.value = false
  }
}

function openCancelDialog(): void {
  cancelDialogVisible.value = true
}

async function executeCancel(): Promise<void> {
  const accessToken = token()
  if (!accessToken || !session.value) return

  reviewSaving.value = true
  cancelDialogVisible.value = false
  try {
    session.value = await cancelPlacementSession(accessToken, session.value.id, session.value.version)
    await loadResults(session.value.id, 0, resultsMeta.value?.pageSize ?? 20)
  } catch (error) {
    if (isApiError(error, 401)) return
    if (isApiError(error, 403)) {
      reviewState.value = 'forbidden'
      reviewForbiddenMessage.value = 'Bạn không có quyền hủy phiên xếp lớp này. Phiên đăng nhập vẫn được giữ.'
    } else if (isApiError(error, 409)) {
      reviewState.value = 'conflict'
    } else {
      reviewErrorMessage.value = error instanceof Error ? error.message : 'Không thể hủy phiên xếp lớp.'
    }
  } finally {
    reviewSaving.value = false
  }
}

function handleViewReason(result: PlacementResult): void {
  detailResult.value = result
  detailDialogVisible.value = true
}

function handleContinueManualFromDetail(): void {
  detailDialogVisible.value = false
  void router.push({ name: 'v2-enrollments' })
}

function handleRetryReview(): void {
  if (placementSessionId.value) {
    void loadSession(placementSessionId.value)
  }
}

// Initial setup or route watch
watch(placementSessionId, (newId) => {
  if (newId) {
    void loadSession(newId)
  }
}, { immediate: true })

onMounted(() => {
  void loadAcademicContext()
})
</script>

<template>
  <div class="placement-workspace-view">
    <!-- View tạo mới phiên -->
    <PlacementSessionSetup
      v-if="isNewSessionRoute"
      :academic-years="academicYears"
      :grades="grades"
      :classes="classes"
      :unassigned-students="unassignedStudents"
      :loading-context="loadingContext"
      :loading-classes="loadingClasses"
      :loading-unassigned="loadingUnassigned"
      :saving="setupSaving"
      :error-message="setupErrorMessage"
      :forbidden="setupForbidden"
      @update:academic-year-id="loadYearDetails"
      @submit="handleCreateSession"
      @cancel="handleCancelSetup"
    />

    <!-- View xem phiên / mô phỏng / xác nhận -->
    <PlacementWorkspaceReview
      v-else
      :session="session"
      :results="results"
      :results-meta="resultsMeta"
      :student-names-by-id="studentsCache"
      :loading-results="loadingResults"
      :review-state="reviewState"
      :forbidden-message="reviewForbiddenMessage"
      :editable="session?.status === 'DRAFT'"
      :saving="reviewSaving"
      :academic-year-label="currentAcademicYearLabel"
      :grade-label="currentGradeLabel"
      @simulate="handleSimulate"
      @confirm="openConfirmDialog"
      @cancel="openCancelDialog"
      @retry="handleRetryReview"
      @view-reason="handleViewReason"
      @update-profile="handleUpdateProfile"
      @page-change="handlePageChange"
    />

    <!-- Dialog xem lý do chi tiết -->
    <PlacementResultDetailDialog
      v-model:visible="detailDialogVisible"
      :result="detailResult"
      :student-label="detailStudentLabel"
      :target-class-label="detailTargetClassLabel"
      @continue-manual="handleContinueManualFromDetail"
    />

    <!-- Dialog xác nhận Confirm -->
    <Dialog
      v-model:visible="confirmDialogVisible"
      modal
      header="Xác nhận xếp lớp tự động"
      :style="{ width: '480px', maxWidth: '95vw' }"
    >
      <div class="confirm-dialog-content">
        <p>
          Chỉ các học sinh có kết quả <strong>Tự động</strong> mới được tạo ghi danh vào lớp đích tương ứng.
        </p>
        <p class="text-amber-800">
          Các học sinh có trạng thái <strong>Cần xếp thủ công</strong> sẽ không được tạo ghi danh tự động và sẽ tiếp tục được giáo vụ xử lý tại màn hình Xếp lớp.
        </p>
        <p>Bạn có chắc chắn muốn xác nhận kết quả phân lớp này?</p>
      </div>
      <template #footer>
        <Button label="Hủy" severity="secondary" text :disabled="reviewSaving" @click="confirmDialogVisible = false" />
        <Button label="Xác nhận" icon="pi pi-check" :loading="reviewSaving" @click="executeConfirm" />
      </template>
    </Dialog>

    <!-- Dialog xác nhận Hủy phiên -->
    <Dialog
      v-model:visible="cancelDialogVisible"
      modal
      header="Hủy phiên xếp lớp"
      :style="{ width: '440px', maxWidth: '95vw' }"
    >
      <div class="confirm-dialog-content">
        <p>Bạn có chắc chắn muốn hủy phiên xếp lớp này?</p>
        <p class="text-muted">Sau khi hủy, phiên xếp lớp sẽ bị khóa và chuyển sang trạng thái chỉ đọc.</p>
      </div>
      <template #footer>
        <Button label="Đóng" severity="secondary" text :disabled="reviewSaving" @click="cancelDialogVisible = false" />
        <Button label="Hủy phiên" severity="danger" icon="pi pi-times" :loading="reviewSaving" @click="executeCancel" />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.placement-workspace-view {
  width: 100%;
}
.confirm-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 0.95rem;
  line-height: 1.5;
}
.confirm-dialog-content p {
  margin: 0;
}
.text-amber-800 {
  color: #92400e;
}
.text-muted {
  color: var(--p-text-muted-color, #6b7280);
  font-size: 0.85rem;
}
</style>
