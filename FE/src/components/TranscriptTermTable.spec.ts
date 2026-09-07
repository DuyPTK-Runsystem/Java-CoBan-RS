import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import TranscriptTermTable from './TranscriptTermTable.vue'
import type { ResTermSubjectResultDTO } from '@/types/transcript'

describe('TranscriptTermTable.vue', () => {
  const mockSubjects: ResTermSubjectResultDTO[] = [
    {
      subjectId: 1,
      subjectName: 'Toán học',
      subjectType: 'ACADEMIC',
      dtbmh: 8.5,
      skillScore: null,
      calculatedVersion: 1,
      calculatedAt: '2026-09-03T10:00:00',
      assessmentColumns: [
        { columnId: 101, assessmentType: 'KTTT', columnNo: 1, columnName: 'Miệng 1', scoreStatus: 'SCORED', scoreValue: 8.0 },
        { columnId: 102, assessmentType: 'KTTT', columnNo: 2, columnName: 'Miệng 2', scoreStatus: 'SCORED', scoreValue: 9.0 },
        { columnId: 103, assessmentType: 'KTTT', columnNo: 3, columnName: '15 phút', scoreStatus: 'SCORED', scoreValue: 8.5 },
        { columnId: 104, assessmentType: 'KTDK', columnNo: 1, columnName: 'Giữa kỳ', scoreStatus: 'SCORED', scoreValue: 8.0 },
        { columnId: 105, assessmentType: 'KTCK', columnNo: 1, columnName: 'Cuối kỳ', scoreStatus: 'SCORED', scoreValue: 9.0 },
      ],
    },
    {
      subjectId: 2,
      subjectName: 'Vật lí',
      subjectType: 'ACADEMIC',
      dtbmh: 7.2,
      skillScore: null,
      calculatedVersion: 1,
      calculatedAt: '2026-09-03T10:00:00',
      assessmentColumns: [
        { columnId: 201, assessmentType: 'KTTT', columnNo: 1, columnName: 'Miệng', scoreStatus: 'SCORED', scoreValue: 7.0 },
        { columnId: 202, assessmentType: 'KTDK', columnNo: 1, columnName: 'Giữa kỳ 1', scoreStatus: 'SCORED', scoreValue: 7.5 },
        { columnId: 203, assessmentType: 'KTDK', columnNo: 2, columnName: 'Giữa kỳ 2', scoreStatus: 'SCORED', scoreValue: 8.0 },
      ],
    },
    {
      subjectId: 3,
      subjectName: 'Giáo dục thể chất',
      subjectType: 'SKILL',
      dtbmh: null,
      skillScore: 8.0,
      calculatedVersion: 1,
      calculatedAt: '2026-09-03T10:00:00',
      assessmentColumns: [],
    },
  ]

  it('renders dynamic columns for maximum assessment counts across subjects', () => {
    const wrapper = mount(TranscriptTermTable, {
      props: {
        subjects: mockSubjects,
        dtbhk: 7.8,
        excusedAbsences: 2,
        unexcusedAbsences: 1,
      },
    })

    // maxKttx = 3 (from Math), maxKtdk = 2 (from Physics), maxKtck = 1 (from Math)
    const kttxGroupHeader = wrapper.find('.group-kttx')
    expect(kttxGroupHeader.attributes('colspan')).toBe('3')

    const ktdkGroupHeader = wrapper.find('.group-ktdk')
    expect(ktdkGroupHeader.attributes('colspan')).toBe('2')

    const ktckGroupHeader = wrapper.find('.group-ktck')
    expect(ktckGroupHeader.attributes('colspan')).toBe('1')

    // Table rows
    const rows = wrapper.findAll('.transcript-grid-table tbody tr')
    expect(rows.length).toBe(3)
    expect(wrapper.text()).toContain('Toán học')
    expect(wrapper.text()).toContain('Vật lí')
    expect(wrapper.text()).toContain('Giáo dục thể chất')
  })

  it('renders skill subject with merged evaluation cell and Đạt status', () => {
    const wrapper = mount(TranscriptTermTable, {
      props: {
        subjects: mockSubjects,
        dtbhk: 7.8,
      },
    })

    const skillCell = wrapper.find('.skill-evaluated')
    expect(skillCell.exists()).toBe(true)
    expect(skillCell.text()).toBe('Đạt')
    // total cols = 3 + 2 + 1 = 6
    expect(skillCell.attributes('colspan')).toBe('6')
  })

  it('displays the footer summary with dtbhk and absence stats', () => {
    const wrapper = mount(TranscriptTermTable, {
      props: {
        subjects: mockSubjects,
        dtbhk: 7.8,
        excusedAbsences: 4,
        unexcusedAbsences: 0,
      },
    })

    expect(wrapper.find('.summary-value.highlight').text()).toBe('7.8')
    expect(wrapper.text()).toContain('Số buổi vắng có phép:')
    expect(wrapper.text()).toContain('4')
    expect(wrapper.text()).toContain('Số buổi vắng không phép:')
    expect(wrapper.text()).toContain('0')
  })

  it('handles empty subject list gracefully', () => {
    const wrapper = mount(TranscriptTermTable, {
      props: {
        subjects: [],
        dtbhk: null,
      },
    })

    expect(wrapper.find('.empty-cell').text()).toContain('Chưa có dữ liệu môn học')
    expect(wrapper.find('.summary-value.highlight').text()).toBe('—')
  })

  it('correctly matches accented and unaccented assessment types (KTĐK/KTDK and KTTX/KTTT)', () => {
    const subjectsWithAccents: ResTermSubjectResultDTO[] = [
      {
        subjectId: 10,
        subjectName: 'Hóa học',
        subjectType: 'ACADEMIC',
        dtbmh: 8.8,
        skillScore: null,
        calculatedVersion: 1,
        calculatedAt: '2026-09-03T10:00:00',
        assessmentColumns: [
          { columnId: 301, assessmentType: 'KTTX', columnNo: 1, columnName: 'TX 1', scoreStatus: 'SCORED', scoreValue: 8.5 },
          { columnId: 302, assessmentType: 'KTĐK', columnNo: 1, columnName: 'Giữa kỳ', scoreStatus: 'SCORED', scoreValue: 9.0 },
          { columnId: 303, assessmentType: 'KTCK', columnNo: 1, columnName: 'Cuối kỳ', scoreStatus: 'SCORED', scoreValue: 8.8 },
        ],
      },
    ]

    const wrapper = mount(TranscriptTermTable, {
      props: {
        subjects: subjectsWithAccents,
        dtbhk: 8.8,
      },
    })

    const kttxGroupHeader = wrapper.find('.group-kttx')
    expect(kttxGroupHeader.attributes('colspan')).toBe('1')

    const ktdkGroupHeader = wrapper.find('.group-ktdk')
    expect(ktdkGroupHeader.attributes('colspan')).toBe('1')

    const scoreCells = wrapper.findAll('.cell-score')
    expect(scoreCells.length).toBe(3)
    expect(scoreCells[0].text()).toBe('8.5')
    expect(scoreCells[1].text()).toBe('9.0')
    expect(scoreCells[2].text()).toBe('8.8')
  })

  it('correctly maps scores to columnNo slots with gaps and sets empty slot to —', () => {
    const subjectsWithGaps: ResTermSubjectResultDTO[] = [
      {
        subjectId: 20,
        subjectName: 'Sinh học',
        subjectType: 'ACADEMIC',
        dtbmh: 8.0,
        skillScore: null,
        calculatedVersion: 1,
        calculatedAt: '2026-09-03T10:00:00',
        assessmentColumns: [
          { columnId: 401, assessmentType: 'KTTX', columnNo: 1, columnName: 'TX 1', scoreStatus: 'SCORED', scoreValue: 8.0 },
          { columnId: 403, assessmentType: 'KTTX', columnNo: 3, columnName: 'TX 3', scoreStatus: 'SCORED', scoreValue: 9.5 },
          { columnId: 404, assessmentType: 'KTĐK', columnNo: 2, columnName: 'Giữa kỳ 2', scoreStatus: 'SCORED', scoreValue: 7.5 },
          { columnId: 405, assessmentType: 'KTCK', columnNo: 1, columnName: 'Cuối kỳ', scoreStatus: 'SCORED', scoreValue: 8.0 },
        ],
      },
    ]

    const wrapper = mount(TranscriptTermTable, {
      props: {
        subjects: subjectsWithGaps,
        dtbhk: 8.0,
      },
    })

    // maxKttx = 3 (due to columnNo 3), maxKtdk = 2 (due to columnNo 2), maxKtck = 1
    const kttxGroupHeader = wrapper.find('.group-kttx')
    expect(kttxGroupHeader.attributes('colspan')).toBe('3')

    const ktdkGroupHeader = wrapper.find('.group-ktdk')
    expect(ktdkGroupHeader.attributes('colspan')).toBe('2')

    const scoreCells = wrapper.findAll('.cell-score')
    expect(scoreCells.length).toBe(6) // 3 (KTTX) + 2 (KTĐK) + 1 (KTCK)
    expect(scoreCells[0].text()).toBe('8.0') // KTTX col 1
    expect(scoreCells[1].text()).toBe('—')   // KTTX col 2 (missing/gap)
    expect(scoreCells[2].text()).toBe('9.5') // KTTX col 3
    expect(scoreCells[3].text()).toBe('—')   // KTĐK col 1 (missing/gap)
    expect(scoreCells[4].text()).toBe('7.5') // KTĐK col 2
    expect(scoreCells[5].text()).toBe('8.0') // KTCK col 1
  })
})

