import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import router from '@/router'
import SubjectListView from './SubjectListView.vue'

const mocks = vi.hoisted(() => ({
  fetchSubjects: vi.fn(),
  fetchAcademicYears: vi.fn(),
  fetchGrades: vi.fn(),
  fetchSchoolClasses: vi.fn(),
  fetchSemesters: vi.fn(),
  fetchSubjectApplicabilities: vi.fn(),
  createSubjectApplicability: vi.fn(),
  updateSubjectApplicability: vi.fn(),
  deactivateSubjectApplicability: vi.fn(),
  confirmRequire: vi.fn(),
}))

vi.mock('@/services/academicApi', () => ({
  fetchSubjects: mocks.fetchSubjects,
  fetchAcademicYears: mocks.fetchAcademicYears,
  fetchGrades: mocks.fetchGrades,
  fetchSchoolClasses: mocks.fetchSchoolClasses,
  fetchSemesters: mocks.fetchSemesters,
  fetchSubjectApplicabilities: mocks.fetchSubjectApplicabilities,
  createSubjectApplicability: mocks.createSubjectApplicability,
  updateSubjectApplicability: mocks.updateSubjectApplicability,
  deactivateSubjectApplicability: mocks.deactivateSubjectApplicability,
}))

vi.mock('primevue/useconfirm', () => ({
  useConfirm: () => ({ require: mocks.confirmRequire }),
}))

const subject = { id: 101, code: 'MAT', name: 'Toán', subjectType: 'ACADEMIC', applicationScope: 'GRADE', status: 'ACTIVE' }
const applicability = { id: 501, subjectId: 101, semesterId: 11, scopeType: 'GRADE', gradeLevelId: 1, classId: null, status: 'ACTIVE' }

const buttonStub = { props: ['label'], template: '<button v-bind="$attrs">{{ label }}</button>' }
const selectStub = { props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'placeholder', 'invalid', 'fluid'], template: '<select :value="modelValue"><slot /></select>' }
const inputTextStub = { props: ['modelValue'], template: '<input v-bind="$attrs" :value="modelValue" />' }
const pageStateStub = { template: '<div><slot /></div>' }
const formAlertStub = { props: ['message'], template: '<div>{{ message }}</div>' }
const subjectTableStub = {
  props: ['subjects'],
  template: '<div><button data-testid="open-applicability" @click="$emit(\'configureApplicability\', subjects[0])">Open</button></div>',
}
const subjectDialogStub = { template: '<div />' }
const applicabilityDialogStub = {
  props: ['visible', 'applicabilities', 'mode'],
  emits: ['save', 'create', 'edit', 'deactivate', 'reactivate', 'cancel', 'update:visible'],
  template: `
    <div v-if="visible" data-testid="applicability-dialog">
      <span data-testid="applicability-count">{{ applicabilities.length }}</span>
      <button data-testid="save-applicability" @click="$emit('save', { semesterId: 11, scopeType: 'GRADE', gradeLevelId: 1, classId: null })">Save</button>
      <button data-testid="deactivate-applicability" @click="$emit('deactivate', applicabilities[0])">Deactivate</button>
    </div>
  `,
}

function mountView() {
  return mount(SubjectListView, {
    global: {
      plugins: [router],
      stubs: {
        Button: buttonStub,
        ConfirmDialog: true,
        FormAlert: formAlertStub,
        InputText: inputTextStub,
        PageState: pageStateStub,
        Select: selectStub,
        SubjectApplicabilityDialog: applicabilityDialogStub,
        SubjectDialog: subjectDialogStub,
        SubjectTable: subjectTableStub,
      },
    },
  })
}

describe('SubjectListView applicability flow', () => {
  beforeEach(async () => {
    clearAuthSession()
    saveAuthSession({ accessToken: 'jwt-token', user: { id: 1, username: 'academic.admin' } })
    mocks.fetchSubjects.mockReset().mockResolvedValue([subject])
    mocks.fetchAcademicYears.mockReset().mockResolvedValue([{ id: 1, code: '2026-2027', status: 'ACTIVE' }])
    mocks.fetchGrades.mockReset().mockResolvedValue([{ id: 1, name: 'Khối 6' }])
    mocks.fetchSchoolClasses.mockReset().mockResolvedValue([])
    mocks.fetchSemesters.mockReset().mockResolvedValue([{ id: 11, name: 'Học kỳ 1' }])
    mocks.fetchSubjectApplicabilities.mockReset().mockResolvedValue([applicability])
    mocks.createSubjectApplicability.mockReset().mockResolvedValue(applicability)
    mocks.updateSubjectApplicability.mockReset().mockResolvedValue(applicability)
    mocks.deactivateSubjectApplicability.mockReset().mockResolvedValue(undefined)
    mocks.confirmRequire.mockReset()
    await router.push({ name: 'v2-academic-subjects' })
  })

  afterEach(() => {
    clearAuthSession()
  })

  it('loads real applicability data and reloads it after create', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="open-applicability"]').trigger('click')
    await flushPromises()
    expect(mocks.fetchSubjectApplicabilities).toHaveBeenCalledWith('jwt-token', 101)
    expect(wrapper.get('[data-testid="applicability-count"]').text()).toBe('1')

    await wrapper.get('[data-testid="save-applicability"]').trigger('click')
    await flushPromises()

    expect(mocks.createSubjectApplicability).toHaveBeenCalledWith('jwt-token', 101, {
      semesterId: 11, scopeType: 'GRADE', gradeLevelId: 1, classId: null,
    })
    expect(mocks.fetchSubjectApplicabilities).toHaveBeenCalledTimes(2)
  })

  it('confirms deactivation before calling the soft-delete service', async () => {
    const wrapper = mountView()
    await flushPromises()
    await wrapper.get('[data-testid="open-applicability"]').trigger('click')
    await flushPromises()

    await wrapper.get('[data-testid="deactivate-applicability"]').trigger('click')
    expect(mocks.confirmRequire).toHaveBeenCalledOnce()
    mocks.confirmRequire.mock.calls[0][0].accept()
    await flushPromises()

    expect(mocks.deactivateSubjectApplicability).toHaveBeenCalledWith('jwt-token', 101, 501)
  })
})
