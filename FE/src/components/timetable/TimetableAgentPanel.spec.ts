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
      { id: 2, label: '10A1 · Toán · Sáng tiết 2 · 2026-11-01 → 2026-11-30' },
    ])
  })
})
