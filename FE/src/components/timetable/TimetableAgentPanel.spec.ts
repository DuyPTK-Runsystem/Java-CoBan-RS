import { defineComponent, nextTick, type PropType } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TimetableAgentPanel from './TimetableAgentPanel.vue'
import type { TimetableEntry } from '@/types/timetable'

function createWidgetStub(name: string, buttonLike = false) {
  return defineComponent({
    name,
    props: {
      modelValue: { type: null as unknown as PropType<unknown>, default: undefined },
      options: { type: Array as PropType<unknown[]>, default: () => [] },
      inputId: { type: String, default: '' },
      disabled: { type: Boolean, default: false },
      label: { type: String, default: '' },
      buttonLike: { type: Boolean, default: buttonLike },
    },
    emits: ['update:modelValue'],
    template: '<button v-if="buttonLike" type="submit" :disabled="disabled">{{ label }}</button><div v-else :data-testid="inputId" />',
  })
}

const MultiSelectStub = createWidgetStub('PrimeVueMultiSelectStub')
const InputNumberStub = createWidgetStub('PrimeVueInputNumberStub')
const CheckboxStub = createWidgetStub('PrimeVueCheckboxStub')
const TextareaStub = createWidgetStub('PrimeVueTextareaStub')
const ButtonStub = createWidgetStub('PrimeVueButtonStub', true)

const assignments = [
  { id: 501, classId: 11, className: '10A1', subjectName: 'Toán', teacherName: 'Cô An' },
]

function mountPanel(canGenerate = true, existingEntries: TimetableEntry[] = [], defaultValidFrom = '2026-10-05', defaultValidTo = '2026-12-31') {
  return mount(TimetableAgentPanel, {
    props: {
      targetRevisionId: 42,
      expectedVersion: 7,
      defaultValidFrom,
      defaultValidTo,
      classes: [{ id: 11, name: '10A1' }, { id: 12, name: '10A2' }],
      assignments,
      existingEntries,
      canGenerate,
      busy: false,
    },
    global: {
      stubs: {
        MultiSelect: MultiSelectStub,
        InputNumber: InputNumberStub,
        Checkbox: CheckboxStub,
        Textarea: TextareaStub,
        Button: ButtonStub,
      },
    },
  })
}

describe('TimetableAgentPanel', () => {
  it('submits the selected scope only after each demand is confirmed', async () => {
    const wrapper = mountPanel()
    const [classPicker, lockedPicker] = wrapper.findAllComponents(MultiSelectStub)
    classPicker!.vm.$emit('update:modelValue', [11])
    await nextTick()
    wrapper.findComponent(InputNumberStub).vm.$emit('update:modelValue', 4)
    wrapper.findComponent(CheckboxStub).vm.$emit('update:modelValue', true)
    await nextTick()

    await wrapper.find('form').trigger('submit')

    expect(wrapper.emitted('generate')).toEqual([[
      {
        targetRevisionId: 42,
        expectedVersion: 7,
        classIds: [11],
        validFrom: '2026-10-05',
        validTo: '2026-12-31',
        demands: [{ assignmentId: 501, periodsPerWeek: 4 }],
        lockedEntryIds: [],
        preferences: '',
        userRequest: '',
      },
    ]])
    expect(lockedPicker!.props('options')).toEqual([])
    expect(wrapper.emitted('classesChanged')).toEqual([[[11]]])
  })

  it('autofills one weekly pattern from matching entries without adding successive schedules', async () => {
    const row: TimetableEntry = {
      id: 1, revisionId: 42, assignmentId: 501, periodId: 71,
      validFrom: '2026-10-01', validTo: '2026-11-30',
      classId: 11, className: '10A1', subjectId: 90, subjectName: 'Toán',
      teacherId: 20, teacherName: 'Cô An', dayOfWeek: 1, session: 'MORNING', periodIndex: 1,
    }
    const wrapper = mountPanel(true, [
      row, { ...row, id: 2, periodIndex: 2 },
      { ...row, id: 3, validFrom: '2026-12-01', validTo: '2026-12-31', periodIndex: 3 },
      { ...row, id: 4, assignmentId: 502, periodIndex: 4 },
      { ...row, id: 5, classId: 12, periodIndex: 4 },
      { ...row, id: 6, revisionId: 43, periodIndex: 4 },
      { ...row, id: 7 },
    ])
    wrapper.findComponent(MultiSelectStub).vm.$emit('update:modelValue', [11])
    await nextTick()
    expect(wrapper.findComponent(InputNumberStub).props('modelValue')).toBe(2)
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()

    await wrapper.get('#agent-from').setValue('2026-12-01')
    expect(wrapper.findComponent(InputNumberStub).props('modelValue')).toBe(1)
    await wrapper.get('#agent-from').setValue('2026-12-31')
    await wrapper.setProps({ existingEntries: [] })
    expect(wrapper.findComponent(InputNumberStub).props('modelValue')).toBeNull()
  })

  it('fills asynchronously loaded assignments and preserves manual edits including cleared values', async () => {
    const row: TimetableEntry = {
      id: 1, revisionId: 42, assignmentId: 501, periodId: 71,
      validFrom: '2026-11-01', validTo: '2026-12-31',
      classId: 11, className: '10A1', subjectId: 90, subjectName: 'Toán',
      teacherId: 20, teacherName: 'Cô An', dayOfWeek: 1, session: 'MORNING', periodIndex: 1,
    }
    const wrapper = mountPanel()
    await wrapper.setProps({ assignments: [] })
    wrapper.findComponent(MultiSelectStub).vm.$emit('update:modelValue', [11])
    await nextTick()
    await wrapper.setProps({ assignments, existingEntries: [row] })
    expect(wrapper.findComponent(InputNumberStub).props('modelValue')).toBe(1)
    wrapper.findComponent(InputNumberStub).vm.$emit('update:modelValue', 4)
    await nextTick()
    await wrapper.setProps({ existingEntries: [row, { ...row, id: 2, periodIndex: 2 }] })
    expect(wrapper.findComponent(InputNumberStub).props('modelValue')).toBe(4)
    wrapper.findComponent(InputNumberStub).vm.$emit('update:modelValue', null)
    await nextTick()
    await wrapper.get('#agent-from').setValue('2026-11-01')
    expect(wrapper.findComponent(InputNumberStub).props('modelValue')).toBeNull()
    await wrapper.setProps({ targetRevisionId: 43 })
    wrapper.findComponent(MultiSelectStub).vm.$emit('update:modelValue', [11])
    await nextTick()
    expect(wrapper.findComponent(InputNumberStub).props('modelValue')).toBeNull()
  })

  it('keeps generation unavailable when the backend capability is false', async () => {
    const wrapper = mountPanel(false)

    expect(wrapper.text()).toContain('Gợi ý thời khoá biểu hiện chưa khả dụng')
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()

    await wrapper.find('form').trigger('submit')
    expect(wrapper.emitted('generate')).toBeUndefined()
  })

  it('offers only locked rows fully contained in the selected class and date range', async () => {
    const rows: TimetableEntry[] = [1, 2, 3, 4].map((id) => ({
      id,
      revisionId: 42,
      assignmentId: 501 + id,
      periodId: 70 + id,
      validFrom: ['2026-10-01', '2026-11-01', '2026-11-15', '2026-11-01'][id - 1]!,
      validTo: ['2026-11-15', '2026-11-30', '2026-12-01', '2026-11-30'][id - 1]!,
      classId: id === 4 ? 12 : 11,
      className: id === 4 ? '10A2' : '10A1',
      subjectId: 90,
      subjectName: 'Toán',
      teacherId: 20,
      teacherName: 'Cô An',
      dayOfWeek: 1,
      session: 'MORNING',
      periodIndex: id,
    }))
    const wrapper = mountPanel(true, rows, '2026-11-01', '2026-11-30')
    const [classPicker, lockedPicker] = wrapper.findAllComponents(MultiSelectStub)
    classPicker!.vm.$emit('update:modelValue', [11])
    await nextTick()

    expect(lockedPicker!.props('options')).toEqual([
      { id: 2, label: '10A1 · Toán · Thứ Hai · Sáng tiết 2 · 2026-11-01 → 2026-11-30' },
    ])
  })

  it('renders demands as a table with classes as rows and subjects as columns', async () => {
    const multiAssignments = [
      { id: 101, classId: 11, className: '10A1', subjectName: 'Toán', teacherName: 'Cô An' },
      { id: 102, classId: 11, className: '10A1', subjectName: 'Văn', teacherName: 'Thầy Bình' },
      { id: 103, classId: 12, className: '10A2', subjectName: 'Toán', teacherName: 'Cô An' },
    ]
    const wrapper = mount(TimetableAgentPanel, {
      props: {
        targetRevisionId: 42,
        expectedVersion: 7,
        defaultValidFrom: '2026-10-05',
        defaultValidTo: '2026-12-31',
        classes: [{ id: 11, name: '10A1' }, { id: 12, name: '10A2' }],
        assignments: multiAssignments,
        existingEntries: [],
        canGenerate: true,
        busy: false,
      },
      global: {
        stubs: {
          MultiSelect: MultiSelectStub,
          InputNumber: InputNumberStub,
          Checkbox: CheckboxStub,
          Textarea: TextareaStub,
          Button: ButtonStub,
        },
      },
    })
    const classPicker = wrapper.findComponent(MultiSelectStub)
    classPicker.vm.$emit('update:modelValue', [11, 12])
    await nextTick()

    const headers = wrapper.findAll('th').map((th) => th.text())
    expect(headers).toEqual(['Lớp', 'Toán', 'Văn'])

    const rows = wrapper.findAll('tbody tr')
    expect(rows).toHaveLength(2)

    // Row 1: 10A1
    expect(rows[0]!.find('td').text()).toBe('10A1')
    // Row 2: 10A2 has no Văn, so should render dash
    expect(rows[1]!.text()).toContain('—')

    const inputNumbers = wrapper.findAllComponents(InputNumberStub)
    expect(inputNumbers).toHaveLength(3)
  })
})
