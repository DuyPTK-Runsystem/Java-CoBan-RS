import { defineComponent, h, type PropType } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import TeacherUnavailabilityDialog from './TeacherUnavailabilityDialog.vue'

const PrimeVueStub = defineComponent({
  props: {
    modelValue: { type: null as unknown as PropType<unknown>, default: undefined },
    options: { type: Array as PropType<Array<Record<string, unknown>>>, default: () => [] },
    optionLabel: { type: String, default: 'label' },
    optionValue: { type: String, default: 'value' },
    label: { type: String, default: '' },
    value: { type: null as unknown as PropType<unknown>, default: undefined },
    loading: { type: Boolean, default: false },
  },
  emits: ['click', 'update:modelValue'],
  setup(props, { emit, slots }) {
    return () => {
      if (props.label) return h('button', { type: 'button', onClick: () => emit('click') }, props.label)
      const isWeekdaySelect = props.options.some((option) => option.label === 'Thứ Hai')
      return h(
        'div',
        {
          'data-testid': isWeekdaySelect ? 'weekday-select' : undefined,
          'data-model-value': isWeekdaySelect ? String(props.modelValue) : undefined,
        },
        isWeekdaySelect ? props.options.map((option) => h('span', String(option.label))) : [slots.default?.(), slots.footer?.()],
      )
    }
  },
})

describe('TeacherUnavailabilityDialog ISO weekday contract', () => {
  it('defaults weekly unavailability to labeled Monday and submits ISO weekday 1', async () => {
    const wrapper = mount(TeacherUnavailabilityDialog, {
      props: {
        visible: true,
        unavailability: null,
        semesters: [{ id: 7, academicYearId: 2026, name: 'HK1 2026 - 2027' }],
        teachers: [{ id: 12, teacherName: 'Cô Lan' }],
      },
      global: {
        stubs: {
          Dialog: PrimeVueStub,
          Select: PrimeVueStub,
          DatePicker: PrimeVueStub,
          Checkbox: PrimeVueStub,
          Button: PrimeVueStub,
          Textarea: PrimeVueStub,
        },
      },
    })

    const weekdaySelect = wrapper.get('[data-testid="weekday-select"]')
    expect(weekdaySelect.attributes('data-model-value')).toBe('1')
    expect(weekdaySelect.text()).toContain('Thứ Hai')
    expect(weekdaySelect.text()).toContain('Thứ Bảy')

    await wrapper.findAll('button').find((button) => button.text() === 'Gửi đăng ký')!.trigger('click')
    await flushPromises()

    expect(wrapper.emitted('save')?.[0]?.[0]).toMatchObject({
      isEdit: false,
      createPayload: { semesterId: 7, teacherId: 12, dayOfWeek: 1, specificDate: null },
    })
  })
})
