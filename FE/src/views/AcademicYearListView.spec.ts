import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import router from '@/router'
import AcademicYearListView from './AcademicYearListView.vue'

const mocks = vi.hoisted(() => ({
  fetchAcademicYears: vi.fn(),
  createAcademicYear: vi.fn(),
  updateAcademicYear: vi.fn(),
  closeAcademicYear: vi.fn(),
  confirmRequire: vi.fn(),
}))

vi.mock('@/services/academicApi', () => ({
  fetchAcademicYears: mocks.fetchAcademicYears,
  createAcademicYear: mocks.createAcademicYear,
  updateAcademicYear: mocks.updateAcademicYear,
  closeAcademicYear: mocks.closeAcademicYear,
}))

vi.mock('primevue/useconfirm', () => ({
  useConfirm: () => ({ require: mocks.confirmRequire }),
}))

const academicYears = [
  { id: 1, code: '2026-2027', startDate: '2026-09-01', endDate: '2027-05-31', status: 'ACTIVE', notes: 'Hiện tại' },
  { id: 2, code: '2025-2026', startDate: '2025-09-01', endDate: '2026-05-31', status: 'CLOSED', notes: null },
]

const buttonStub = {
  props: ['label', 'disabled'],
  emits: ['click'],
  template: '<button :disabled="disabled" @click="$emit(\'click\')">{{ label }}</button>',
}
const inputTextStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: '<input v-bind="$attrs" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}
const selectStub = {
  props: ['modelValue', 'options'],
  emits: ['update:modelValue'],
  template: '<select :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)" />',
}
const tableStub = {
  props: ['academicYears'],
  emits: ['edit', 'close', 'viewSemesters'],
  template: `
    <div data-testid="year-table">
      <span data-testid="year-count">{{ academicYears.length }}</span>
      <button data-testid="edit-year" @click="$emit('edit', academicYears[0])">Edit</button>
      <button data-testid="close-year" @click="$emit('close', academicYears[0])">Close</button>
      <button data-testid="view-semesters" @click="$emit('viewSemesters', academicYears[0])">Semesters</button>
    </div>
  `,
}
const dialogStub = {
  props: ['visible', 'mode', 'initialValue'],
  emits: ['save', 'cancel', 'update:visible'],
  template: `
    <div v-if="visible" data-testid="academic-year-dialog">
      <span data-testid="dialog-mode">{{ mode }}</span>
      <button data-testid="dialog-save" @click="$emit('save', { code: '2026-2027', startDate: '2026-09-01', endDate: '2027-05-31', status: 'ACTIVE', notes: '' })">Save</button>
    </div>
  `,
}
const pageStateStub = { template: '<div><slot /></div>' }
const formAlertStub = { props: ['message'], template: '<div>{{ message }}</div>' }

function mountView() {
  return mount(AcademicYearListView, {
    global: {
      plugins: [router],
      stubs: {
        AcademicYearDialog: dialogStub,
        AcademicYearTable: tableStub,
        Button: buttonStub,
        ConfirmDialog: true,
        FormAlert: formAlertStub,
        InputText: inputTextStub,
        PageState: pageStateStub,
        Select: selectStub,
      },
    },
  })
}

describe('AcademicYearListView', () => {
  beforeEach(async () => {
    clearAuthSession()
    saveAuthSession({ accessToken: 'jwt-token', user: { id: 1, username: 'academic.admin' } })
    mocks.fetchAcademicYears.mockReset().mockResolvedValue(academicYears)
    mocks.createAcademicYear.mockReset().mockResolvedValue(academicYears[0])
    mocks.updateAcademicYear.mockReset().mockResolvedValue(academicYears[0])
    mocks.closeAcademicYear.mockReset().mockResolvedValue(academicYears[0])
    mocks.confirmRequire.mockReset()
    await router.push({ name: 'v2-academic-years' })
  })

  afterEach(() => {
    clearAuthSession()
  })

  it('loads and filters the complete academic-year list locally', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(mocks.fetchAcademicYears).toHaveBeenCalledWith('jwt-token')
    expect(wrapper.get('[data-testid="year-count"]').text()).toBe('2')

    await wrapper.findComponent(inputTextStub).vm.$emit('update:modelValue', '2027')
    await flushPromises()

    expect(wrapper.get('[data-testid="year-count"]').text()).toBe('1')
  })

  it('opens edit, saves through the API, closes a year after confirmation, and navigates to semesters', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="edit-year"]').trigger('click')
    expect(wrapper.get('[data-testid="dialog-mode"]').text()).toBe('edit')
    await wrapper.get('[data-testid="dialog-save"]').trigger('click')
    await flushPromises()

    expect(mocks.updateAcademicYear).toHaveBeenCalledWith('jwt-token', 1, {
      code: '2026-2027', startDate: '2026-09-01', endDate: '2027-05-31', status: 'ACTIVE', notes: null,
    })

    await wrapper.get('[data-testid="close-year"]').trigger('click')
    expect(mocks.confirmRequire).toHaveBeenCalledOnce()
    mocks.confirmRequire.mock.calls[0][0].accept()
    await flushPromises()
    expect(mocks.closeAcademicYear).toHaveBeenCalledWith('jwt-token', 1)

    const pushSpy = vi.spyOn(router, 'push')
    const viewSemesters = (wrapper.vm as unknown as { viewSemesters: (academicYear: typeof academicYears[number]) => Promise<void> }).viewSemesters
    await viewSemesters(academicYears[0])
    expect(pushSpy).toHaveBeenCalledWith({ name: 'v2-semesters', params: { academicYearId: 1 } })
    pushSpy.mockRestore()
  })
})
