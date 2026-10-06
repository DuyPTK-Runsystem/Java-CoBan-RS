import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import TimetableWeekGrid from './TimetableWeekGrid.vue'
import { ISO_WEEKDAYS, SCHOOL_WEEKDAYS, getIsoWeekdayLabel } from '@/utils/isoWeekday'

const ButtonStub = defineComponent({
  name: 'PrimeVueButtonStub',
  props: { ariaLabel: { type: String, default: '' } },
  template: '<button :aria-label="ariaLabel"><slot /></button>',
})

describe('ISO weekday mapping', () => {
  it('labels Monday as 1, Saturday as 6, and Sunday as 7', () => {
    expect(ISO_WEEKDAYS.map(({ value, label }) => [value, label])).toEqual([
      [1, 'Thứ Hai'],
      [2, 'Thứ Ba'],
      [3, 'Thứ Tư'],
      [4, 'Thứ Năm'],
      [5, 'Thứ Sáu'],
      [6, 'Thứ Bảy'],
      [7, 'Chủ Nhật'],
    ])
    expect(SCHOOL_WEEKDAYS.map(({ value }) => value)).toEqual([1, 2, 3, 4, 5, 6])
    expect(getIsoWeekdayLabel(7)).toBe('Chủ Nhật')
    expect(getIsoWeekdayLabel(0)).toBe('Không xác định')
    expect(getIsoWeekdayLabel(8)).toBe('Không xác định')
  })

  it('renders a Monday entry under Monday and excludes Sunday from the six-day school grid', () => {
    const mondayPeriod = { id: 101, dayOfWeek: 1, session: 'MORNING' as const, periodIndex: 1, periodName: 'Tiết 1', startTime: '07:00', endTime: '07:45' }
    const mondayEntry = {
      id: 201,
      entryId: 201,
      revisionId: 1,
      assignmentId: 1,
      periodId: mondayPeriod.id,
      classId: 1,
      className: '6A1',
      subjectId: 1,
      subjectName: 'Toán thứ Hai',
      teacherId: 1,
      teacherName: 'Cô Lan',
      dayOfWeek: 1,
      session: 'MORNING' as const,
      periodIndex: 1,
      validFrom: '2026-10-05',
      validTo: '2026-12-31',
    }
    const wrapper = mount(TimetableWeekGrid, {
      props: { periods: [mondayPeriod], entries: [mondayEntry] },
      global: { stubs: { Button: ButtonStub } },
    })
    const headers = wrapper.findAll('thead th').map((header) => header.text())
    const mondayCell = wrapper.find('[aria-label="Thứ Hai, buổi sáng, tiết 1: Toán thứ Hai"]')

    expect(headers).toEqual(['Buổi / Tiết', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'])
    expect(mondayCell.exists()).toBe(true)
    expect(mondayCell.text()).toContain('Toán thứ Hai')
    expect(wrapper.find('[aria-label="Thứ Ba, buổi sáng, tiết 1: Toán thứ Hai"]').exists()).toBe(false)
    expect(headers).not.toContain('Chủ Nhật')
  })
})
