import { computed, ref, type ComputedRef, type Ref } from 'vue'
import { useConfirm } from 'primevue/useconfirm'

import {
  createOrGetAttendanceSession,
  deleteAttendanceException,
  fetchAttendanceSession,
  fetchAttendanceSessionStudents,
  upsertAttendanceException,
} from '@/services/attendanceApi'
import { isApiError } from '@/types/api'
import type {
  AttendanceApiScope,
  AttendanceSession,
  AttendanceSessionPeriod,
  AttendanceStudent,
  UpsertAttendanceExceptionRequest,
} from '@/types/attendance'
import type { LoadingState } from '@/types/ui'

type AccessToken = () => string | null

interface AttendanceSessionContext {
  accessToken: AccessToken
  scope: ComputedRef<AttendanceApiScope>
  isStudent: ComputedRef<boolean>
  canOpenSession: ComputedRef<boolean>
  classId: Ref<number | null>
  semesterId: Ref<number | null>
  attendanceDate: Ref<string>
  sessionPeriod: Ref<AttendanceSessionPeriod>
  readOnly: ComputedRef<boolean>
  contextKey: () => string
}

function messageFor(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

export function useAttendanceSession(context: AttendanceSessionContext) {
  const confirm = useConfirm()
  const session = ref<AttendanceSession | null>(null)
  const students = ref<AttendanceStudent[]>([])
  const loading = ref(false)
  const error = ref('')
  const forbidden = ref(false)
  const opening = ref(false)
  const saving = ref(false)
  const exceptionVisible = ref(false)
  const selectedStudent = ref<AttendanceStudent | null>(null)
  const mutationError = ref('')
  const statusMessage = ref('')
  let requestKey = ''

  const state = computed<LoadingState>(() => {
    if (loading.value) return 'loading'
    if (error.value) return 'error'
    if (session.value && students.value.length === 0) return 'empty'
    return 'success'
  })

  function reset(): void {
    session.value = null
    students.value = []
    error.value = ''
    forbidden.value = false
    selectedStudent.value = null
    exceptionVisible.value = false
  }

  async function loadStudents(sessionId: number): Promise<void> {
    const accessToken = context.accessToken()
    if (!accessToken) return
    loading.value = true
    error.value = ''
    forbidden.value = false
    try {
      students.value = await fetchAttendanceSessionStudents(accessToken, sessionId, context.scope.value)
    } catch (caughtError) {
      if (isApiError(caughtError, 401)) return
      forbidden.value = isApiError(caughtError, 403)
      error.value = messageFor(caughtError, 'Không thể tải danh sách học sinh của buổi điểm danh.')
    } finally {
      loading.value = false
    }
  }

  async function load(): Promise<void> {
    if (context.isStudent.value || context.classId.value === null || context.semesterId.value === null || !context.attendanceDate.value) return
    const key = context.contextKey()
    requestKey = key
    reset()
    const accessToken = context.accessToken()
    if (!accessToken) return
    loading.value = true
    error.value = ''
    forbidden.value = false
    try {
      const loadedSession = await fetchAttendanceSession(accessToken, {
        classId: context.classId.value,
        semesterId: context.semesterId.value,
        attendanceDate: context.attendanceDate.value,
        sessionPeriod: context.sessionPeriod.value,
      }, context.scope.value)
      if (requestKey !== key) return
      session.value = loadedSession
      await loadStudents(loadedSession.sessionId)
    } catch (caughtError) {
      if (isApiError(caughtError, 401) || isApiError(caughtError, 404) || requestKey !== key) return
      forbidden.value = isApiError(caughtError, 403)
      error.value = messageFor(caughtError, 'Không thể tải buổi điểm danh.')
    } finally {
      if (requestKey === key) loading.value = false
    }
  }

  function openException(student: AttendanceStudent): void {
    if (context.readOnly.value) return
    selectedStudent.value = student
    mutationError.value = ''
    exceptionVisible.value = true
  }

  function closeException(): void {
    exceptionVisible.value = false
    selectedStudent.value = null
    mutationError.value = ''
  }

  async function open(): Promise<void> {
    if (!context.canOpenSession.value || context.classId.value === null || context.semesterId.value === null || !context.attendanceDate.value || context.readOnly.value) return
    const accessToken = context.accessToken()
    if (!accessToken) return
    opening.value = true
    error.value = ''
    try {
      const loadedSession = await createOrGetAttendanceSession(accessToken, {
        classId: context.classId.value,
        semesterId: context.semesterId.value,
        attendanceDate: context.attendanceDate.value,
        sessionPeriod: context.sessionPeriod.value,
      }, context.scope.value)
      session.value = loadedSession
      await loadStudents(loadedSession.sessionId)
    } catch (caughtError) {
      if (isApiError(caughtError, 401)) return
      forbidden.value = isApiError(caughtError, 403)
      error.value = messageFor(caughtError, 'Không thể mở buổi điểm danh.')
    } finally {
      opening.value = false
    }
  }

  async function saveException(request: UpsertAttendanceExceptionRequest): Promise<void> {
    const accessToken = context.accessToken()
    if (!accessToken || !session.value || !selectedStudent.value || context.readOnly.value) return
    saving.value = true
    mutationError.value = ''
    try {
      await upsertAttendanceException(accessToken, session.value.sessionId, selectedStudent.value.studentId, request, context.scope.value)
      await loadStudents(session.value.sessionId)
      statusMessage.value = `Đã cập nhật điểm danh cho ${selectedStudent.value.studentCode}.`
      closeException()
    } catch (caughtError) {
      if (isApiError(caughtError, 401)) return
      mutationError.value = messageFor(caughtError, 'Không thể lưu điểm danh.')
    } finally {
      saving.value = false
    }
  }

  async function removeException(student: AttendanceStudent): Promise<void> {
    const accessToken = context.accessToken()
    if (!accessToken || !session.value) return
    saving.value = true
    mutationError.value = ''
    try {
      await deleteAttendanceException(accessToken, session.value.sessionId, student.studentId, context.scope.value)
      await loadStudents(session.value.sessionId)
      statusMessage.value = `Đã xóa ghi nhận của ${student.studentCode}; trạng thái trở về có mặt.`
    } catch (caughtError) {
      if (isApiError(caughtError, 401)) return
      mutationError.value = messageFor(caughtError, 'Không thể xóa ghi nhận điểm danh.')
    } finally {
      saving.value = false
    }
  }

  function confirmDelete(student: AttendanceStudent): void {
    if (!session.value || context.readOnly.value) return
    confirm.require({
      header: 'Xóa ghi nhận điểm danh',
      message: `Xóa trạng thái điểm danh của ${student.studentCode} và trả về trạng thái có mặt mặc định?`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Xóa ghi nhận',
      rejectLabel: 'Hủy',
      accept: () => { void removeException(student) },
    })
  }

  return {
    session,
    students,
    loading,
    error,
    forbidden,
    opening,
    saving,
    exceptionVisible,
    selectedStudent,
    mutationError,
    statusMessage,
    state,
    reset,
    load,
    open,
    openException,
    closeException,
    saveException,
    confirmDelete,
  }
}
