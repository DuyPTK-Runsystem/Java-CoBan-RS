import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import ClassTranscriptViewerView from './ClassTranscriptViewerView.vue'

const mocks = vi.hoisted(() => ({
  fetchAcademicYears: vi.fn(),
  fetchSemesters: vi.fn(),
  fetchAccessibleClasses: vi.fn(),
  fetchClassTermTranscript: vi.fn(),
  fetchClassAnnualTranscript: vi.fn(),
  push: vi.fn(),
}))

vi.mock('@/services/academicApi', () => ({
  fetchAcademicYears: mocks.fetchAcademicYears,
  fetchSemesters: mocks.fetchSemesters,
}))

vi.mock('@/services/classTranscriptApi', () => ({
  fetchAccessibleClasses: mocks.fetchAccessibleClasses,
  fetchClassTermTranscript: mocks.fetchClassTermTranscript,
  fetchClassAnnualTranscript: mocks.fetchClassAnnualTranscript,
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mocks.push }),
  useRoute: () => ({ query: {} }),
}))

const mockYears = [
  { id: 1, code: '2026-2027', name: 'Năm học 2026–2027', startDate: '2026-09-01', endDate: '2027-05-31', status: 'ACTIVE' as const, notes: null },
]

const mockSemesters = [
  { id: 11, academicYearId: 1, code: 'HK1', name: 'Học kỳ I', displayOrder: 1, startDate: '2026-09-01', endDate: '2026-12-31', automaticLockAt: null, status: 'ACTIVE' as const, lockedAt: null, lockedBy: null, lockReason: null, reopenUntil: null },
]

const mockClasses = [
  { id: 10, academicYearId: 1, gradeLevelId: 1, classCode: '10A1', className: 'Lớp 10A1', capacity: 40, status: 'ACTIVE' as const },
]

const mockClassTermData = {
  classId: 10,
  classCode: '10A1',
  className: 'Lớp 10A1',
  academicYearId: 1,
  semesterId: 11,
  students: [
    {
      studentId: 101,
      studentCode: 'HS001',
      fullName: 'Nguyễn Văn A',
      calculationStatus: 'FINISH' as const,
      dtbhk: 8.0,
      subjects: [
        {
          subjectId: 1,
          subjectName: 'Toán học',
          subjectType: 'ACADEMIC' as const,
          dtbmh: 8.5,
          skillScore: null,
          calculatedVersion: 1,
          calculatedAt: '2026-09-04T08:00:00',
          assessmentColumns: [
            { columnId: 1, assessmentType: 'KTTX' as const, columnNo: 1, columnName: 'TX1', scoreStatus: 'SCORED', scoreValue: 8.5 },
          ],
        },
      ],
    },
  ],
}

const buttonStub = {
  props: ['label'],
  emits: ['click'],
  template: '<button v-bind="$attrs" @click="$emit(\'click\')">{{ label }}<slot /></button>',
}

describe('ClassTranscriptViewerView.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    saveAuthSession({
      accessToken: 'test-token',
      user: {
        id: 1,
        username: 'gvcn_an',
        roles: ['TEACHER'],
      },
    })
    mocks.fetchAcademicYears.mockResolvedValue(mockYears)
    mocks.fetchSemesters.mockResolvedValue(mockSemesters)
    mocks.fetchAccessibleClasses.mockResolvedValue(mockClasses)
    mocks.fetchClassTermTranscript.mockResolvedValue(mockClassTermData)
    mocks.fetchClassAnnualTranscript.mockResolvedValue({ ...mockClassTermData, students: [] })
  })

  afterEach(() => {
    clearAuthSession()
  })

  it('mounts and loads context and initial class term transcript', async () => {
    const wrapper = mount(ClassTranscriptViewerView, {
      global: {
        stubs: {
          Select: true,
          Button: buttonStub,
        },
      },
    })

    await flushPromises()

    expect(mocks.fetchAcademicYears).toHaveBeenCalled()
    expect(mocks.fetchSemesters).toHaveBeenCalledWith('test-token', 1)
    expect(mocks.fetchAccessibleClasses).toHaveBeenCalledWith('test-token', 1)
    expect(mocks.fetchClassTermTranscript).toHaveBeenCalledWith('test-token', 10, 11)

    expect(wrapper.text()).toContain('Bảng điểm lớp học')
    expect(wrapper.text()).toContain('Nguyễn Văn A')
  })

  it('switches scope from SUBJECT to SUMMARY', async () => {
    const wrapper = mount(ClassTranscriptViewerView, {
      global: {
        stubs: {
          Select: true,
          Button: buttonStub,
        },
      },
    })

    await flushPromises()

    const summaryScopeBtn = wrapper.findAll('.scope-btn')[1]
    await summaryScopeBtn.trigger('click')

    expect(wrapper.text()).toContain('Bảng điểm tổng kết học kỳ')
  })

  it('handles selecting student and navigates to individual transcript', async () => {
    const wrapper = mount(ClassTranscriptViewerView, {
      global: {
        stubs: {
          Select: true,
          Button: buttonStub,
        },
      },
    })

    await flushPromises()

    const studentBtn = wrapper.find('.student-link-btn')
    await studentBtn.trigger('click')

    expect(mocks.push).toHaveBeenCalledWith({
      path: '/v2/transcripts',
      query: {
        studentId: '101',
        studentName: 'Nguyễn Văn A',
        studentCode: 'HS001',
        classId: '10',
        academicYearId: '1',
        semesterId: '11',
        from: 'class-transcripts',
      },
    })
  })

  it('renders mini tabs and period tabs without icons or parentheses', async () => {
    const wrapper = mount(ClassTranscriptViewerView, {
      global: {
        stubs: {
          Select: true,
          Button: buttonStub,
        },
      },
    })

    await flushPromises()

    const scopeBtns = wrapper.findAll('.scope-btn')
    expect(scopeBtns).toHaveLength(2)
    expect(scopeBtns[0].text()).toBe('BẢNG ĐIỂM THEO MÔN')
    expect(scopeBtns[1].text()).toBe('BẢNG ĐIỂM TỔNG KẾT')

    const tabBtns = wrapper.findAll('.tab-btn')
    expect(tabBtns).toHaveLength(2)
    expect(tabBtns[0].text()).toBe('Bảng điểm Học kỳ')
    expect(tabBtns[1].text()).toBe('Bảng điểm Cả năm')

    expect(wrapper.text()).not.toContain('📖')
    expect(wrapper.text()).not.toContain('📊')
    expect(wrapper.text()).not.toContain('📌')
    expect(wrapper.text()).not.toContain('🏆')
    expect(wrapper.text()).not.toContain('Mỗi bảng là một môn')
    expect(wrapper.text()).not.toContain('Tổng hợp toàn bộ các môn')
  })
})

