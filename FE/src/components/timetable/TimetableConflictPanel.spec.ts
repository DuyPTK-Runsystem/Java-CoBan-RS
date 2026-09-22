import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import TimetableConflictPanel from './TimetableConflictPanel.vue'

describe('TimetableConflictPanel', () => {
  it('expands vertically for all issues and hides internal issue codes', () => {
    const wrapper = mount(TimetableConflictPanel, {
      props: {
        issues: [{
          code: 'LOAD_BELOW_TARGET',
          severity: 'WARNING',
          message: 'Giáo viên Phạm Minh Quân được xếp 2 tiết, thiếu so với định mức 15 tiết.',
          teacherName: 'Phạm Minh Quân',
        }],
        blockingCount: 0,
        warningCount: 1,
      },
    })

    expect(wrapper.text()).toContain('Cảnh báo')
    expect(wrapper.text()).toContain('Giáo viên Phạm Minh Quân')
    expect(wrapper.text()).not.toContain('LOAD_BELOW_TARGET')
    expect(wrapper.find('.timetable-conflict-panel').classes()).toContain('w-full')
    expect(wrapper.find('.timetable-conflict-issues').classes()).not.toContain('max-h-[480px]')
    expect(wrapper.find('.timetable-conflict-issues').classes()).not.toContain('overflow-y-auto')
  })
})
