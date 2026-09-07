import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ClassSummaryTranscriptTable from './ClassSummaryTranscriptTable.vue'
import type {
  ResClassAnnualTranscriptDTO,
  ResClassTermTranscriptDTO,
} from '@/types/classTranscript'

describe('ClassSummaryTranscriptTable.vue', () => {
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
        dtbhk: 8.2,
        subjects: [
          {
            subjectId: 1,
            subjectName: 'Toán học',
            subjectType: 'ACADEMIC',
            dtbmh: 8.5,
            skillScore: null,
            calculatedVersion: 1,
            calculatedAt: '2026-09-04T08:00:00',
            assessmentColumns: [],
          },
          {
            subjectId: 2,
            subjectName: 'Vật lí',
            subjectType: 'ACADEMIC',
            dtbmh: 7.9,
            skillScore: null,
            calculatedVersion: 1,
            calculatedAt: '2026-09-04T08:00:00',
            assessmentColumns: [],
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
        regularDtbcn: 4.8,
        finalDtbcn: 6.2,
        resultSource: 'RETAKE',
        subjects: [
          {
            subjectId: 1,
            subjectName: 'Toán học',
            subjectType: 'ACADEMIC',
            hk1: 7.0,
            hk2: 7.0,
            regularDtbmhCn: 7.0,
            officialDtbmhCn: 7.0,
            calculationSource: 'REGULAR',
            calculatedVersion: 1,
            calculatedAt: '2026-09-04T08:00:00',
            retake: null,
          },
          {
            subjectId: 2,
            subjectName: 'Vật lí',
            subjectType: 'ACADEMIC',
            hk1: 2.5,
            hk2: 3.1,
            regularDtbmhCn: 2.8,
            officialDtbmhCn: 5.5,
            calculationSource: 'RETAKE',
            calculatedVersion: 2,
            calculatedAt: '2026-09-04T08:00:00',
            retake: {
              retakeId: 88,
              preRetakeScore: 2.8,
              retakeScore: 5.5,
              examDate: '2026-08-15',
              status: 'SCORED',
              note: 'Đạt',
            },
          },
        ],
      },
    ],
  }

  it('renders Mode 2A (TERM) summary table with subject columns and TBHK', async () => {
    const wrapper = mount(ClassSummaryTranscriptTable, {
      props: {
        mode: 'TERM',
        termData: mockTermData,
      },
    })

    expect(wrapper.text()).toContain('Toán học')
    expect(wrapper.text()).toContain('Vật lí')
    expect(wrapper.text()).toContain('Nguyễn Văn A')
    expect(wrapper.text()).toContain('8.5') // Toán
    expect(wrapper.text()).toContain('7.9') // Vật lí
    expect(wrapper.text()).toContain('8.2') // TBHK

    const btn = wrapper.find('.student-link-btn')
    await btn.trigger('click')
    expect(wrapper.emitted('selectStudent')?.[0]).toEqual([101])
  })

  it('renders Mode 2B (ANNUAL) summary table with inline retake score format', async () => {
    const wrapper = mount(ClassSummaryTranscriptTable, {
      props: {
        mode: 'ANNUAL',
        annualData: mockAnnualData,
      },
    })

    expect(wrapper.text()).toContain('Lê Thị B')
    expect(wrapper.text()).toContain('7.0') // Toán
    // Vật lí has inline retake: 2.8 (Thi lại: 5.5)
    expect(wrapper.text()).toContain('2.8')
    expect(wrapper.text()).toContain('(Thi lại: 5.5)')
    expect(wrapper.text()).toContain('6.2') // TBCN
    expect(wrapper.text()).toContain('Lên lớp sau thi lại')

    const btn = wrapper.find('.student-link-btn')
    await btn.trigger('click')
    expect(wrapper.emitted('selectStudent')?.[0]).toEqual([102])
  })

  it('renders empty cell message when no students exist', () => {
    const wrapper = mount(ClassSummaryTranscriptTable, {
      props: {
        mode: 'TERM',
        termData: { ...mockTermData, students: [] },
      },
    })
    expect(wrapper.text()).toContain('Chưa có dữ liệu học sinh trong lớp')
  })
})

