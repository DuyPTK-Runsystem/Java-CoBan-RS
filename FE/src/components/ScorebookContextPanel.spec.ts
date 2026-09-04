import { shallowMount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ScorebookContextPanel from './ScorebookContextPanel.vue'

describe('ScorebookContextPanel', () => {
  it('builds human-facing class and subject labels', () => {
    const wrapper = shallowMount(ScorebookContextPanel, {
      props: {
        academicYears: [],
        semesters: [],
        classes: [{
          id: 3,
          academicYearId: 1,
          gradeLevelId: 6,
          classCode: '6A1',
          className: 'Lớp 6A1',
          capacity: 35,
          status: 'ACTIVE',
        }],
        classSubjects: [{ id: 20, classId: 3, subjectId: 9, semesterId: 2, status: 'ACTIVE' }],
        subjects: [{
          id: 9,
          code: 'TOAN',
          name: 'Toán',
          subjectType: 'ACADEMIC',
          applicationScope: 'GRADE',
          status: 'ACTIVE',
        }],
        academicYearId: 1,
        semesterId: 2,
        classId: 3,
        classSubjectId: 20,
      },
    })
    const view = wrapper.vm as unknown as {
      classOptions: Array<{ label: string }>
      classSubjectOptions: Array<{ label: string }>
    }

    expect(view.classOptions[0].label).toBe('6A1 · Lớp 6A1')
    expect(view.classSubjectOptions[0].label).toBe('TOAN · Toán')
  })
})

