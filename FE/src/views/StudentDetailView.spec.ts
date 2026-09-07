import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import type { AcademicYear, Semester } from '@/types/academic'
import type { StudentAttendanceHistoryResponse } from '@/types/attendance'
import type { StudentEnrollmentHistory } from '@/types/enrollment'
import type { Student } from '@/types/student'
import type {
  ResCalculationTaskDTO,
  ResStudentAnnualTranscriptDTO,
  ResStudentTermTranscriptDTO,
} from '@/types/transcript'
import StudentDetailView from './StudentDetailView.vue'

const mocks = vi.hoisted(() => ({
  getStudent: vi.fn(),
  fetchAcademicYears: vi.fn(),
  fetchSemesters: vi.fn(),
  fetchStudentEnrollmentHistory: vi.fn(),
  fetchStudentAttendanceHistoryById: vi.fn(),
  fetchStudentTermTranscript: vi.fn(),
  fetchStudentAnnualTranscript: vi.fn(),
  recalculateTranscriptById: vi.fn(),
  push: vi.fn(),
  replace: vi.fn(),
  params: { studentId: '101' },
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mocks.push,
    replace: mocks.replace,
  }),
  useRoute: () => ({
    params: mocks.params,
  }),
}))

vi.mock('@/services/studentApi', () => ({
  getStudent: mocks.getStudent,
}))

vi.mock('@/services/academicApi', () => ({
  fetchAcademicYears: mocks.fetchAcademicYears,
  fetchSemesters: mocks.fetchSemesters,
}))

vi.mock('@/services/enrollmentApi', () => ({
  fetchStudentEnrollmentHistory: mocks.fetchStudentEnrollmentHistory,
}))

vi.mock('@/services/attendanceApi', () => ({
  fetchStudentAttendanceHistoryById: mocks.fetchStudentAttendanceHistoryById,
}))

vi.mock('@/services/transcriptApi', () => ({
  fetchStudentTermTranscript: mocks.fetchStudentTermTranscript,
  fetchStudentAnnualTranscript: mocks.fetchStudentAnnualTranscript,
}))

vi.mock('@/services/calculationTaskApi', () => ({
  recalculateTranscriptById: mocks.recalculateTranscriptById,
}))

const sampleStudent: Student = {
  studentId: 101,
  studentCode: 'STU0000001',
  studentName: 'Nguyễn Văn An',
  dateOfBirth: '2010-05-15',
  address: '123 Đường Láng, Hà Nội',
  status: 'ACTIVE',
  currentClassCode: '6A1',
  account: {
    userId: 10,
    username: 'nguyenvanan0000001',
    role: 'STUDENT',
  },
  averageScore: 8.5,
}

const sampleAcademicYears: AcademicYear[] = [
  {
    id: 1,
    code: '2026-2027',
    name: 'Năm học 2026-2027',
    startDate: '2026-09-01',
    endDate: '2027-05-31',
    status: 'ACTIVE',
    notes: null,
  },
]

const sampleSemesters: Semester[] = [
  {
    id: 11,
    academicYearId: 1,
    code: 'HK1',
    name: 'Học kỳ 1',
    displayOrder: 1,
    startDate: '2026-09-01',
    endDate: '2026-12-31',
    automaticLockAt: null,
    status: 'ACTIVE',
    lockedAt: null,
    lockedBy: null,
    lockReason: null,
    reopenUntil: null,
  },
]

const sampleEnrollments: StudentEnrollmentHistory[] = [
  {
    enrollment: {
      id: 501,
      studentId: 101,
      studentCode: 'STU0000001',
      studentName: 'Nguyễn Văn An',
      academicYearId: 1,
      currentClassId: 101,
      currentClassCode: '6A1',
      status: 'ACTIVE',
      enrolledAt: '2026-09-01',
      completedAt: null,
    },
    transfers: [
      {
        transferId: 901,
        fromClassId: 102,
        toClassId: 101,
        effectiveAt: '2026-10-01',
        reason: 'Chuyển theo nguyện vọng gia đình',
        approvedBy: 1,
      },
    ],
  },
]

const sampleAttendance: StudentAttendanceHistoryResponse = {
  items: [
    {
      attendanceDate: '2026-09-05',
      sessionPeriod: 'MORNING',
      classId: 101,
      className: '6A1',
      status: 'PRESENT',
      attendanceRecordId: 1001,
      exceptionStatus: null,
      note: null,
    },
  ],
  summary: {
    validSessionCount: 20,
    presentCount: 18,
    excusedAbsenceCount: 2,
    unexcusedAbsenceCount: 0,
    lateCount: 1,
    earlyLeaveCount: 0,
  },
  page: 0,
  size: 10,
  totalElements: 1,
  totalPages: 1,
}

const sampleTermTranscript: ResStudentTermTranscriptDTO = {
  studentId: 101,
  academicYearId: 1,
  semesterId: 11,
  calculationStatus: 'FINISH',
  sourceVersion: 1,
  calculatedVersion: 1,
  calculatedAt: '2026-09-04T10:00:00Z',
  dtbhk: 8.5,
  transferNotes: [],
  subjects: [
    {
      subjectId: 1,
      subjectName: 'Toán học',
      subjectType: 'ACADEMIC',
      dtbmh: 8.7,
      skillScore: null,
      calculatedVersion: 1,
      calculatedAt: '2026-09-04T10:00:00Z',
      assessmentColumns: [],
    },
  ],
}

const sampleAnnualTranscript: ResStudentAnnualTranscriptDTO = {
  studentId: 101,
  academicYearId: 1,
  calculationStatus: 'FINISH',
  sourceVersion: 1,
  calculatedVersion: 1,
  calculatedAt: '2026-09-04T10:00:00Z',
  regularDtbcn: 8.2,
  finalDtbcn: 8.2,
  resultSource: 'REGULAR',
  lastCalculationTaskId: 501,
  transferNotes: [],
  subjects: [
    {
      subjectId: 1,
      subjectName: 'Toán học',
      subjectType: 'ACADEMIC',
      hk1: 8.0,
      hk2: 8.4,
      regularDtbmhCn: 8.2,
      officialDtbmhCn: 8.2,
      calculationSource: 'REGULAR',
      calculatedVersion: 1,
      calculatedAt: '2026-09-04T10:00:00Z',
      retake: null,
    },
  ],
}

const buttonStub = {
  props: ['label', 'loading', 'disabled'],
  emits: ['click'],
  template: '<button :disabled="disabled || loading" @click="$emit(\'click\')">{{ label }}<slot /></button>',
}

const tagStub = {
  props: ['value', 'severity'],
  template: '<span class="tag-stub" :data-severity="severity">{{ value }}</span>',
}

const selectStub = {
  props: ['modelValue', 'options', 'optionLabel', 'optionValue'],
  emits: ['update:modelValue'],
  template: `
    <select :value="modelValue" @change="$emit('update:modelValue', Number($event.target.value))">
      <option v-for="opt in options" :key="opt[optionValue ?? 'id']" :value="opt[optionValue ?? 'id']">
        {{ opt[optionLabel ?? 'name'] }}
      </option>
    </select>
  `,
}

const tabsStub = {
  props: ['value'],
  emits: ['update:value'],
  template: '<div class="tabs-stub"><slot /></div>',
}

const tabListStub = { template: '<div class="tab-list-stub"><slot /></div>' }
const tabStub = { props: ['value'], template: '<div class="tab-item-stub" :data-tab="value"><slot /></div>' }
const tabPanelsStub = { template: '<div class="tab-panels-stub"><slot /></div>' }
const tabPanelStub = { props: ['value'], template: '<div class="tab-panel-stub" :data-panel="value"><slot /></div>' }

const dataTableStub = {
  props: ['value', 'loading'],
  template: '<div class="datatable-stub"><slot /></div>',
}

const columnStub = {
  props: ['field', 'header'],
  template: '<div class="column-stub">{{ header }}</div>',
}

const transcriptStatusCardStub = {
  props: ['status', 'studentName'],
  template: '<div class="transcript-status-card-stub">{{ studentName }} - {{ status?.calculationStatus }}</div>',
}

const transcriptTermTableStub = {
  props: ['subjects', 'dtbhk'],
  template: '<div class="transcript-term-table-stub">ĐTB: {{ dtbhk }}</div>',
}

const transcriptAnnualTableStub = {
  props: ['subjects', 'regularDtbcn', 'finalDtbcn'],
  template: '<div class="transcript-annual-table-stub">ĐTB Cả năm: {{ finalDtbcn }}</div>',
}

const serverPaginationStub = {
  props: ['page', 'pageSize', 'totalRecords'],
  emits: ['page-change'],
  template: '<div class="server-pagination-stub" />',
}

const emptyStateStub = {
  props: ['heading', 'message', 'icon', 'title', 'description'],
  template: '<div class="empty-state-stub">{{ heading || title }} - {{ message || description }}</div>',
}

function mountView() {
  return mount(StudentDetailView, {
    global: {
      stubs: {
        Button: buttonStub,
        Tag: tagStub,
        Select: selectStub,
        Tabs: tabsStub,
        TabList: tabListStub,
        Tab: tabStub,
        TabPanels: tabPanelsStub,
        TabPanel: tabPanelStub,
        DataTable: dataTableStub,
        Column: columnStub,
        TranscriptStatusCard: transcriptStatusCardStub,
        TranscriptTermTable: transcriptTermTableStub,
        TranscriptAnnualTable: transcriptAnnualTableStub,
        ServerPagination: serverPaginationStub,
        EmptyState: emptyStateStub,
      },
    },
  })
}

describe('StudentDetailView.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    clearAuthSession()
    saveAuthSession({
      accessToken: 'test-token',
      user: { id: 1, username: 'admin', roles: ['ADMIN'] },
    })
    mocks.params = { studentId: '101' }
    mocks.getStudent.mockResolvedValue(sampleStudent)
    mocks.fetchAcademicYears.mockResolvedValue(sampleAcademicYears)
    mocks.fetchSemesters.mockResolvedValue(sampleSemesters)
    mocks.fetchStudentEnrollmentHistory.mockResolvedValue(sampleEnrollments)
    mocks.fetchStudentAttendanceHistoryById.mockResolvedValue(sampleAttendance)
    mocks.fetchStudentTermTranscript.mockResolvedValue(sampleTermTranscript)
    mocks.fetchStudentAnnualTranscript.mockResolvedValue(sampleAnnualTranscript)
    mocks.recalculateTranscriptById.mockResolvedValue({
      taskId: 5001,
      studentCode: 'STU0000001',
      academicYearId: 1,
      status: 'SUBMITTED',
      requestedAt: '2026-09-05T00:00:00Z',
    } as ResCalculationTaskDTO)
  })

  afterEach(() => {
    clearAuthSession()
  })

  it('TC-F4-01: Tab 1 binds demographics and excludes deprecated averageScore', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(mocks.getStudent).toHaveBeenCalledWith('test-token', 101)
    expect(wrapper.text()).toContain('Nguyễn Văn An')
    expect(wrapper.text()).toContain('STU0000001')
    expect(wrapper.text()).toContain('123 Đường Láng, Hà Nội')

    // Deprecated averageScore check: Tab 1 must NOT render averageScore
    const tab1 = wrapper.find('[data-panel="profile"]')
    expect(tab1.text()).not.toContain('Điểm trung bình')
    expect(tab1.text()).not.toContain('8.5')
  })

  it('TC-F4-01-ACC: Tab 1 binds linked account information when provisioned', async () => {
    const wrapper = mountView()
    await flushPromises()

    const tab1 = wrapper.find('[data-panel="profile"]')
    expect(tab1.text()).toContain('Tài khoản đăng nhập liên kết (V3)')
    expect(tab1.text()).toContain('nguyenvanan0000001')
    expect(tab1.text()).toContain('STUDENT')
  })

  it('Tab 1 displays unprovisioned account notice when student has no account', async () => {
    mocks.getStudent.mockResolvedValueOnce({
      ...sampleStudent,
      account: null,
      userId: null,
      username: null,
    })

    const wrapper = mountView()
    await flushPromises()

    const tab1 = wrapper.find('[data-panel="profile"]')
    expect(tab1.text()).toContain('Chưa cấp tài khoản đăng nhập')
  })

  it('TC-F4-02: Tab 2 binds current class and transfer history', async () => {
    const wrapper = mountView()
    await flushPromises()

    // Switch tab to enrollment
    const tabs = wrapper.findComponent(tabsStub)
    await tabs.vm.$emit('update:value', 'enrollment')
    await flushPromises()

    expect(mocks.fetchStudentEnrollmentHistory).toHaveBeenCalledWith('test-token', 101)
    const tab2 = wrapper.find('[data-panel="enrollment"]')
    expect(tab2.text()).toContain('6A1')
    expect(tab2.text()).toContain('2026-2027')
  })

  it('TC-F4-03: Tab 3 binds attendance summary counters and sessions', async () => {
    const wrapper = mountView()
    await flushPromises()

    // Switch tab to attendance
    const tabs = wrapper.findComponent(tabsStub)
    await tabs.vm.$emit('update:value', 'attendance')
    await flushPromises()

    expect(mocks.fetchStudentAttendanceHistoryById).toHaveBeenCalledWith(
      'test-token',
      101,
      expect.objectContaining({ page: 0, size: 10 }),
    )

    const tab3 = wrapper.find('[data-panel="attendance"]')
    expect(tab3.text()).toContain('20') // validSessionCount
    expect(tab3.text()).toContain('18') // presentCount
    expect(tab3.text()).toContain('90.0%') // attendanceRate
    expect(tab3.text()).toContain('2') // excusedAbsenceCount
  })

  it('TC-F4-04: Tab 4 binds term transcript and calculation status', async () => {
    const wrapper = mountView()
    await flushPromises()

    // Switch tab to transcript
    const tabs = wrapper.findComponent(tabsStub)
    await tabs.vm.$emit('update:value', 'transcript')
    await flushPromises()

    expect(mocks.fetchStudentTermTranscript).toHaveBeenCalledWith('test-token', 101, 11)
    const tab4 = wrapper.find('[data-panel="transcript"]')
    expect(tab4.text()).toContain('ĐTB: 8.5')
  })

  it('TC-F4-05: Recalculate button invokes calculation task for authorized ADMIN role', async () => {
    const wrapper = mountView()
    await flushPromises()

    // Switch tab to transcript
    const tabs = wrapper.findComponent(tabsStub)
    await tabs.vm.$emit('update:value', 'transcript')
    await flushPromises()

    const recalcBtn = wrapper.findAll('button').find((b) => b.text().includes('Yêu cầu tính lại điểm'))
    expect(recalcBtn).toBeDefined()
    await recalcBtn!.trigger('click')
    await flushPromises()

    expect(mocks.recalculateTranscriptById).toHaveBeenCalledWith('test-token', 101, 1)
    expect(wrapper.text()).toContain('Mã tác vụ: #5001')
  })

  it('Hides Recalculate button for unauthorized TEACHER role', async () => {
    clearAuthSession()
    saveAuthSession({
      accessToken: 'teacher-token',
      user: { id: 3, username: 'teacher', roles: ['TEACHER'] },
    })

    const wrapper = mountView()
    await flushPromises()

    const tabs = wrapper.findComponent(tabsStub)
    await tabs.vm.$emit('update:value', 'transcript')
    await flushPromises()

    const recalcBtn = wrapper.findAll('button').find((b) => b.text().includes('Yêu cầu tính lại điểm'))
    expect(recalcBtn).toBeUndefined()
  })

  it('Header action buttons navigate to list and edit views', async () => {
    const wrapper = mountView()
    await flushPromises()

    const backBtn = wrapper.findAll('button').find((b) => b.text().includes('Quay lại danh sách'))
    await backBtn!.trigger('click')
    expect(mocks.push).toHaveBeenCalledWith('/v2/students')

    const editBtn = wrapper.findAll('button').find((b) => b.text().includes('Chỉnh sửa'))
    await editBtn!.trigger('click')
    expect(mocks.push).toHaveBeenCalledWith('/v2/students/101/edit')
  })
})
