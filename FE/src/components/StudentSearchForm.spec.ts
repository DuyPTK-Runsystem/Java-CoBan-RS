import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import StudentSearchForm from './StudentSearchForm.vue'
import { primeVueStubs } from '@/test/stubs'

describe('StudentSearchForm', () => {
  it('emits deterministic search criteria', async () => {
    const wrapper = mount(StudentSearchForm, {
      global: { stubs: primeVueStubs },
    })

    await wrapper.get('#search-student-code').setValue('STU0000001')
    await wrapper.get('#search-student-name').setValue('Nguyen Van A')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('search')).toEqual([[{
      studentCode: 'STU0000001',
      studentName: 'Nguyen Van A',
      dateOfBirth: null,
    }]])
  })
})
