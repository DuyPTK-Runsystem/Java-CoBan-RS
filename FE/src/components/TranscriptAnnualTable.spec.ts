import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import TranscriptAnnualTable from './TranscriptAnnualTable.vue'
import type { ResAnnualSubjectResultDTO } from '@/types/transcript'

describe('TranscriptAnnualTable.vue', () => {
  const mockAnnualSubjects: ResAnnualSubjectResultDTO[] = [
    {
      subjectId: 1,
      subjectName: 'Toán học',
      subjectType: 'ACADEMIC',
      hk1: 4.0,
      hk2: 4.5,
      regularDtbmhCn: 4.3,
      officialDtbmhCn: 6.0,
      calculationSource: 'RETAKE',
      calculatedVersion: 2,
      calculatedAt: '2026-09-03T10:00:00',
      retake: {
        retakeId: 501,
        preRetakeScore: 4.3,
        retakeScore: 6.0,
        examDate: '2026-08-20',
        status: 'SCORED',
        note: 'Đã hoàn thành thi lại',
      },
    },
    {
      subjectId: 2,
      subjectName: 'Vật lí',
      subjectType: 'ACADEMIC',
      hk1: 7.0,
      hk2: 7.5,
      regularDtbmhCn: 7.3,
      officialDtbmhCn: 7.3,
      calculationSource: 'REGULAR',
      calculatedVersion: 1,
      calculatedAt: '2026-09-03T10:00:00',
      retake: null,
    },
  ]

  it('renders annual scores and distinguishes retake subjects', () => {
    const wrapper = mount(TranscriptAnnualTable, {
      props: {
        subjects: mockAnnualSubjects,
        regularDtbcn: 5.8,
        finalDtbcn: 6.6,
      },
    })

    const rows = wrapper.findAll('.transcript-grid-table tbody tr')
    expect(rows.length).toBe(2)

    // First subject has retake
    const firstRow = rows[0]
    expect(firstRow.text()).toContain('Toán học')
    expect(firstRow.text()).toContain('RETAKE')
    expect(firstRow.text()).toContain('4.3')
    expect(firstRow.text()).toContain('6.0')

    // Second subject is regular (no REGULAR badge displayed)
    const secondRow = rows[1]
    expect(secondRow.text()).toContain('Vật lí')
    expect(secondRow.text()).not.toContain('REGULAR')
    expect(secondRow.text()).toContain('—')
  })

  it('renders footer summary with regular and final annual GPA', () => {
    const wrapper = mount(TranscriptAnnualTable, {
      props: {
        subjects: mockAnnualSubjects,
        regularDtbcn: 5.8,
        finalDtbcn: 6.6,
      },
    })

    expect(wrapper.text()).toContain('ĐTB cả năm ban đầu:')
    expect(wrapper.text()).toContain('5.8')
    expect(wrapper.text()).toContain('ĐTB cả năm chính thức:')
    expect(wrapper.find('.summary-value.highlight').text()).toBe('6.6')
  })

  it('handles empty annual subject list', () => {
    const wrapper = mount(TranscriptAnnualTable, {
      props: {
        subjects: [],
        regularDtbcn: null,
        finalDtbcn: null,
      },
    })

    expect(wrapper.find('.empty-cell').text()).toContain('Chưa có dữ liệu bảng điểm cả năm')
    expect(wrapper.find('.summary-value.highlight').text()).toBe('—')
  })
})
