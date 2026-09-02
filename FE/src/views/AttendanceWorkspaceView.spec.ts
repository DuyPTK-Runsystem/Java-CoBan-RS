import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import type { AttendanceExceptionStatus } from '@/types/attendance'
import router from '@/router'
import AttendanceWorkspaceView from './AttendanceWorkspaceView.vue'

const mocks = vi.hoisted(() => ({
  fetchAcademicYears: vi.fn(),
  fetchSchoolClasses: vi.fn(),
  fetchSemesters: vi.fn(),
  fetchAttendanceCalendar: vi.fn(),
  createOrGetAttendanceSession: vi.fn(),
  fetchAttendanceSessionStudents: vi.fn(),
  upsertAttendanceException: vi.fn(),
  deleteAttendanceException: vi.fn(),
  fetchStudentAttendanceHistory: vi.fn(),
  fetchClassAttendanceSummary: vi.fn(),
  confirmRequire: vi.fn(),
}))

vi.mock('primevue/useconfirm', () => ({ useConfirm: () => ({ require: mocks.confirmRequire }) }))

vi.mock('@/services/academicApi', () => ({
  fetchAcademicYears: mocks.fetchAcademicYears,
  fetchSchoolClasses: mocks.fetchSchoolClasses,
  fetchSemesters: mocks.fetchSemesters,
}))

vi.mock('@/services/attendanceApi', () => ({
  fetchAttendanceCalendar: mocks.fetchAttendanceCalendar,
  createOrGetAttendanceSession: mocks.createOrGetAttendanceSession,
  fetchAttendanceSessionStudents: mocks.fetchAttendanceSessionStudents,
  upsertAttendanceException: mocks.upsertAttendanceException,
  deleteAttendanceException: mocks.deleteAttendanceException,
  fetchStudentAttendanceHistory: mocks.fetchStudentAttendanceHistory,
  fetchClassAttendanceSummary: mocks.fetchClassAttendanceSummary,
}))

const academicYears = [{ id: 1, code: '2026-2027', startDate: '2026-09-01', endDate: '2027-05-31', status: 'ACTIVE' as const, notes: null }]
const semesters = [{ id: 2, academicYearId: 1, code: 'HK1', name: 'Học kỳ 1', displayOrder: 1, startDate: '2026-09-01', endDate: '2026-12-31', automaticLockAt: null, status: 'ACTIVE' as const, lockedAt: null, lockedBy: null, lockReason: null, reopenUntil: null }]
const classes = [{ id: 3, academicYearId: 1, gradeLevelId: 6, classCode: '6A1', className: 'Lớp 6A1', capacity: 35, status: 'ACTIVE' as const }]
const attendanceSession = { sessionId: 5012, classId: 3, semesterId: 2, attendanceDate: '2026-09-01', sessionPeriod: 'MORNING' as const, createdBy: 5, createdAt: '2026-09-01T07:00:00' }
const attendanceStudents = [{ studentId: 11, studentCode: 'HS001', studentName: 'Nguyễn Minh An', attendanceRecordId: null, status: 'PRESENT', note: null, recordedBy: null, recordedAt: null, updatedBy: null, updatedAt: null }]

const buttonStub = { props: ['label', 'disabled', 'loading'], template: '<button :disabled="disabled" @click="$emit(\'click\')">{{ label }}</button>' }
const contextStub = { emits: ['open'], template: '<button data-testid="open-session" @click="$emit(\'open\')">Open session</button>' }
const sessionTableStub = { props: ['students'], emits: ['exception', 'delete'], template: '<div data-testid="session-table">{{ students.length }}</div>' }
const exceptionDialogStub = { props: ['visible'], emits: ['save', 'cancel'], template: '<div data-testid="exception-dialog" :data-visible="visible" />' }
const historyPanelStub = { emits: ['search', 'pageChange'], template: '<div data-testid="history-panel" />' }
const summaryPanelStub = { emits: ['search', 'pageChange'], template: '<div data-testid="summary-panel" />' }
const formAlertStub = { props: ['message'], template: '<div data-testid="form-alert">{{ message }}</div>' }
const confirmDialogStub = { template: '<div data-testid="confirm-dialog" />' }

function mountView() {
  return mount(AttendanceWorkspaceView, {
    global: {
      plugins: [router],
      stubs: {
        AttendanceContextPanel: contextStub,
        AttendanceExceptionDialog: exceptionDialogStub,
        AttendanceHistoryPanel: historyPanelStub,
        AttendanceSessionTable: sessionTableStub,
        ClassAttendanceSummaryPanel: summaryPanelStub,
        Button: buttonStub,
        ConfirmDialog: confirmDialogStub,
        FormAlert: formAlertStub,
      },
    },
  })
}

describe('AttendanceWorkspaceView', () => {
  beforeEach(async () => {
    clearAuthSession()
    saveAuthSession({ accessToken: 'jwt-token', user: { id: 5, username: 'teacher.demo', roles: ['TEACHER'] } })
    mocks.fetchAcademicYears.mockReset().mockResolvedValue(academicYears)
    mocks.fetchSchoolClasses.mockReset().mockResolvedValue(classes)
    mocks.fetchSemesters.mockReset().mockResolvedValue(semesters)
    mocks.fetchAttendanceCalendar.mockReset().mockResolvedValue([{ id: 9, academicYearId: 1, semesterId: 2, calendarDate: '2026-09-01', dayType: 'SCHOOL_DAY', reason: null, configuredBy: 5, configuredAt: null, updatedBy: null, updatedAt: null, sessions: [{ id: 10, sessionPeriod: 'MORNING', sessionStatus: 'SCHEDULED', reason: null, configuredBy: 5, configuredAt: null, updatedBy: null, updatedAt: null }] }])
    mocks.createOrGetAttendanceSession.mockReset().mockResolvedValue(attendanceSession)
    mocks.fetchAttendanceSessionStudents.mockReset().mockResolvedValue(attendanceStudents)
    mocks.upsertAttendanceException.mockReset().mockResolvedValue({ attendanceRecordId: 88, sessionId: 5012, studentId: 11, studentCode: 'HS001', studentName: 'Nguyễn Minh An', status: 'LATE', note: '08:05', recordedBy: 5, recordedAt: '2026-09-01T08:05:00', updatedBy: null, updatedAt: null })
    mocks.deleteAttendanceException.mockReset().mockResolvedValue(undefined)
    mocks.fetchStudentAttendanceHistory.mockReset().mockResolvedValue({ items: [], summary: { validSessionCount: 0, presentCount: 0, excusedAbsenceCount: 0, unexcusedAbsenceCount: 0, lateCount: 0, earlyLeaveCount: 0 }, page: 0, size: 10, totalElements: 0, totalPages: 0 })
    mocks.fetchClassAttendanceSummary.mockReset().mockResolvedValue({ class: { id: 3, name: '6A1', gradeLevelId: 6 }, semesterId: 2, from: '2026-09-01', to: '2026-09-30', validSessionCount: 0, summary: { presentCount: 0, excusedAbsenceCount: 0, unexcusedAbsenceCount: 0, lateCount: 0, earlyLeaveCount: 0 }, students: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
    await router.push({ name: 'v2-attendance' })
  })

  afterEach(() => clearAuthSession())

  it('loads academic context and calendar preflight before a session is opened', async () => {
    mountView()
    await flushPromises()

    expect(mocks.fetchAcademicYears).toHaveBeenCalledWith('jwt-token')
    expect(mocks.fetchSchoolClasses).toHaveBeenCalledWith('jwt-token', 1)
    expect(mocks.fetchSemesters).toHaveBeenCalledWith('jwt-token', 1)
    expect(mocks.fetchAttendanceCalendar).toHaveBeenCalledWith('jwt-token', { academicYearId: 1, semesterId: 2, from: '2026-09-01', to: '2026-09-01' })
    expect(mocks.fetchAttendanceSessionStudents).not.toHaveBeenCalled()
  })

  it('opens a session and loads its students only after the context action', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="open-session"]').trigger('click')
    await flushPromises()

    expect(mocks.createOrGetAttendanceSession).toHaveBeenCalledWith('jwt-token', { classId: 3, semesterId: 2, attendanceDate: '2026-09-01', sessionPeriod: 'MORNING' }, 'teacher')
    expect(mocks.fetchAttendanceSessionStudents).toHaveBeenCalledWith('jwt-token', 5012, 'teacher')
    expect(wrapper.find('[data-testid="session-table"]').text()).toContain('1')
  })

  it('uses the office attendance API for an academic office session', async () => {
    clearAuthSession()
    saveAuthSession({ accessToken: 'office-token', user: { id: 2, username: 'office.demo', roles: ['ACADEMIC_OFFICE'] } })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="open-session"]').trigger('click')
    await flushPromises()

    expect(mocks.createOrGetAttendanceSession).toHaveBeenCalledWith('office-token', { classId: 3, semesterId: 2, attendanceDate: '2026-09-01', sessionPeriod: 'MORNING' }, 'office')
    expect(mocks.fetchAttendanceSessionStudents).toHaveBeenCalledWith('office-token', 5012, 'office')
  })

  it('saves an exception and reloads the session rows', async () => {
    const wrapper = mountView()
    await flushPromises()
    await (wrapper.vm as unknown as { openSession: () => Promise<void> }).openSession()
    await flushPromises()
    const view = wrapper.vm as unknown as { openException: (student: typeof attendanceStudents[number]) => void; saveException: (request: { status: AttendanceExceptionStatus; note: string | null }) => Promise<void> }
    view.openException(attendanceStudents[0])
    await view.saveException({ status: 'LATE', note: '08:05' })
    await flushPromises()

    expect(mocks.upsertAttendanceException).toHaveBeenCalledWith('jwt-token', 5012, 11, { status: 'LATE', note: '08:05' }, 'teacher')
    expect(mocks.fetchAttendanceSessionStudents.mock.calls.length).toBeGreaterThan(1)
  })

  it('does not call report APIs for an invalid date range', async () => {
    const wrapper = mountView()
    await flushPromises()
    const view = wrapper.vm as unknown as { historyFrom: string; historyTo: string; searchHistory: () => void }
    view.historyFrom = '2026-10-01'
    view.historyTo = '2026-09-01'
    view.searchHistory()
    await flushPromises()

    expect(mocks.fetchStudentAttendanceHistory).not.toHaveBeenCalled()
  })
})
