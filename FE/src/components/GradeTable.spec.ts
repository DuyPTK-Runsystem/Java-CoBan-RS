import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import GradeTable from './GradeTable.vue'
import type { GradeLevel, GradeStatistic } from '@/types/academic'

describe('GradeTable.vue', () => {
  const mockGrades: GradeLevel[] = [
    { id: 1, code: 'GRADE_6', name: 'Khối 6', gradeLevel: 6, displayOrder: 1, nextGradeId: 2, active: true, description: 'Đầu cấp' },
    { id: 2, code: 'GRADE_7', name: 'Khối 7', gradeLevel: 7, displayOrder: 2, nextGradeId: null, active: false, description: null },
  ]

  it('renders grade rows and fallback when statistics are not provided', () => {
    const wrapper = mount(GradeTable, {
      props: {
        grades: mockGrades,
        gradeStatistics: {},
      },
    })

    expect(wrapper.text()).toContain('Khối 6')
    expect(wrapper.text()).toContain('GRADE_6')
    expect(wrapper.text()).toContain('Khối 7')
    expect(wrapper.text()).toContain('Chưa có dữ liệu thống kê')
  })

  it('renders statistics correctly when gradeStatistics prop is provided', () => {
    const statistics: Record<number, GradeStatistic> = {
      1: { gradeLevelId: 1, activeClassCount: 5, activeStudentCount: 175 },
    }

    const wrapper = mount(GradeTable, {
      props: {
        grades: mockGrades,
        gradeStatistics: statistics,
      },
    })

    expect(wrapper.text()).toContain('5 lớp · 175 học sinh')
    // Khối 2 does not have statistics in the map
    expect(wrapper.text()).toContain('Chưa có dữ liệu thống kê')
  })

  it('emits edit when edit button is clicked', async () => {
    const wrapper = mount(GradeTable, {
      props: {
        grades: mockGrades,
      },
    })

    const editButtons = wrapper.findAll('button[aria-label="Sửa khối"]')
    expect(editButtons.length).toBeGreaterThan(0)
    await editButtons[0]?.trigger('click')

    expect(wrapper.emitted('edit')).toBeTruthy()
    expect(wrapper.emitted('edit')?.[0]?.[0]).toEqual(mockGrades[0])
  })

  it('emits toggleActive when toggle button is clicked', async () => {
    const wrapper = mount(GradeTable, {
      props: {
        grades: mockGrades,
      },
    })

    const toggleButtons = wrapper.findAll('button[aria-label="Ngừng dùng khối"]')
    expect(toggleButtons.length).toBeGreaterThan(0)
    await toggleButtons[0]?.trigger('click')

    expect(wrapper.emitted('toggleActive')).toBeTruthy()
    expect(wrapper.emitted('toggleActive')?.[0]?.[0]).toEqual(mockGrades[0])
  })
})

