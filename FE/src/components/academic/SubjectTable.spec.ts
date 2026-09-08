import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import SubjectTable from './SubjectTable.vue'

const subjects = [{ id: 1, code: 'MAT', name: 'Toán', subjectType: 'ACADEMIC' as const, applicationScope: 'GRADE' as const, status: 'ACTIVE' as const }]

describe('SubjectTable.vue', () => {
  it('shows subject data without mutation controls in read-only mode', () => {
    const wrapper = mount(SubjectTable, { props: { subjects, readOnly: true } })

    expect(wrapper.text()).toContain('Toán')
    expect(wrapper.find('button[aria-label="Sửa môn học"]').exists()).toBe(false)
    expect(wrapper.find('button[aria-label="Cấu hình phạm vi áp dụng"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('Chỉ xem')
  })
})
