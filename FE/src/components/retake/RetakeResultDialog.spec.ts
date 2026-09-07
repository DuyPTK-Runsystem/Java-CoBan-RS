import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import RetakeResultDialog from './RetakeResultDialog.vue'
import type { RetakeRowItem } from '@/types/retake'

const sampleStudents = [
  { id: 1001, code: 'HS0001', name: 'Nguyễn Minh An' },
  { id: 1002, code: 'HS0002', name: 'Trần Gia Bảo' },
]

const sampleYears = [
  { id: 1, code: '2026-2027' },
]

const sampleSubjects = [
  { id: 21, name: 'Toán' },
  { id: 22, name: 'Ngữ văn' },
]

const sampleItem: RetakeRowItem = {
  retakeId: 7001,
  studentId: 1001,
  studentCode: 'HS0001',
  studentName: 'Nguyễn Minh An',
  academicYearId: 1,
  academicYearCode: '2026-2027',
  subjectId: 21,
  subjectName: 'Toán',
  preRetakeScore: 4.0,
  retakeScore: 6.5,
  officialDtbmhCn: 6.5,
  examDate: '2027-06-15',
  status: 'SCORED',
  calculationStatus: 'FINISH',
  calculationSource: 'RETAKE',
  lastTaskId: 8801,
  note: 'Điểm đã đối chiếu biên bản.',
}

// Stubs for PrimeVue components so we can test easily and reliably
const dialogStub = {
  props: ['visible', 'header'],
  template: '<div v-if="visible" data-testid="dialog-root"><h3>{{ header }}</h3><slot /><slot name="footer" /></div>',
}
const inputNumberStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: '<input type="number" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value === \'\' ? null : Number($event.target.value))" />',
}
const inputTextStub = {
  name: 'InputText',
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: '<input type="text" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}
const textareaStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: '<textarea :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)"></textarea>',
}
const selectStub = {
  props: ['modelValue', 'options'],
  emits: ['update:modelValue'],
  template: '<select :value="modelValue" @change="$emit(\'update:modelValue\', Number($event.target.value))"><option v-for="opt in options" :key="opt.id" :value="opt.id">{{ opt.name || opt.code }}</option></select>',
}
const buttonStub = {
  props: ['label', 'disabled'],
  emits: ['click'],
  template: '<button :disabled="disabled" @click="$emit(\'click\')">{{ label }}</button>',
}

function mountDialog(propsOverrides: Record<string, unknown> = {}) {
  return mount(RetakeResultDialog, {
    props: {
      visible: true,
      mode: 'create',
      students: sampleStudents,
      academicYears: sampleYears,
      subjects: sampleSubjects,
      ...propsOverrides,
    },
    global: {
      stubs: {
        Dialog: dialogStub,
        InputNumber: inputNumberStub,
        InputText: inputTextStub,
        Textarea: textareaStub,
        Select: selectStub,
        Button: buttonStub,
      },
    },
  })
}

describe('RetakeResultDialog', () => {
  it('renders create mode and submits valid create data', async () => {
    const wrapper = mountDialog({ mode: 'create' })

    expect(wrapper.text()).toContain('Tạo kỳ thi lại')
    expect(wrapper.text()).toContain('Snapshot:')

    // Trigger save
    await wrapper.find('[data-testid="btn-dialog-save-create"]').trigger('click')

    expect(wrapper.emitted('submitCreate')).toBeTruthy()
    expect(wrapper.emitted('submitCreate')?.[0]?.[0]).toEqual({
      studentId: 1001,
      academicYearId: 1,
      subjectId: 21,
      examDate: undefined,
      retakeScore: undefined,
      note: undefined,
    })
  })

  it('validates score range and precision in score mode', async () => {
    const wrapper = mountDialog({
      mode: 'score',
      item: sampleItem,
    })

    expect(wrapper.text()).toContain('Nhập/sửa điểm thi lại')
    expect(wrapper.text()).toContain('Trước thi lại · preRetakeScore')
    expect(wrapper.text()).toContain('4.0')
    expect(wrapper.find('[data-testid="notice-official"]').exists()).toBe(true)

    // Set invalid score: > 10
    const scoreInput = wrapper.find('[data-testid="input-retake-score"]')
    await scoreInput.setValue(11)
    await wrapper.find('[data-testid="btn-dialog-save-score"]').trigger('click')

    expect(wrapper.find('[data-testid="dialog-validation-error"]').text()).toContain(
      'Điểm thi lại phải từ 0.0 đến 10.0',
    )
    expect(wrapper.emitted('submitScore')).toBeFalsy()

    // Set invalid score: more than 1 decimal place (e.g. 8.55)
    await scoreInput.setValue(8.55)
    await wrapper.find('[data-testid="btn-dialog-save-score"]').trigger('click')

    expect(wrapper.find('[data-testid="dialog-validation-error"]').text()).toContain(
      'Điểm thi lại chỉ được có tối đa 1 chữ số thập phân',
    )

    // Set valid score: 0.0 (boundary edge case!)
    await scoreInput.setValue(0)
    await wrapper.find('[data-testid="btn-dialog-save-score"]').trigger('click')

    expect(wrapper.emitted('submitScore')).toBeTruthy()
    expect(wrapper.emitted('submitScore')?.[0]?.[0]).toBe(7001)
    expect(wrapper.emitted('submitScore')?.[0]?.[1]).toEqual(
      expect.objectContaining({
        retakeScore: 0,
      }),
    )
  })

  it('validates empty score in score mode', async () => {
    const wrapper = mountDialog({
      mode: 'score',
      item: { ...sampleItem, retakeScore: null },
    })

    await wrapper.find('[data-testid="btn-dialog-save-score"]').trigger('click')
    expect(wrapper.find('[data-testid="dialog-validation-error"]').text()).toContain(
      'Điểm thi lại không được để trống',
    )
    expect(wrapper.emitted('submitScore')).toBeFalsy()
  })

  it('renders cancel mode and emits submitCancel', async () => {
    const wrapper = mountDialog({
      mode: 'cancel',
      item: sampleItem,
    })

    expect(wrapper.text()).toContain('Hủy kỳ thi lại?')
    expect(wrapper.text()).toContain('Ảnh hưởng:')

    await wrapper.find('[data-testid="btn-dialog-confirm-cancel"]').trigger('click')
    expect(wrapper.emitted('submitCancel')).toBeTruthy()
    expect(wrapper.emitted('submitCancel')?.[0]?.[0]).toBe(7001)
  })

  it('emits update:visible false and cancel when clicking Hủy', async () => {
    const wrapper = mountDialog({ mode: 'create' })

    await wrapper.find('[data-testid="btn-dialog-cancel"]').trigger('click')
    expect(wrapper.emitted('update:visible')?.[0]?.[0]).toBe(false)
    expect(wrapper.emitted('cancel')).toBeTruthy()
  })

  it('disables inputs and prevents submission when item is CANCELLED in score mode', async () => {
    const wrapper = mountDialog({
      mode: 'score',
      item: { ...sampleItem, status: 'CANCELLED' },
    })

    expect(wrapper.find('[data-testid="notice-cancelled-readonly"]').exists()).toBe(true)
    const saveBtn = wrapper.find('[data-testid="btn-dialog-save-score"]')
    expect(saveBtn.attributes('disabled')).toBeDefined()

    await saveBtn.trigger('click')
    expect(wrapper.emitted('submitScore')).toBeFalsy()
  })

  it('validates invalid date format', async () => {
    const wrapper = mountDialog({
      mode: 'score',
      item: sampleItem,
    })

    await wrapper.findComponent({ name: 'InputText' }).vm.$emit('update:modelValue', 'invalid-date')
    await wrapper.find('[data-testid="btn-dialog-save-score"]').trigger('click')

    expect(wrapper.find('[data-testid="dialog-validation-error"]').text()).toContain(
      'Ngày thi không đúng định dạng yyyy-MM-dd',
    )
    expect(wrapper.emitted('submitScore')).toBeFalsy()
  })

  it('validates required fields in create mode when options are empty', async () => {
    const wrapper = mountDialog({
      mode: 'create',
      students: [],
      academicYears: [],
      subjects: [],
    })

    await wrapper.find('[data-testid="btn-dialog-save-create"]').trigger('click')
    expect(wrapper.find('[data-testid="dialog-validation-error"]').text()).toContain(
      'Vui lòng chọn học sinh, năm học và môn học',
    )
    expect(wrapper.emitted('submitCreate')).toBeFalsy()
  })

  it('renders backend error message when passed as a string', async () => {
    const backendMessage = 'Chưa có điểm tổng kết thường (regular_dtbmh_cn)...'
    const wrapper = mountDialog({
      mode: 'create',
      errorMessage: backendMessage,
    })

    const errorAlert = wrapper.find('[data-testid="dialog-api-error"]')
    expect(errorAlert.exists()).toBe(true)
    expect(errorAlert.text()).toContain(backendMessage)
  })

  it('renders backend error messages when passed as an array of strings', async () => {
    const errorMessages = [
      'Chưa có điểm tổng kết thường',
      'Vui lòng hoàn thành điểm thường trước khi nhập kỳ thi lại',
    ]
    const wrapper = mountDialog({
      mode: 'score',
      item: sampleItem,
      errorMessage: errorMessages,
    })

    const errorAlert = wrapper.find('[data-testid="dialog-api-error"]')
    expect(errorAlert.exists()).toBe(true)
    expect(errorAlert.text()).toContain('Chưa có điểm tổng kết thường')
    expect(errorAlert.text()).toContain('Vui lòng hoàn thành điểm thường trước khi nhập kỳ thi lại')
  })

  it('does not render dialog-api-error when errorMessage is empty or not provided', async () => {
    const wrapper = mountDialog({
      mode: 'create',
      errorMessage: '',
    })

    expect(wrapper.find('[data-testid="dialog-api-error"]').exists()).toBe(false)
  })

  it('renders newline-separated string as a list of error messages', async () => {
    const errorText = 'Chưa có điểm tổng kết thường\nVui lòng hoàn thành điểm thường trước'
    const wrapper = mountDialog({
      mode: 'create',
      errorMessage: errorText,
    })

    const errorAlert = wrapper.find('[data-testid="dialog-api-error"]')
    expect(errorAlert.exists()).toBe(true)
    expect(errorAlert.text()).toContain('Chưa có điểm tổng kết thường')
    expect(errorAlert.text()).toContain('Vui lòng hoàn thành điểm thường trước')
  })
})
