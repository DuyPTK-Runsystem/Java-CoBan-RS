import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import { ApiError } from '@/types/api'
import TranscriptViewerView from './TranscriptViewerView.vue'

const mocks = vi.hoisted(() => ({
  fetchAcademicYears: vi.fn(),
  fetchSemesters: vi.fn(),
  fetchMyTermTranscript: vi.fn(),
  fetchMyAnnualTranscript: vi.fn(),
  fetchMyTermStatus: vi.fn(),
  fetchMyAnnualStatus: vi.fn(),
  fetchStudentAttendanceHistory: vi.fn(),
}))

vi.mock('@/services/academicApi', () => ({
  fetchAcademicYears: mocks.fetchAcademicYears,
  fetchSemesters: mocks.fetchSemesters,
}))

vi.mock('@/services/attendanceApi', () => ({
  fetchStudentAttendanceHistory: mocks.fetchStudentAttendanceHistory,
}))

vi.mock('@/services/transcriptApi', () => ({
  fetchMyTermTranscript: mocks.fetchMyTermTranscript,
  fetchMyAnnualTranscript: mocks.fetchMyAnnualTranscript,
  fetchMyTermStatus: mocks.fetchMyTermStatus,
  fetchMyAnnualStatus: mocks.fetchMyAnnualStatus,
}))

const mockYears = [
  { id: 1, code: '2026-2027', name: 'Năm học 2026–2027', startDate: '2026-09-01', endDate: '2027-05-31', status: 'ACTIVE' as const, notes: null },
]

const mockSemesters = [
  { id: 11, academicYearId: 1, code: 'HK1', name: 'Học kỳ I', displayOrder: 1, startDate: '2026-09-01', endDate: '2026-12-31', automaticLockAt: null, status: 'ACTIVE' as const, lockedAt: null, lockedBy: null, lockReason: null, reopenUntil: null },
]

const mockTermTranscript = {
  studentId: 101,
  academicYearId: 1,
  semesterId: 11,
  calculationStatus: 'FINISH' as const,
  sourceVersion: 1,
  calculatedVersion: 1,
  calculatedAt: '2026-09-03T09:00:00',
  dtbhk: 8.0,
  transferNotes: [],
  subjects: [
    {
      subjectId: 1,
      subjectName: 'Toán học',
      subjectType: 'ACADEMIC' as const,
      dtbmh: 8.5,
      skillScore: null,
      calculatedVersion: 1,
      calculatedAt: '2026-09-03T09:00:00',
      assessmentColumns: [
        { columnId: 1, assessmentType: 'KTTT' as const, columnNo: 1, columnName: 'Miệng', scoreStatus: 'SCORED', scoreValue: 8.0 },
      ],
    },
  ],
}

const mockAnnualTranscript = {
  studentId: 101,
  academicYearId: 1,
  calculationStatus: 'FINISH' as const,
  sourceVersion: 1,
  calculatedVersion: 1,
  calculatedAt: '2026-09-03T09:00:00',
  regularDtbcn: 7.5,
  finalDtbcn: 7.5,
  resultSource: 'REGULAR' as const,
  lastCalculationTaskId: 12,
  transferNotes: [],
  subjects: [
    {
      subjectId: 1,
      subjectName: 'Toán học',
      subjectType: 'ACADEMIC' as const,
      hk1: 8.0,
      hk2: 7.0,
      regularDtbmhCn: 7.5,
      officialDtbmhCn: 7.5,
      calculationSource: 'REGULAR' as const,
      calculatedVersion: 1,
      calculatedAt: '2026-09-03T09:00:00',
      retake: null,
    },
  ],
}

const selectStub = {
  props: ['modelValue', 'options'],
  emits: ['update:modelValue'],
  template: '<select v-bind="$attrs" :value="modelValue" @change="$emit(\'update:modelValue\', Number($event.target.value))"><option v-for="option in options" :key="option.id" :value="option.id">{{ option.code ?? option.name }}</option></select>',
}

const buttonStub = {
  props: ['label'],
  emits: ['click'],
  template: '<button @click="$emit(\'click\')">{{ label }}</button>',
}

function mountView() {
  return mount(TranscriptViewerView, {
    global: {
      stubs: {
        Select: selectStub,
        Button: buttonStub,
      },
    },
  })
}

describe('TranscriptViewerView.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    saveAuthSession({
      accessToken: 'token-abc',
      user: { id: 101, username: 'student1', email: 's1@test.com', roles: ['STUDENT'] },
    })
    mocks.fetchAcademicYears.mockResolvedValue(mockYears)
    mocks.fetchSemesters.mockResolvedValue(mockSemesters)
    mocks.fetchMyTermTranscript.mockResolvedValue(mockTermTranscript)
    mocks.fetchMyAnnualTranscript.mockResolvedValue(mockAnnualTranscript)
    mocks.fetchStudentAttendanceHistory.mockResolvedValue({
      items: [],
      summary: {
        validSessionCount: 20,
        presentCount: 17,
        excusedAbsenceCount: 2,
        unexcusedAbsenceCount: 1,
        lateCount: 0,
        earlyLeaveCount: 0,
      },
      page: 0,
      size: 1,
      totalElements: 20,
      totalPages: 1,
    })
  })

  afterEach(() => {
    clearAuthSession()
  })

  it('initializes academic context and loads term transcript by default', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(mocks.fetchAcademicYears).toHaveBeenCalledWith('token-abc')
    expect(mocks.fetchSemesters).toHaveBeenCalledWith('token-abc', 1)
    expect(mocks.fetchMyTermTranscript).toHaveBeenCalledWith('token-abc', 11)
    expect(mocks.fetchStudentAttendanceHistory).toHaveBeenCalledWith('token-abc', {
      academicYearId: 1,
      semesterId: 11,
      page: 0,
      size: 1,
    })

    expect(wrapper.text()).toContain('Bảng Điểm Học Sinh')
    expect(wrapper.text()).toContain('Toán học')
    expect(wrapper.text()).toContain('8.5')
    expect(wrapper.text()).toContain('FINISH — Chính thức')
    expect(wrapper.text()).toContain('Số buổi vắng có phép:')
    expect(wrapper.text()).toContain('2')
    expect(wrapper.text()).toContain('Số buổi vắng không phép:')
    expect(wrapper.text()).toContain('1')
  })

  it('switches to annual transcript tab and loads annual data', async () => {
    const wrapper = mountView()
    await flushPromises()

    const annualTabBtn = wrapper.findAll('.tab-btn')[1]
    await annualTabBtn.trigger('click')
    await flushPromises()

    expect(mocks.fetchMyAnnualTranscript).toHaveBeenCalledWith('token-abc', 1)
    expect(wrapper.text()).toContain('ĐTB cả năm chính thức:')
    expect(wrapper.text()).toContain('7.5')
  })

  it('displays warning banner when calculationStatus is IN_PROGRESS', async () => {
    mocks.fetchMyTermTranscript.mockResolvedValue({
      ...mockTermTranscript,
      calculationStatus: 'IN_PROGRESS',
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Đang cập nhật:')
    expect(wrapper.text()).toContain('Kiểm tra trạng thái')
  })

  it('handles 403 forbidden state cleanly', async () => {
    mocks.fetchMyTermTranscript.mockRejectedValue(new ApiError(403, 'Forbidden', 'FORBIDDEN'))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Từ chối truy cập (403)')
  })

  it('handles 404 not found state cleanly', async () => {
    mocks.fetchMyTermTranscript.mockRejectedValue(new ApiError(404, 'Not Found', 'NOT_FOUND'))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Không tìm thấy bảng điểm (404)')
  })

  it('loads term transcript gracefully even if attendance API fails', async () => {
    mocks.fetchStudentAttendanceHistory.mockRejectedValue(new Error('Attendance service down'))

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.fetchMyTermTranscript).toHaveBeenCalled()
    expect(wrapper.text()).toContain('Toán học')
    expect(wrapper.text()).toContain('Số buổi vắng có phép:')
    expect(wrapper.text()).toContain('—')
  })

  it('displays version v0 correctly when calculatedVersion is 0', async () => {
    mocks.fetchMyTermTranscript.mockResolvedValue({
      ...mockTermTranscript,
      calculatedVersion: 0,
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Phiên bản:')
    expect(wrapper.text()).toContain('v0')
  })

  it('checks status and reloads transcript when transition from IN_PROGRESS to FINISH', async () => {
    mocks.fetchMyTermTranscript.mockResolvedValue({
      ...mockTermTranscript,
      calculationStatus: 'IN_PROGRESS',
    })
    mocks.fetchMyTermStatus.mockResolvedValue({
      studentId: 101,
      targetType: 'SEMESTER',
      targetId: 11,
      calculationStatus: 'FINISH',
      sourceVersion: 1,
      calculatedVersion: 2,
      calculatedAt: '2026-09-03T11:00:00',
      lastTaskId: 15,
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Đang cập nhật:')
    const syncButton = wrapper.find('.notice.warning button')
    expect(syncButton.exists()).toBe(true)

    // Reset mock for reload
    mocks.fetchMyTermTranscript.mockResolvedValue({
      ...mockTermTranscript,
      calculationStatus: 'FINISH',
      calculatedVersion: 2,
    })

    await syncButton.trigger('click')
    await flushPromises()

    expect(mocks.fetchMyTermStatus).toHaveBeenCalledWith('token-abc', 11)
    expect(wrapper.text()).toContain('Phiên bản:')
    expect(wrapper.text()).toContain('v2')
  })
})

