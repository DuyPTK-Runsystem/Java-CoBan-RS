import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import NotificationAudienceSelector from './NotificationAudienceSelector.vue'

describe('NotificationAudienceSelector', () => {
  it('renders dropdown and displays school notice when audience is SCHOOL', () => {
    const wrapper = mount(NotificationAudienceSelector, {
      props: {
        audienceType: 'SCHOOL',
        targetReference: '',
      },
      global: {
        stubs: {
          Dropdown: { props: ['modelValue', 'options'], template: '<select><option v-for="o in options" :key="o.value" :value="o.value">{{ o.label }}</option></select>' },
          InputText: true,
        },
      },
    })
    expect(wrapper.text()).toContain('Toàn trường')
  })

  it('renders class input field when audience is CLASS', () => {
    const wrapper = mount(NotificationAudienceSelector, {
      props: {
        audienceType: 'CLASS',
        targetReference: '10A1',
      },
      global: {
        stubs: {
          Dropdown: true,
          InputText: { props: ['modelValue'], template: '<input :value="modelValue" />' },
        },
      },
    })
    expect(wrapper.text()).toContain('Mã lớp học')
  })

  it('renders individual input field when audience is INDIVIDUAL', () => {
    const wrapper = mount(NotificationAudienceSelector, {
      props: {
        audienceType: 'INDIVIDUAL',
        targetReference: '1, 2, 3',
      },
      global: {
        stubs: {
          Dropdown: true,
          InputText: { props: ['modelValue'], template: '<input :value="modelValue" />' },
        },
      },
    })
    expect(wrapper.text()).toContain('Danh sách User ID')
  })
})

