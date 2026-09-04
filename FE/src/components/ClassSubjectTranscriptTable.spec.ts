import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ClassSubjectTranscriptTable from './ClassSubjectTranscriptTable.vue'
import type {
  ResClassAnnualTranscriptDTO,
  ResClassTermTranscriptDTO,
} from '@/types/classTranscript'

describe('ClassSubjectTranscriptTable.vue', () => {
  const mockTermData: ResClassTermTranscriptDTO = {
    classId: 1,
    classCode: '10A1',
    className: 'Lớp 10A1',
    academicYearId: 2026,
    semesterId: 1,
    students: [
      {
        studentId: 101,
        studentCode: 'HS001',
        fullName: 'Nguyễn Văn A',
        calculationStatus: 'FINISH',
        dtbhk: 8.5,
        subjects: [
          {
            subjectId: 1,
            subjectName: 'Toán học',
            subjectType: 'ACADEMIC',
            dtbmh: 8.0,
            skillScore: null,
            calculatedVersion: 1,
            calculatedAt: '2026-09-04T08:00:00',
            assessmentColumns: [
              { columnId: 1, assessmentType: 'KTTX', columnNo: 1, columnName: 'TX1', scoreStatus: 'SCORED', scoreValue: 8.0 },
              { columnId: 2, assessmentType: 'KTTX', columnNo: 2, columnName: 'TX2', scoreStatus: 'SCORED', scoreValue: 9.0 },
              { columnId: 3, assessmentType: 'KTDK', columnNo: 1, columnName: 'GK', scoreStatus: 'SCORED', scoreValue: 7.5 },
              { columnId: 4, assessmentType: 'KTCK', columnNo: 1, columnName: 'CK', scoreStatus: 'SCORED', scoreValue: 8.5 },
            ],
          },
        ],
      },
    ],
  }

  const mockAnnualData: ResClassAnnualTranscriptDTO = {
    classId: 1,
    classCode: '10A1',
    className: 'Lớp 10A1',
    academicYearId: 2026,
    students: [
      {
        studentId: 102,
        studentCode: 'HS002',
        fullName: 'Lê Thị B',
        calculationStatus: 'FINISH',
        regularDtbcn: 4.3,
        finalDtbcn: 5.0,
        resultSource: 'RETAKE',
        subjects: [
          {
            subjectId: 1,
            subjectName: 'Toán học',
            subjectType: 'ACADEMIC',
            hk1: 4.0,
            hk2: 4.5,
            regularDtbmhCn: 4.3,
            officialDtbmhCn: 5.0,
            calculationSource: 'RETAKE',
            calculatedVersion: 2,
            calculatedAt: '2026-09-04T08:00:00',
            retake: {
              retakeId: 55,
              preRetakeScore: 4.3,
              retakeScore: 6.0,
              examDate: '2026-08-15',
              status: 'SCORED',
              note: 'Đạt sau thi lại',
            },
          },
        ],
      },
    ],
  }

  it('renders Mode 1A (TERM) table with dynamic columns and student names', async () => {
    const wrapper = mount(ClassSubjectTranscriptTable, {
      props: {
        mode: 'TERM',
        subjectId: 1,
        subjectName: 'Toán học',
        termData: mockTermData,
      },
    })

    expect(wrapper.text()).toContain('Nguyễn Văn A')
    expect(wrapper.text()).toContain('8.0') // KTTX 1
    expect(wrapper.text()).toContain('9.0') // KTTX 2
    expect(wrapper.text()).toContain('7.5') // KTDK 1
    expect(wrapper.text()).toContain('8.5') // KTCK

    const btn = wrapper.find('.student-link-btn')
    expect(btn.exists()).toBe(true)
    await btn.trigger('click')
    expect(wrapper.emitted('selectStudent')?.[0]).toEqual([101])
  })

  it('renders Mode 1B (ANNUAL) table with retake badge and official scores', async () => {
    const wrapper = mount(ClassSubjectTranscriptTable, {
      props: {
        mode: 'ANNUAL',
        subjectId: 1,
        subjectName: 'Toán học',
        annualData: mockAnnualData,
      },
    })

    expect(wrapper.text()).toContain('Lê Thị B')
    expect(wrapper.text()).toContain('4.0') // HK1
    expect(wrapper.text()).toContain('4.5') // HK2
    expect(wrapper.text()).toContain('6.0') // Điểm thi lại
    expect(wrapper.text()).toContain('5.0') // ĐTBCN chính thức
    expect(wrapper.text()).toContain('Đạt sau thi lại')

    const btn = wrapper.find('.student-link-btn')
    await btn.trigger('click')
    expect(wrapper.emitted('selectStudent')?.[0]).toEqual([102])
  })

  it('renders empty table row when no students are present', () => {
    const wrapper = mount(ClassSubjectTranscriptTable, {
      props: {
        mode: 'TERM',
        subjectId: 1,
        subjectName: 'Toán học',
        termData: { ...mockTermData, students: [] },
      },
    })
    expect(wrapper.text()).toContain('Chưa có dữ liệu học sinh trong lớp')
  })
})

