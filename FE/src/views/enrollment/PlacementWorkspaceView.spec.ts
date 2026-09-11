import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import router from '@/router'
import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import { ApiError } from '@/types/api'
import PlacementWorkspaceView from './PlacementWorkspaceView.vue'

const mocks = vi.hoisted(() => ({
  fetchAcademicYears: vi.fn(),
  fetchGrades: vi.fn(),
  fetchSchoolClasses: vi.fn(),
  fetchUnassignedStudents: vi.fn(),
  createPlacementSession: vi.fn(),
  getPlacementSession: vi.fn(),
  updatePlacementSession: vi.fn(),
  simulatePlacementSession: vi.fn(),
  fetchPlacementResults: vi.fn(),
  confirmPlacementSession: vi.fn(),
  cancelPlacementSession: vi.fn(),
  getStudent: vi.fn(),
}))

vi.mock('@/services/academicApi', () => ({
  fetchAcademicYears: mocks.fetchAcademicYears,
  fetchGrades: mocks.fetchGrades,
  fetchSchoolClasses: mocks.fetchSchoolClasses,
}))

vi.mock('@/services/enrollmentApi', () => ({
  fetchUnassignedStudents: mocks.fetchUnassignedStudents,
}))

vi.mock('@/services/placementApi', () => ({
  createPlacementSession: mocks.createPlacementSession,
  getPlacementSession: mocks.getPlacementSession,
  updatePlacementSession: mocks.updatePlacementSession,
  simulatePlacementSession: mocks.simulatePlacementSession,
  fetchPlacementResults: mocks.fetchPlacementResults,
  confirmPlacementSession: mocks.confirmPlacementSession,
  cancelPlacementSession: mocks.cancelPlacementSession,
}))

vi.mock('@/services/studentApi', () => ({
  getStudent: mocks.getStudent,
}))

const academicYears = [
  { id: 1, code: '2026-2027', startDate: '2026-09-01', endDate: '2027-05-31', status: 'ACTIVE' as const, notes: null },
]
const grades = [
  { id: 8, code: 'GRADE_8', name: 'Khối 8', gradeLevel: 8 as const, displayOrder: 1, nextGradeId: 9, active: true, description: null },
]
const classes = [
  { id: 81, academicYearId: 1, gradeLevelId: 8, classCode: '8A1', className: 'Lớp 8A1', capacity: 35, status: 'ACTIVE' as const },
]
const unassigned = [
  { studentId: 101, studentCode: 'HS101', studentName: 'Nguyễn Văn A' },
]

const sampleSession = {
  id: 74,
  academicYearId: 1,
  targetGradeId: 8,
  status: 'READY_FOR_CONFIRM' as const,
  ruleVersion: '074-v1',
  version: 2,
  targetClassIds: [81],
  targetClasses: [
    { classId: 81, classCode: '8A1', className: 'Lớp 8A1', profile: 'REGULAR' as const, capacity: 35, genderTargetMale: 17, genderTargetFemale: 18 },
  ],
  results: [],
}

const sampleResultsPage = {
  meta: { page: 0, pageSize: 20, totalPages: 1, totalItems: 1 },
  result: [
    {
      id: 1,
      studentId: 101,
      targetClassId: 81,
      resultStatus: 'AUTO_ASSIGNED' as const,
      score: 8.5,
      issueCode: null,
      issueSeverity: null,
      explanation: 'Phân bổ tự động theo cách cân bằng học lực và nam nữ.',
    },
  ],
}

function mountView() {
  return mount(PlacementWorkspaceView, {
    global: {
      plugins: [router],
      stubs: {
        Button: {
          props: ['label', 'disabled', 'loading'],
          emits: ['click'],
          template: '<button :disabled="disabled || loading" @click="$emit(\'click\')">{{ label }}</button>',
        },
        Dialog: {
          props: ['visible', 'header'],
          emits: ['update:visible'],
          template: '<div v-if="visible" data-testid="dialog" :data-header="header"><slot /><slot name="footer" /></div>',
        },
        Select: true,
        Checkbox: true,
        DataTable: {
          props: ['value'],
          template: '<div data-testid="data-table" />',
        },
        Column: true,
        Paginator: {
          props: ['first', 'rows', 'totalRecords'],
          emits: ['page'],
          template: '<div data-testid="paginator"><button class="trigger-page-btn" @click="$emit(\'page\', { page: 1, rows: 20 })">Page 2</button></div>',
        },
        Tag: { props: ['value'], template: '<span>{{ value }}</span>' },
        InputText: true,
      },
    },
  })
}

describe('PlacementWorkspaceView', () => {
  const forbiddenError = () => new ApiError(403, 'Forbidden', { kind: 'forbidden' })

  beforeEach(async () => {
    clearAuthSession()
    saveAuthSession({ accessToken: 'jwt-token', user: { id: 1, username: 'admin01', roles: ['ADMIN'] } })

    mocks.fetchAcademicYears.mockReset().mockResolvedValue(academicYears)
    mocks.fetchGrades.mockReset().mockResolvedValue(grades)
    mocks.fetchSchoolClasses.mockReset().mockResolvedValue(classes)
    mocks.fetchUnassignedStudents.mockReset().mockResolvedValue(unassigned)
    mocks.createPlacementSession.mockReset().mockResolvedValue(sampleSession)
    mocks.getPlacementSession.mockReset().mockResolvedValue(sampleSession)
    mocks.updatePlacementSession.mockReset().mockResolvedValue({ ...sampleSession, version: 3 })
    mocks.simulatePlacementSession.mockReset().mockResolvedValue(sampleSession)
    mocks.fetchPlacementResults.mockReset().mockResolvedValue(sampleResultsPage)
    mocks.confirmPlacementSession.mockReset().mockResolvedValue({ ...sampleSession, status: 'CONFIRMED', version: 3 })
    mocks.cancelPlacementSession.mockReset().mockResolvedValue({ ...sampleSession, status: 'CANCELLED', version: 3 })
    mocks.getStudent.mockReset().mockResolvedValue({ studentId: 101, studentCode: 'HS101', fullName: 'Nguyễn Văn A' })
  })

  afterEach(() => {
    clearAuthSession()
  })

  it('renders setup form on /v2/enrollments/placement/new and creates session', async () => {
    await router.push({ name: 'v2-placement-new' })
    const replaceSpy = vi.spyOn(router, 'replace')

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Tạo phiên xếp lớp tự động')
    expect(mocks.fetchAcademicYears).toHaveBeenCalledWith('jwt-token')

    // Find submit button in setup form
    const createBtn = wrapper.findAll('button').find((b) => b.text().includes('Tạo phiên nháp'))
    expect(createBtn).toBeDefined()
    await createBtn?.trigger('click')
    await flushPromises()

    expect(mocks.createPlacementSession).toHaveBeenCalledWith('jwt-token', expect.objectContaining({
      academicYearId: 1,
      targetGradeId: 8,
      ruleVersion: '074-v1',
    }))
    expect(replaceSpy).toHaveBeenCalledWith({
      name: 'v2-placement-session',
      params: { placementSessionId: '74' },
    })
  })

  it('shows a visible access-denied state when creating a session returns 403', async () => {
    await router.push({ name: 'v2-placement-new' })
    mocks.createPlacementSession.mockRejectedValueOnce(forbiddenError())

    const wrapper = mountView()
    await flushPromises()
    const createBtn = wrapper.findAll('button').find((b) => b.text().includes('Tạo phiên nháp'))
    await createBtn?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Bạn không có quyền tạo phiên xếp lớp này')
    expect(wrapper.text()).toContain('Phiên đăng nhập vẫn được giữ')
  })

  it('renders review mode on /v2/enrollments/placement/:placementSessionId and hydrates students', async () => {
    await router.push({ name: 'v2-placement-session', params: { placementSessionId: '74' } })
    mountView()
    await flushPromises()

    expect(mocks.getPlacementSession).toHaveBeenCalledWith('jwt-token', 74)
    expect(mocks.fetchPlacementResults).toHaveBeenCalledWith('jwt-token', 74, 0, 20)
    expect(mocks.getStudent).toHaveBeenCalledWith('jwt-token', 101)
  })

  it('executes simulation with current session version and reloads results', async () => {
    await router.push({ name: 'v2-placement-session', params: { placementSessionId: '74' } })
    const wrapper = mountView()
    await flushPromises()

    const simulateBtn = wrapper.findAll('button').find((b) => b.text().includes('Mô phỏng lại'))
    expect(simulateBtn).toBeDefined()
    await simulateBtn?.trigger('click')
    await flushPromises()

    expect(mocks.simulatePlacementSession).toHaveBeenCalledWith('jwt-token', 74, 2)
    expect(mocks.fetchPlacementResults).toHaveBeenCalledTimes(2)
  })

  it('shows a visible access-denied state when simulation returns 403', async () => {
    await router.push({ name: 'v2-placement-session', params: { placementSessionId: '74' } })
    mocks.simulatePlacementSession.mockRejectedValueOnce(forbiddenError())
    const wrapper = mountView()
    await flushPromises()

    const simulateBtn = wrapper.findAll('button').find((b) => b.text().includes('Mô phỏng lại'))
    await simulateBtn?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Bạn không có quyền mô phỏng phiên xếp lớp này')
  })

  it('handles confirm action with idempotency key and dialog confirmation', async () => {
    await router.push({ name: 'v2-placement-session', params: { placementSessionId: '74' } })
    const wrapper = mountView()
    await flushPromises()

    const confirmReviewBtn = wrapper.findAll('button').find((b) => b.text().includes('Xác nhận phần tự động'))
    expect(confirmReviewBtn).toBeDefined()
    await confirmReviewBtn?.trigger('click')
    await flushPromises()

    // Confirm dialog is opened
    const confirmDialog = wrapper.find('[data-testid="dialog"][data-header="Xác nhận xếp lớp tự động"]')
    expect(confirmDialog.exists()).toBe(true)

    // Click confirm in dialog
    const executeConfirmBtn = confirmDialog.findAll('button').find((b) => b.text() === 'Xác nhận')
    await executeConfirmBtn?.trigger('click')
    await flushPromises()

    expect(mocks.confirmPlacementSession).toHaveBeenCalledWith('jwt-token', 74, expect.objectContaining({
      expectedVersion: 2,
      idempotencyKey: expect.any(String),
    }))
  })

  it('shows a visible access-denied state when confirmation returns 403', async () => {
    await router.push({ name: 'v2-placement-session', params: { placementSessionId: '74' } })
    mocks.confirmPlacementSession.mockRejectedValueOnce(forbiddenError())
    const wrapper = mountView()
    await flushPromises()

    const confirmReviewBtn = wrapper.findAll('button').find((b) => b.text().includes('Xác nhận phần tự động'))
    await confirmReviewBtn?.trigger('click')
    await flushPromises()
    const confirmDialog = wrapper.find('[data-testid="dialog"][data-header="Xác nhận xếp lớp tự động"]')
    const executeConfirmBtn = confirmDialog.findAll('button').find((b) => b.text() === 'Xác nhận')
    await executeConfirmBtn?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Bạn không có quyền xác nhận phiên xếp lớp này')
  })

  it('handles cancel action with dialog confirmation', async () => {
    await router.push({ name: 'v2-placement-session', params: { placementSessionId: '74' } })
    const wrapper = mountView()
    await flushPromises()

    const cancelReviewBtn = wrapper.findAll('button').find((b) => b.text().includes('Hủy phiên'))
    expect(cancelReviewBtn).toBeDefined()
    await cancelReviewBtn?.trigger('click')
    await flushPromises()

    const cancelDialog = wrapper.find('[data-testid="dialog"][data-header="Hủy phiên xếp lớp"]')
    expect(cancelDialog.exists()).toBe(true)

    const executeCancelBtn = cancelDialog.findAll('button').find((b) => b.text() === 'Hủy phiên')
    await executeCancelBtn?.trigger('click')
    await flushPromises()

    expect(mocks.cancelPlacementSession).toHaveBeenCalledWith('jwt-token', 74, 2)
  })

  it('shows a visible access-denied state when cancellation returns 403', async () => {
    await router.push({ name: 'v2-placement-session', params: { placementSessionId: '74' } })
    mocks.cancelPlacementSession.mockRejectedValueOnce(forbiddenError())
    const wrapper = mountView()
    await flushPromises()

    const cancelReviewBtn = wrapper.findAll('button').find((b) => b.text().includes('Hủy phiên'))
    await cancelReviewBtn?.trigger('click')
    await flushPromises()
    const cancelDialog = wrapper.find('[data-testid="dialog"][data-header="Hủy phiên xếp lớp"]')
    const executeCancelBtn = cancelDialog.findAll('button').find((b) => b.text() === 'Hủy phiên')
    await executeCancelBtn?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Bạn không có quyền hủy phiên xếp lớp này')
  })

  it('shows a visible access-denied state when a draft profile update returns 403', async () => {
    await router.push({ name: 'v2-placement-session', params: { placementSessionId: '74' } })
    mocks.getPlacementSession.mockResolvedValueOnce({ ...sampleSession, status: 'DRAFT' })
    mocks.updatePlacementSession.mockRejectedValueOnce(forbiddenError())
    const wrapper = mountView()
    await flushPromises()

    const view = wrapper.vm as unknown as {
      handleUpdateProfile: (classId: number, profile: 'ADVANCED' | 'SUPPORT' | 'REGULAR') => Promise<void>
    }
    await view.handleUpdateProfile(81, 'ADVANCED')
    await flushPromises()

    expect(wrapper.text()).toContain('Bạn không có quyền thay đổi cách phân lớp của phiên này')
  })
})
