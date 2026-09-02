import { shallowMount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import BulkScoreEntryDialog from './BulkScoreEntryDialog.vue'

const column = { columnId: 7, assessmentType: 'KTTT' as const, columnNo: 1, columnName: 'TX1' }
const students = [{
  studentId: 11,
  studentCode: 'HS001',
  studentName: 'An',
  scores: {
    '7': {
      scoreId: 1,
      assessmentColumnId: 7,
      studentId: 11,
      studentCode: 'HS001',
      studentName: 'An',
      scoreStatus: 'SCORED' as const,
      scoreValue: 5,
      note: null,
      enteredBy: null,
      enteredAt: null,
      updatedBy: null,
      updatedAt: null,
      version: 3,
    },
  },
}]

describe('BulkScoreEntryDialog', () => {
  it('initializes rows when mounted visible', () => {
    const wrapper = shallowMount(BulkScoreEntryDialog, {
      props: { visible: true, column, students },
    })
    const view = wrapper.vm as unknown as {
      rows: Array<{ studentId: number; scoreValue: number | null; expectedVersion: number | null }>
    }

    expect(view.rows).toEqual([
      expect.objectContaining({ studentId: 11, scoreValue: 5, expectedVersion: 3 }),
    ])
  })

  it('sends only changed rows with status and expectedVersion', async () => {
    const wrapper = shallowMount(BulkScoreEntryDialog, {
      props: { visible: false, column, students },
    })
    await wrapper.setProps({ visible: true })
    const view = wrapper.vm as unknown as {
      rows: Array<{ scoreStatus: string; scoreValue: number | null }>
      save: () => void
    }
    view.rows[0].scoreStatus = 'ABSENT'
    view.rows[0].scoreValue = 5
    view.save()

    expect(wrapper.emitted('save')?.[0]).toEqual([{
      items: [{
        studentId: 11,
        scoreStatus: 'ABSENT',
        scoreValue: null,
        note: null,
        expectedVersion: 3,
      }],
    }])
  })

  it('does not submit unchanged rows', async () => {
    const wrapper = shallowMount(BulkScoreEntryDialog, {
      props: { visible: false, column, students },
    })
    await wrapper.setProps({ visible: true })
    const view = wrapper.vm as unknown as { save: () => void; validationMessage: string }
    view.save()

    expect(wrapper.emitted('save')).toBeUndefined()
    expect(view.validationMessage).toContain('Chưa có thay đổi')
  })

  it('formats a bulk score with one decimal only after that input loses focus', () => {
    const wrapper = shallowMount(BulkScoreEntryDialog, {
      props: { visible: true, column, students },
    })
    const view = wrapper.vm as unknown as {
      focusedStudentId: number | null
      rows: Array<{ studentId: number }>
      minFractionDigits: (row: { studentId: number }) => number
    }

    view.focusedStudentId = 11
    expect(view.minFractionDigits(view.rows[0])).toBe(0)

    view.focusedStudentId = null
    expect(view.minFractionDigits(view.rows[0])).toBe(1)
  })
})
