import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import TeacherLoadPanel from './TeacherLoadPanel.vue'

describe('TeacherLoadPanel', () => {
  it('renders NORMAL as the blue Vietnamese target-status label', () => {
    const wrapper = mount(TeacherLoadPanel, {
      props: {
        teacherLoads: [{
          teacherId: 1,
          teacherName: 'Phạm Minh Quân',
          assignedPeriods: 15,
          basePeriods: 15,
          reductions: 0,
          targetPeriods: 15,
          difference: 0,
          evaluationStatus: 'NORMAL',
          reductionDetails: [],
        }],
      },
    })

    expect(wrapper.text()).toContain('ĐỦ ĐỊNH MỨC')
    expect(wrapper.text()).not.toContain('NORMAL')
    expect(wrapper.find('.p-tag-info').exists()).toBe(true)
  })
})
