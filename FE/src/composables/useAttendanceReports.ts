import { ref, type ComputedRef, type Ref } from 'vue'

import { fetchSemesters } from '@/services/academicApi'
import { fetchClassAttendanceSummary, fetchStudentAttendanceHistory } from '@/services/attendanceApi'
import { isApiError } from '@/types/api'
import type { AcademicYear, SchoolClass, Semester } from '@/types/academic'
import type { ClassAttendanceSummaryResponse, StudentAttendanceHistoryResponse } from '@/types/attendance'

type AccessToken = () => string | null

interface AttendanceReportContext {
  accessToken: AccessToken
  isStudent: ComputedRef<boolean>
  classes: Ref<SchoolClass[]>
  semesters: Ref<Semester[]>
}

function messageFor(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

function validateDateRange(from: string, to: string): string {
  return from && to && from > to ? 'Từ ngày phải nhỏ hơn hoặc bằng đến ngày.' : ''
}

export function useAttendanceReports(context: AttendanceReportContext) {
  const historyAcademicYearId = ref<number | null>(null)
  const historySemesterId = ref<number | null>(null)
  const historySemesters = ref<Semester[]>([])
  const historyFrom = ref('')
  const historyTo = ref('')
  const historyPage = ref(0)
  const historyPageSize = ref(10)
  const historyResponse = ref<StudentAttendanceHistoryResponse | null>(null)
  const historyLoading = ref(false)
  const historyError = ref('')
  const historyForbidden = ref(false)

  const summaryClassId = ref<number | null>(null)
  const summarySemesterId = ref<number | null>(null)
  const summaryFrom = ref('')
  const summaryTo = ref('')
  const summaryPage = ref(0)
  const summaryPageSize = ref(20)
  const summaryResponse = ref<ClassAttendanceSummaryResponse | null>(null)
  const summaryLoading = ref(false)
  const summaryError = ref('')
  const summaryForbidden = ref(false)

  function reset(): void {
    historyResponse.value = null
    summaryResponse.value = null
    historyError.value = ''
    summaryError.value = ''
    historyForbidden.value = false
    summaryForbidden.value = false
  }

  function applyYearContext(academicYearId: number, semesterList: Semester[], firstClass: SchoolClass | undefined, activeSemester: Semester | undefined): void {
    historyAcademicYearId.value = academicYearId
    historySemesters.value = semesterList
    historySemesterId.value = activeSemester?.id ?? null
    summaryClassId.value = firstClass?.id ?? null
    summarySemesterId.value = activeSemester?.id ?? null
    summaryFrom.value = activeSemester?.startDate ?? ''
    summaryTo.value = activeSemester?.endDate ?? ''
  }

  async function loadHistory(): Promise<void> {
    const accessToken = context.accessToken()
    if (!accessToken) return
    const rangeError = validateDateRange(historyFrom.value, historyTo.value)
    if (rangeError) {
      historyError.value = rangeError
      historyResponse.value = null
      return
    }
    historyLoading.value = true
    historyError.value = ''
    historyForbidden.value = false
    try {
      historyResponse.value = await fetchStudentAttendanceHistory(accessToken, {
        academicYearId: historyAcademicYearId.value,
        semesterId: historySemesterId.value,
        from: historyFrom.value || undefined,
        to: historyTo.value || undefined,
        page: historyPage.value,
        size: historyPageSize.value,
      })
    } catch (caughtError) {
      if (isApiError(caughtError, 401)) return
      historyForbidden.value = isApiError(caughtError, 403)
      historyError.value = messageFor(caughtError, 'Không thể tải lịch sử chuyên cần.')
    } finally {
      historyLoading.value = false
    }
  }

  function searchHistory(): void {
    historyPage.value = 0
    void loadHistory()
  }

  async function loadHistorySemesters(academicYearId: number): Promise<void> {
    const accessToken = context.accessToken()
    if (!accessToken) return
    const items = await fetchSemesters(accessToken, academicYearId)
    if (historyAcademicYearId.value !== academicYearId) return
    historySemesters.value = items
    historySemesterId.value = items.find((item) => item.status === 'ACTIVE')?.id ?? items[0]?.id ?? null
  }

  function changeHistoryYear(value: number | null): void {
    historyAcademicYearId.value = value
    historySemesterId.value = null
    historyResponse.value = null
    if (value === null) {
      historySemesters.value = []
      return
    }
    void loadHistorySemesters(value).catch((caughtError: unknown) => {
      if (isApiError(caughtError, 401)) return
      historyError.value = messageFor(caughtError, 'Không thể tải học kỳ cho năm học đã chọn.')
    })
  }

  function changeHistoryPage(page: number, pageSize: number): void {
    historyPage.value = page
    historyPageSize.value = pageSize
    void loadHistory()
  }

  async function loadSummary(): Promise<void> {
    const accessToken = context.accessToken()
    if (context.isStudent.value || !accessToken || summaryClassId.value === null || summarySemesterId.value === null) return
    const rangeError = validateDateRange(summaryFrom.value, summaryTo.value)
    if (rangeError) {
      summaryError.value = rangeError
      summaryResponse.value = null
      return
    }
    if (!summaryFrom.value || !summaryTo.value) {
      summaryError.value = 'Từ ngày và đến ngày là bắt buộc.'
      summaryResponse.value = null
      return
    }
    summaryLoading.value = true
    summaryError.value = ''
    summaryForbidden.value = false
    try {
      summaryResponse.value = await fetchClassAttendanceSummary(accessToken, summaryClassId.value, {
        semesterId: summarySemesterId.value,
        from: summaryFrom.value,
        to: summaryTo.value,
        page: summaryPage.value,
        size: summaryPageSize.value,
      })
    } catch (caughtError) {
      if (isApiError(caughtError, 401)) return
      summaryForbidden.value = isApiError(caughtError, 403)
      summaryError.value = messageFor(caughtError, 'Không thể tải báo cáo chuyên cần của lớp.')
    } finally {
      summaryLoading.value = false
    }
  }

  function changeSummaryPage(page: number, pageSize: number): void {
    summaryPage.value = page
    summaryPageSize.value = pageSize
    void loadSummary()
  }

  async function initializeStudentContext(academicYears: Ref<AcademicYear[]>): Promise<void> {
    const accessToken = context.accessToken()
    if (!accessToken) return
    const years = academicYears.value
    const activeYear = years.find((item) => item.status === 'ACTIVE') ?? years[0]
    historySemesterId.value = null
    historySemesters.value = []
    historyAcademicYearId.value = activeYear?.id ?? null
    if (activeYear) await loadHistorySemesters(activeYear.id)
  }

  return {
    historyAcademicYearId,
    historySemesterId,
    historySemesters,
    historyFrom,
    historyTo,
    historyPage,
    historyPageSize,
    historyResponse,
    historyLoading,
    historyError,
    historyForbidden,
    summaryClassId,
    summarySemesterId,
    summaryFrom,
    summaryTo,
    summaryPage,
    summaryPageSize,
    summaryResponse,
    summaryLoading,
    summaryError,
    summaryForbidden,
    reset,
    applyYearContext,
    loadHistory,
    searchHistory,
    changeHistoryYear,
    changeHistoryPage,
    loadSummary,
    changeSummaryPage,
    initializeStudentContext,
  }
}
