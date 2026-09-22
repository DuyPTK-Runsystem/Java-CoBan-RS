import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import type { ValidationError } from '@/types/api'
import FormAlert from './FormAlert.vue'

describe('FormAlert', () => {
  it('renders global and field validation messages with the selected tone', () => {
    const validationErrors: ValidationError[] = [{ field: 'name', messages: ['Name is required.', 'Name is too short.'] }]
    const wrapper = mount(FormAlert, {
      props: { tone: 'warning', message: 'Review the request.', messages: ['A general warning.'], validationErrors },
    })

    expect(wrapper.attributes('role')).toBe('alert')
    expect(wrapper.classes()).toContain('form-alert-warning')
    expect(wrapper.text()).toContain('Review the request.')
    expect(wrapper.text()).toContain('A general warning.')
    expect(wrapper.text()).toContain('name: Name is required.')
    expect(wrapper.text()).toContain('name: Name is too short.')
  })

  it('renders success messages with the success tone and status role', () => {
    const wrapper = mount(FormAlert, {
      props: { tone: 'success', message: 'Saved successfully.' },
    })

    expect(wrapper.attributes('role')).toBe('status')
    expect(wrapper.classes()).toContain('form-alert-success')
    expect(wrapper.text()).toContain('Saved successfully.')
  })
})
