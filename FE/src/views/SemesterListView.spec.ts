import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import router from '@/router'
import SemesterListView from './SemesterListView.vue'

const mocks = vi.hoisted(() => ({
  fetchAcademicYears: vi.fn(),
  fetchSemesters: vi.fn(),
  createSemester: vi.fn(),
  updateSemester: vi.fn(),
  activateSemester: vi.fn(),
  getSemesterCompletenessReport: vi.fn(),
  lockSemester: vi.fn(),
  reopenSemester: vi.fn(),
  confirmRequire: vi.fn(),
}))

vi.mock('@/services/academicApi', () => ({
  fetchAcademicYears: mocks.fetchAcademicYears,
  fetchSemesters: mocks.fetchSemesters,
  createSemester: mocks.createSemester,
  updateSemester: mocks.updateSemester,
  activateSemester: mocks.activateSemester,
  getSemesterCompletenessReport: mocks.getSemesterCompletenessReport,
  lockSemester: mocks.lockSemester,
  reopenSemester: mocks.reopenSemester,
}))

vi.mock('primevue/useconfirm', () => ({
  useConfirm: () => ({ require: mocks.confirmRequire }),
}))

const academicYear = { id: 1, code: '2026-2027', startDate: '2026-09-01', endDate: '2027-05-31', status: 'ACTIVE', notes: null }
const semester = { id: 11, academicYearId: 1, code: 'HK1', name: 'Học kỳ I', displayOrder: 1, startDate: '2026-09-01', endDate: '2026-12-31', automaticLockAt: '2027-01-15T17:00:00', status: 'ACTIVE', lockedAt: null, lockedBy: null, lockReason: null, reopenUntil: null }
const report = {
  reportId: 101, runId: 201, semesterId: 11, checkpointCode: 'PRE_LOCK', reportStatus: 'COMPLETE', evaluatedAt: '2027-01-15T16:30:00', scopeType: 'SEMESTER',
  summary: { complete: true, missingKtdkCount: 0, invalidKtckCount: 0, missingSkillColumnsCount: 0, unenteredScoreCount: 0, studentWithoutScoreDataCount: 0, unpublishedScorebookCount: 0, pendingScoreChangeRequestCount: 0, details: [] },
  failureReason: null, correlationId: null,
}
const buttonStub = {
  props: ['label', 'disabled'],
  emits: ['click'],
  template: '<button :disabled="disabled" @click="$emit(\'click\')">{{ label }}</button>',
}
const dialogStub = {
  props: ['visible'],
  template: '<div v-if="visible"><slot /></div>',
}
const textareaStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: '<textarea v-bind="$attrs" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}
const tableStub = {
  props: ['semesters'],
  emits: ['edit', 'activate', 'viewStatus', 'lock', 'reopen'],
  template: `
    <div data-testid="semester-table">
      <span data-testid="semester-count">{{ semesters.length }}</span>
      <button data-testid="create-semester" @click="$emit('edit', null)">Create</button>
      <button data-testid="activate-semester" @click="$emit('activate', semesters[0])">Activate</button>
      <button data-testid="view-status" @click="$emit('viewStatus', semesters[0])">Status</button>
      <button data-testid="reopen-semester" @click="$emit('reopen', semesters[0])">Reopen</button>
    </div>
  `,
}
const semesterDialogStub = {
  props: ['visible', 'mode'],
  emits: ['save', 'cancel', 'update:visible'],
  template: `
    <div v-if="visible" data-testid="semester-dialog">
      <span data-testid="semester-dialog-mode">{{ mode }}</span>
      <button data-testid="semester-dialog-save" @click="$emit('save', { code: 'HK1', name: 'Học kỳ I', displayOrder: 1, startDate: '2026-09-01', endDate: '2026-12-31', automaticLockAt: '2027-01-15T17:00:00' })">Save</button>
    </div>
  `,
}
const statusDialogStub = {
  props: ['visible', 'report', 'loading'],
  emits: ['lock', 'reopen', 'update:visible'],
  template: `
    <div v-if="visible" data-testid="status-dialog">
      <span data-testid="report-loaded">{{ report ? report.reportStatus : 'none' }}</span>
      <button data-testid="status-lock" @click="$emit('lock')">Lock</button>
      <button data-testid="status-reopen" @click="$emit('reopen')">Reopen</button>
    </div>
  `,
}
const pageStateStub = { template: '<div><slot /></div>' }
const formAlertStub = { props: ['message'], template: '<div>{{ message }}</div>' }

function mountView() {
  return mount(SemesterListView, {
    global: {
      plugins: [router],
      stubs: {
        Button: buttonStub,
        ConfirmDialog: true,
        Dialog: dialogStub,
        FormAlert: formAlertStub,
        PageState: pageStateStub,
        SemesterDialog: semesterDialogStub,
        SemesterStatusDialog: statusDialogStub,
        SemesterTable: tableStub,
        Textarea: textareaStub,
      },
    },
  })
}

describe('SemesterListView', () => {
  beforeEach(async () => {
    clearAuthSession()
    saveAuthSession({ accessToken: 'jwt-token', user: { id: 1, username: 'academic.admin' } })
    mocks.fetchAcademicYears.mockReset().mockResolvedValue([academicYear])
    mocks.fetchSemesters.mockReset().mockResolvedValue([semester])
    mocks.createSemester.mockReset().mockResolvedValue(semester)
    mocks.updateSemester.mockReset().mockResolvedValue(semester)
    mocks.activateSemester.mockReset().mockResolvedValue(semester)
    mocks.getSemesterCompletenessReport.mockReset().mockResolvedValue(report)
    mocks.lockSemester.mockReset().mockResolvedValue({ ...semester, status: 'LOCKED' })
    mocks.reopenSemester.mockReset().mockResolvedValue(semester)
    mocks.confirmRequire.mockReset()
    await router.push({ name: 'v2-semesters', params: { academicYearId: 1 } })
  })

  afterEach(() => {
    clearAuthSession()
  })

  it('resolves the academic-year context before loading semesters and loads the completeness report', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(mocks.fetchAcademicYears).toHaveBeenCalledWith('jwt-token')
    expect(mocks.fetchSemesters).toHaveBeenCalledWith('jwt-token', 1)
    expect(wrapper.get('[data-testid="semester-count"]').text()).toBe('1')

    await wrapper.get('[data-testid="view-status"]').trigger('click')
    await flushPromises()

    expect(mocks.getSemesterCompletenessReport).toHaveBeenCalledWith('jwt-token', 11)
    expect(wrapper.get('[data-testid="report-loaded"]').text()).toBe('COMPLETE')
  })

  it('uses lifecycle endpoints for activate and reopen with a required reason', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="activate-semester"]').trigger('click')
    expect(mocks.confirmRequire).toHaveBeenCalledOnce()
    mocks.confirmRequire.mock.calls[0][0].accept()
    await flushPromises()
    expect(mocks.activateSemester).toHaveBeenCalledWith('jwt-token', 11)

    await wrapper.get('[data-testid="reopen-semester"]').trigger('click')
    await wrapper.get('[data-testid="status-reopen"]').trigger('click')
    expect(wrapper.find('#semester-reopen-reason').exists()).toBe(true)
    await wrapper.get('#semester-reopen-reason').setValue('Rà soát bổ sung dữ liệu điểm')
    const submitReopenButton = wrapper.findAll('button').find((button) => button.text() === 'Mở lại học kỳ')
    expect(submitReopenButton).toBeDefined()
    await submitReopenButton?.trigger('click')
    await flushPromises()

    expect(mocks.reopenSemester).toHaveBeenCalledWith('jwt-token', 11, { reason: 'Rà soát bổ sung dữ liệu điểm' })
  })
})
