import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import SchoolClassTable from './SchoolClassTable.vue'
import type { ClassStatistic, GradeLevel, SchoolClass } from '@/types/academic'

describe('SchoolClassTable.vue', () => {
  const mockGrades: GradeLevel[] = [
    { id: 1, code: 'GRADE_6', name: 'Khối 6', gradeLevel: 6, displayOrder: 1, nextGradeId: 2, active: true, description: null },
  ]

  const mockClasses: SchoolClass[] = [
    { id: 101, academicYearId: 1, gradeLevelId: 1, classCode: '6A1', className: 'Lớp 6A1', capacity: 35, status: 'ACTIVE' },
    { id: 102, academicYearId: 1, gradeLevelId: 1, classCode: '6A2', className: 'Lớp 6A2', capacity: 40, status: 'PLANNED' },
    { id: 103, academicYearId: 1, gradeLevelId: 1, classCode: '6A3', className: 'Lớp 6A3', capacity: 35, status: 'CLOSED' },
  ]

  it('renders class list and fallback when statistics are unavailable', () => {
    const wrapper = mount(SchoolClassTable, {
      props: {
        schoolClasses: mockClasses,
        grades: mockGrades,
        classStatistics: {},
      },
    })

    expect(wrapper.text()).toContain('6A1')
    expect(wrapper.text()).toContain('Lớp 6A1')
    expect(wrapper.text()).toContain('Chưa có dữ liệu sĩ số')
    expect(wrapper.text()).toContain('Chưa có dữ liệu thống kê')
  })

  it('renders student count and capacity warnings when statistics are provided', () => {
    const stats: Record<number, ClassStatistic> = {
      101: {
        classId: 101,
        classCode: '6A1',
        className: 'Lớp 6A1',
        gradeLevelId: 1,
        capacity: 35,
        activeStudentCount: 30,
        gradeAverage: 35,
        warning: null,
      },
      102: {
        classId: 102,
        classCode: '6A2',
        className: 'Lớp 6A2',
        gradeLevelId: 1,
        capacity: 40,
        activeStudentCount: 45,
        gradeAverage: 35,
        warning: {
          classId: 102,
          academicYearId: 1,
          gradeLevelId: 1,
          activeStudentCount: 45,
          gradeAverage: 35,
          message: 'Lớp 6A2 lệch +29% so với trung bình khối (35 học sinh)',
        },
      },
    }

    const wrapper = mount(SchoolClassTable, {
      props: {
        schoolClasses: mockClasses,
        grades: mockGrades,
        classStatistics: stats,
      },
    })

    // Sĩ số hiển thị active / capacity
    expect(wrapper.text()).toContain('30 / 35')
    expect(wrapper.text()).toContain('45 / 40')

    // Warning hiển thị trên lớp 102
    expect(wrapper.text()).toContain('+29% · Cảnh báo')
  })

  it('emits edit when edit button is clicked', async () => {
    const wrapper = mount(SchoolClassTable, {
      props: {
        schoolClasses: mockClasses,
        grades: mockGrades,
      },
    })

    const editButtons = wrapper.findAll('button[aria-label="Sửa lớp"]')
    expect(editButtons.length).toBeGreaterThan(0)
    await editButtons[0]?.trigger('click')

    expect(wrapper.emitted('edit')).toBeTruthy()
    expect(wrapper.emitted('edit')?.[0]?.[0]).toEqual(mockClasses[0])
  })

  it('emits close when close button is clicked', async () => {
    const wrapper = mount(SchoolClassTable, {
      props: {
        schoolClasses: mockClasses,
        grades: mockGrades,
      },
    })

    const closeButtons = wrapper.findAll('button[aria-label="Đóng lớp"]')
    expect(closeButtons.length).toBeGreaterThan(0)
    await closeButtons[0]?.trigger('click')

    expect(wrapper.emitted('close')).toBeTruthy()
    expect(wrapper.emitted('close')?.[0]?.[0]).toEqual(mockClasses[0])
  })
})

