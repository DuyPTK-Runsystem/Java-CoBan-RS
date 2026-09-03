import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import { ApiError } from '@/types/api'
import type { ResRetakeExamDTO } from '@/types/retake'
import RetakeResultView from './RetakeResultView.vue'

const mocks = vi.hoisted(() => ({
  fetchRetakeExams: vi.fn(),
  fetchRetakeExam: vi.fn(),
  createRetakeExam: vi.fn(),
  updateRetakeScore: vi.fn(),
  cancelRetakeExam: vi.fn(),
  fetchAcademicYears: vi.fn(),
  fetchSubjects: vi.fn(),
  fetchStudents: vi.fn(),
  fetchStudentAnnualTranscript: vi.fn(),
}))

vi.mock('@/services/retakeApi', () => ({
  fetchRetakeExams: mocks.fetchRetakeExams,
  fetchRetakeExam: mocks.fetchRetakeExam,
  createRetakeExam: mocks.createRetakeExam,
  updateRetakeScore: mocks.updateRetakeScore,
  cancelRetakeExam: mocks.cancelRetakeExam,
}))

vi.mock('@/services/academicApi', () => ({
  fetchAcademicYears: mocks.fetchAcademicYears,
  fetchSubjects: mocks.fetchSubjects,
}))

vi.mock('@/services/studentApi', () => ({
  fetchStudents: mocks.fetchStudents,
}))

vi.mock('@/services/transcriptApi', () => ({
  fetchStudentAnnualTranscript: mocks.fetchStudentAnnualTranscript,
}))

const sampleExams: ResRetakeExamDTO[] = [
  {
    retakeId: 7001,
    studentId: 1001,
    academicYearId: 1,
    subjectId: 21,
    preRetakeScore: 4.0,
    retakeScore: 6.5,
    examDate: '2027-06-15',
    status: 'SCORED',
    note: 'Điểm tốt',
  },
  {
    retakeId: 7002,
    studentId: 1002,
    academicYearId: 1,
    subjectId: 22,
    preRetakeScore: 4.5,
    retakeScore: null,
    examDate: '2027-06-15',
    status: 'PLANNED',
    note: null,
  },
]

const buttonStub = {
  props: ['label', 'disabled', 'loading'],
  emits: ['click'],
  template: '<button :disabled="disabled" @click="$emit(\'click\')">{{ label }}</button>',
}
const selectStub = {
  props: ['modelValue', 'options'],
  emits: ['update:modelValue'],
  template: '<select :value="modelValue" @change="$emit(\'update:modelValue\', Number($event.target.value))"><option v-for="opt in options" :key="opt.id ?? opt.value" :value="opt.id ?? opt.value">{{ opt.label }}</option></select>',
}
const tagStub = {
  props: ['value'],
  template: '<span>{{ value }}</span>',
}
const tableStub = {
  props: ['items'],
  emits: ['editScore', 'cancel'],
  template: `
    <div data-testid="table-stub">
      <span data-testid="row-count">{{ items.length }}</span>
      <button data-testid="trigger-score" @click="$emit('editScore', items[0])">Score</button>
      <button data-testid="trigger-cancel" @click="$emit('cancel', items[0])">Cancel</button>
    </div>
  `,
}
const dialogStub = {
  props: ['visible', 'mode', 'item', 'errorMessage'],
  emits: ['update:visible', 'submitCreate', 'submitScore', 'submitCancel', 'cancel'],
  template: `
    <div v-if="visible" data-testid="dialog-stub">
      <span data-testid="stub-mode">{{ mode }}</span>
      <span data-testid="stub-error-message">{{ Array.isArray(errorMessage) ? errorMessage.join(', ') : errorMessage }}</span>
      <button data-testid="stub-save-create" @click="$emit('submitCreate', { studentId: 1001, academicYearId: 1, subjectId: 21, retakeScore: 6.5 })">Save Create</button>
      <button data-testid="stub-save-score" @click="$emit('submitScore', 7001, { retakeScore: 7.0 })">Save Score</button>
      <button data-testid="stub-save-cancel" @click="$emit('submitCancel', 7001)">Save Cancel</button>
    </div>
  `,
}
const serverPaginationStub = {
  props: ['page', 'pageSize', 'totalRecords'],
  emits: ['pageChange'],
  template: '<div data-testid="pagination-stub"><button data-testid="page-next" @click="$emit(\'pageChange\', 1, 10)">Next</button></div>',
}

function mountView() {
  return mount(RetakeResultView, {
    global: {
      stubs: {
        Button: buttonStub,
        Select: selectStub,
        Tag: tagStub,
        RetakeResultTable: tableStub,
        RetakeResultDialog: dialogStub,
        ServerPagination: serverPaginationStub,
      },
    },
  })
}

describe('RetakeResultView', () => {
  beforeEach(() => {
    clearAuthSession()
    saveAuthSession({
      accessToken: 'jwt-token-123',
      user: { id: 1, username: 'academic.office', roles: ['ACADEMIC_OFFICE'] },
    })

    mocks.fetchRetakeExams.mockReset().mockResolvedValue({
      content: sampleExams,
      page: 0,
      size: 10,
      totalElements: 2,
      totalPages: 1,
    })
    mocks.createRetakeExam.mockReset().mockResolvedValue(sampleExams[0])
    mocks.updateRetakeScore.mockReset().mockResolvedValue(sampleExams[0])
    mocks.cancelRetakeExam.mockReset().mockResolvedValue({ ...sampleExams[0], status: 'CANCELLED' })

    mocks.fetchAcademicYears.mockReset().mockResolvedValue([{ id: 1, code: '2026-2027' }])
    mocks.fetchSubjects.mockReset().mockResolvedValue([{ id: 21, name: 'Toán' }])
    mocks.fetchStudents.mockReset().mockResolvedValue({
      content: [{ id: 1001, studentCode: 'HS0001', fullName: 'Nguyễn Minh An' }],
    })
    mocks.fetchStudentAnnualTranscript.mockReset().mockResolvedValue({
      studentId: 1001,
      academicYearId: 1,
      calculationStatus: 'FINISH',
      lastCalculationTaskId: 8801,
      subjects: [
        {
          subjectId: 21,
          subjectName: 'Toán',
          officialDtbmhCn: 6.5,
          calculationSource: 'RETAKE',
        },
      ],
    })
  })

  afterEach(() => {
    clearAuthSession()
  })

  it('loads retake exams and lookups on mount, displaying metrics', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(mocks.fetchRetakeExams).toHaveBeenCalledWith('jwt-token-123', expect.objectContaining({
      page: 0,
      size: 10,
    }))

    expect(wrapper.get('[data-testid="metric-total"]').text()).toBe('2')
    expect(wrapper.get('[data-testid="metric-planned"]').text()).toBe('1')
    expect(wrapper.get('[data-testid="metric-scored"]').text()).toBe('1')
    expect(wrapper.get('[data-testid="metric-cancelled"]').text()).toBe('0')
    expect(wrapper.get('[data-testid="row-count"]').text()).toBe('2')
  })

  it('filters retake exams and resets page to 0', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="btn-filter"]').trigger('click')
    await flushPromises()

    expect(mocks.fetchRetakeExams).toHaveBeenCalledTimes(2)
  })

  it('handles empty state properly', async () => {
    mocks.fetchRetakeExams.mockResolvedValueOnce({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="panel-empty"]').exists()).toBe(true)
  })

  it('handles 403 Forbidden without breaking session', async () => {
    mocks.fetchRetakeExams.mockRejectedValueOnce(
      new ApiError(403, 'Forbidden', { kind: 'forbidden' }),
    )

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="panel-forbidden"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Bạn không có quyền quản lý kỳ thi lại')
  })

  it('shows calculation IN_PROGRESS warning when transcript calculation is running', async () => {
    mocks.fetchStudentAnnualTranscript.mockResolvedValueOnce({
      studentId: 1001,
      academicYearId: 1,
      calculationStatus: 'IN_PROGRESS',
      lastCalculationTaskId: null,
      subjects: [],
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="notice-calculation-in-progress"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="tag-calculation-progress"]').exists()).toBe(true)
  })

  it('opens create dialog, submits, and refreshes list', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="btn-open-create"]').trigger('click')
    expect(wrapper.find('[data-testid="dialog-stub"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="stub-mode"]').text()).toBe('create')

    await wrapper.get('[data-testid="stub-save-create"]').trigger('click')
    await flushPromises()

    expect(mocks.createRetakeExam).toHaveBeenCalledWith('jwt-token-123', expect.objectContaining({
      studentId: 1001,
      academicYearId: 1,
      subjectId: 21,
    }))
    expect(mocks.fetchRetakeExams).toHaveBeenCalledTimes(2)
  })

  it('opens score dialog, submits, and refreshes list', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="trigger-score"]').trigger('click')
    expect(wrapper.find('[data-testid="dialog-stub"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="stub-mode"]').text()).toBe('score')

    await wrapper.get('[data-testid="stub-save-score"]').trigger('click')
    await flushPromises()

    expect(mocks.updateRetakeScore).toHaveBeenCalledWith('jwt-token-123', 7001, {
      retakeScore: 7.0,
    })
    expect(mocks.fetchRetakeExams).toHaveBeenCalledTimes(2)
  })

  it('opens cancel dialog, confirms, and refreshes list', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="trigger-cancel"]').trigger('click')
    expect(wrapper.find('[data-testid="dialog-stub"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="stub-mode"]').text()).toBe('cancel')

    await wrapper.get('[data-testid="stub-save-cancel"]').trigger('click')
    await flushPromises()

    expect(mocks.cancelRetakeExam).toHaveBeenCalledWith('jwt-token-123', 7001)
    expect(mocks.fetchRetakeExams).toHaveBeenCalledTimes(2)
  })

  it('handles pagination page changes', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="page-next"]').trigger('click')
    await flushPromises()

    expect(mocks.fetchRetakeExams).toHaveBeenLastCalledWith('jwt-token-123', expect.objectContaining({
      page: 1,
      size: 10,
    }))
  })

  it('handles 404 not found and renders panel-not-found', async () => {
    mocks.fetchRetakeExams.mockRejectedValueOnce(
      new ApiError(404, 'Not Found', { kind: 'not_found' }),
    )

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="panel-not-found"]').exists()).toBe(true)
  })

  it('handles generic error and renders panel-error with retry button', async () => {
    mocks.fetchRetakeExams.mockRejectedValueOnce(new Error('Network disconnected'))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="panel-error"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="panel-error"]').text()).toContain('Network disconnected')
  })

  it('renders neutral calculation status when no items are calculated', async () => {
    mocks.fetchStudentAnnualTranscript.mockResolvedValue({
      studentId: 1001,
      academicYearId: 1,
      calculationStatus: null,
      lastCalculationTaskId: null,
      subjects: [],
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="tag-calculation-none"]').exists()).toBe(true)
  })

  it('prioritizes backend message over hardcoded fallback when create fails with 409 Conflict', async () => {
    const backendMessage = 'Chưa có điểm tổng kết thường (regular_dtbmh_cn)...'
    mocks.createRetakeExam.mockRejectedValueOnce(
      new ApiError(409, backendMessage, { rawMessages: [backendMessage] }),
    )

    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="btn-open-create"]').trigger('click')
    await wrapper.get('[data-testid="stub-save-create"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="dialog-stub"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="stub-error-message"]').text()).toBe(backendMessage)
  })

  it('falls back to generic 409 message in create dialog when backend returns no specific message', async () => {
    mocks.createRetakeExam.mockRejectedValueOnce(
      new ApiError(409, 'Conflict', { rawMessages: [], globalMessages: [] }),
    )

    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="btn-open-create"]').trigger('click')
    await wrapper.get('[data-testid="stub-save-create"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="stub-error-message"]').text()).toBe(
      '409 Conflict: Record cùng student/year/subject đã tồn tại hoặc lifecycle không cho phép thao tác.',
    )
  })

  it('prioritizes backend message over hardcoded fallback when update score fails with 409 Conflict', async () => {
    const backendMessage = 'Bản ghi đã khóa sổ điểm, không thể sửa điểm'
    mocks.updateRetakeScore.mockRejectedValueOnce(
      new ApiError(409, backendMessage, { rawMessages: [backendMessage] }),
    )

    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="trigger-score"]').trigger('click')
    await wrapper.get('[data-testid="stub-save-score"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="dialog-stub"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="stub-error-message"]').text()).toBe(backendMessage)
  })

  it('falls back to generic 409 message in score dialog when backend returns no specific message', async () => {
    mocks.updateRetakeScore.mockRejectedValueOnce(
      new ApiError(409, 'The request conflicts with existing data.', { rawMessages: [], globalMessages: [] }),
    )

    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="trigger-score"]').trigger('click')
    await wrapper.get('[data-testid="stub-save-score"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="stub-error-message"]').text()).toBe(
      '409 Conflict: Dữ liệu đã thay đổi hoặc lifecycle không cho phép cập nhật điểm.',
    )
  })

  it('prioritizes backend message over hardcoded fallback when cancel fails with 409 Conflict', async () => {
    const backendMessage = 'Record ở trạng thái CANCELLED từ trước'
    mocks.cancelRetakeExam.mockRejectedValueOnce(
      new ApiError(409, backendMessage, { rawMessages: [backendMessage] }),
    )

    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="trigger-cancel"]').trigger('click')
    await wrapper.get('[data-testid="stub-save-cancel"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="dialog-stub"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="stub-error-message"]').text()).toBe(backendMessage)
  })

  it('falls back to generic 409 message in cancel dialog when backend returns no specific message', async () => {
    mocks.cancelRetakeExam.mockRejectedValueOnce(
      new ApiError(409, 'Conflict', { rawMessages: [], globalMessages: [] }),
    )

    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="trigger-cancel"]').trigger('click')
    await wrapper.get('[data-testid="stub-save-cancel"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="stub-error-message"]').text()).toBe(
      '409 Conflict: Record đã bị hủy hoặc không được phép hủy ở trạng thái hiện tại.',
    )
  })

  it('prioritizes backend message when fetchRetakeExams fails with 409 with specific message', async () => {
    const backendMessage = 'Năm học chưa mở đợt thi lại'
    mocks.fetchRetakeExams.mockRejectedValueOnce(
      new ApiError(409, backendMessage, { rawMessages: [backendMessage] }),
    )

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="panel-error"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="panel-error"]').text()).toContain(backendMessage)
  })

  it('falls back to default Vietnamese message when fetchRetakeExams fails with 409 without message', async () => {
    mocks.fetchRetakeExams.mockRejectedValueOnce(
      new ApiError(409, 'Conflict', { rawMessages: [], globalMessages: [] }),
    )

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="panel-error"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="panel-error"]').text()).toContain(
      'Bản ghi cùng học sinh/năm học/môn học đã tồn tại hoặc trạng thái không cho phép thao tác',
    )
  })

  it('passes array of backend error messages to create dialog when backend returns multiple errors', async () => {
    const errorList = ['Lỗi 1: Điểm không hợp lệ', 'Lỗi 2: Ngày thi không hợp lệ']
    mocks.createRetakeExam.mockRejectedValueOnce(
      new ApiError(409, 'Conflict', { rawMessages: errorList }),
    )

    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="btn-open-create"]').trigger('click')
    await wrapper.get('[data-testid="stub-save-create"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="stub-error-message"]').text()).toBe(
      'Lỗi 1: Điểm không hợp lệ, Lỗi 2: Ngày thi không hợp lệ',
    )
  })

  it('prioritizes custom backend message on 403 Forbidden in both banner and panel', async () => {
    const customForbidden = 'Tài khoản không thuộc tổ chuyên môn quản lý kỳ thi lại'
    mocks.fetchRetakeExams.mockRejectedValueOnce(
      new ApiError(403, customForbidden, { rawMessages: [customForbidden] }),
    )

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="panel-forbidden"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="panel-forbidden"]').text()).toContain(customForbidden)
  })
})
