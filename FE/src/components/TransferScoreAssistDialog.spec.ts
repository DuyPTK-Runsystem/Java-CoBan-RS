import { shallowMount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import TransferScoreAssistDialog from './TransferScoreAssistDialog.vue'
import type { TransferScoreAssistSnapshot } from '@/types/enrollment'

const snapshot: TransferScoreAssistSnapshot = {
  enrollmentId: 701, studentId: 11, studentCode: 'HS011', studentName: 'An', academicYearId: 1, semesterId: 7,
  sourceClass: { id: 101, academicYearId: 1, classCode: '6A1', className: null }, targetClass: { id: 102, academicYearId: 1, classCode: '6A2', className: null }, hasExistingScores: true, warnings: [],
  subjects: [{ subjectId: 1, subjectCode: 'MATH', subjectName: 'Toán', sourceEvidence: [{ assessmentColumnId: 10, scorebookId: 501, assessmentType: 'KTTT', columnNo: 1, columnName: null, scoreStatus: 'SCORED', scoreValue: 0, note: null, version: 3 }], targetColumns: [{ assessmentColumnId: 20, scorebookId: 601, assessmentType: 'KTTT', columnNo: 1, columnName: null, status: 'ACTIVE', mappingKey: 'MATH/KTTT/1', suggestedSourceColumnId: 10, existingScoreStatus: null, existingScoreValue: null, existingNote: null, existingVersion: null }] }],
}

describe('TransferScoreAssistDialog', () => {
  it('renders source evidence when the backend reports existing scores', () => {
    const wrapper = shallowMount(TransferScoreAssistDialog, {
      props: { visible: true, snapshot, effectiveAt: '2026-09-07T09:00:00', reason: '' },
      global: { stubs: { Dialog: { template: '<div><slot /></div>' } } },
    })

    expect(wrapper.find('.transfer-score-table').exists()).toBe(true)
    expect(wrapper.find('.source-evidence').exists()).toBe(true)
    expect(wrapper.find('.transfer-score-empty').exists()).toBe(false)
  })

  it('copies a zero score only into draft and does not submit', () => {
    const wrapper = shallowMount(TransferScoreAssistDialog, { props: { visible: true, snapshot, effectiveAt: '2026-09-07T09:00:00', reason: '' } })
    const view = wrapper.vm as unknown as { drafts: Record<number, { scoreStatus: string | null; scoreValue: number | null }>; suggestionFor: (subjectId: number, target: { scoreStatus: string | null; scoreValue: number | null; note: string }) => void }
    view.suggestionFor(1, view.drafts[20] as never)
    expect(view.drafts[20].scoreStatus).toBe('SCORED')
    expect(view.drafts[20].scoreValue).toBe(0)
    expect(wrapper.emitted('confirm')).toBeUndefined()
  })

  it('keeps empty distinct from zero and validates scored values', () => {
    const wrapper = shallowMount(TransferScoreAssistDialog, { props: { visible: true, snapshot, effectiveAt: '2026-09-07T09:00:00', reason: '' } })
    const view = wrapper.vm as unknown as { drafts: Record<number, { scoreStatus: 'SCORED' | null; scoreValue: number | null; note: string }>; confirm: () => void; validationMessage: string }
    expect(view.drafts[20].scoreStatus).toBeNull()
    view.drafts[20].scoreStatus = 'SCORED'
    view.drafts[20].scoreValue = 11
    view.confirm()
    expect(view.validationMessage).toContain('0 đến 10')
    expect(wrapper.emitted('confirm')).toBeUndefined()
  })
})
