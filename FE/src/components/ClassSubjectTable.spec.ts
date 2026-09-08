import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ClassSubjectTable from './ClassSubjectTable.vue'

describe('ClassSubjectTable.vue', () => {
  it('shows class-subject data without status controls in read-only mode', () => {
    const wrapper = mount(ClassSubjectTable, {
      props: {
        classSubjects: [{ id: 1, classId: 2, subjectId: 3, semesterId: 4, status: 'ACTIVE' }],
        subjects: [{ id: 3, code: 'MAT', name: 'Toán', subjectType: 'ACADEMIC', applicationScope: 'GRADE', status: 'ACTIVE' }],
        semesters: [{ id: 4, code: 'HK1', name: 'Học kỳ 1', status: 'OPEN' }],
        readOnly: true,
      },
    })

    expect(wrapper.text()).toContain('Toán')
    expect(wrapper.find('button[aria-label="Đổi trạng thái lớp-môn"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('Chỉ xem')
  })
})
