import type { Meta, StoryObj } from '@storybook/vue3'

import ClassSubjectTranscriptTable from './ClassSubjectTranscriptTable.vue'
import type {
  ResClassAnnualTranscriptDTO,
  ResClassTermTranscriptDTO,
} from '@/types/classTranscript'

const sampleTermData: ResClassTermTranscriptDTO = {
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
          dtbmh: 8.5,
          skillScore: null,
          calculatedVersion: 1,
          calculatedAt: '2026-09-04T08:00:00',
          assessmentColumns: [
            { columnId: 1, assessmentType: 'KTTX', columnNo: 1, columnName: 'TX1', scoreStatus: 'SCORED', scoreValue: 8.0 },
            { columnId: 2, assessmentType: 'KTTX', columnNo: 2, columnName: 'TX2', scoreStatus: 'SCORED', scoreValue: 9.0 },
            { columnId: 3, assessmentType: 'KTTX', columnNo: 3, columnName: 'TX3', scoreStatus: 'SCORED', scoreValue: 8.5 },
            { columnId: 4, assessmentType: 'KTDK', columnNo: 1, columnName: 'GK', scoreStatus: 'SCORED', scoreValue: 8.0 },
            { columnId: 5, assessmentType: 'KTCK', columnNo: 1, columnName: 'CK', scoreStatus: 'SCORED', scoreValue: 9.0 },
          ],
        },
      ],
    },
    {
      studentId: 102,
      studentCode: 'HS002',
      fullName: 'Lê Thị B',
      calculationStatus: 'FINISH',
      dtbhk: 6.5,
      subjects: [
        {
          subjectId: 1,
          subjectName: 'Toán học',
          subjectType: 'ACADEMIC',
          dtbmh: 6.2,
          skillScore: null,
          calculatedVersion: 1,
          calculatedAt: '2026-09-04T08:00:00',
          assessmentColumns: [
            { columnId: 6, assessmentType: 'KTTX', columnNo: 1, columnName: 'TX1', scoreStatus: 'SCORED', scoreValue: 6.0 },
            { columnId: 7, assessmentType: 'KTTX', columnNo: 2, columnName: 'TX2', scoreStatus: 'SCORED', scoreValue: 7.0 },
            { columnId: 8, assessmentType: 'KTDK', columnNo: 1, columnName: 'GK', scoreStatus: 'SCORED', scoreValue: 6.0 },
            { columnId: 9, assessmentType: 'KTCK', columnNo: 1, columnName: 'CK', scoreStatus: 'SCORED', scoreValue: 6.0 },
          ],
        },
      ],
    },
  ],
}

const sampleAnnualData: ResClassAnnualTranscriptDTO = {
  classId: 1,
  classCode: '10A1',
  className: 'Lớp 10A1',
  academicYearId: 2026,
  students: [
    {
      studentId: 101,
      studentCode: 'HS001',
      fullName: 'Nguyễn Văn A',
      calculationStatus: 'FINISH',
      regularDtbcn: 8.3,
      finalDtbcn: 8.3,
      resultSource: 'REGULAR',
      subjects: [
        {
          subjectId: 1,
          subjectName: 'Toán học',
          subjectType: 'ACADEMIC',
          hk1: 8.0,
          hk2: 8.5,
          regularDtbmhCn: 8.3,
          officialDtbmhCn: 8.3,
          calculationSource: 'REGULAR',
          calculatedVersion: 1,
          calculatedAt: '2026-09-04T08:00:00',
          retake: null,
        },
      ],
    },
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
            retakeId: 77,
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

const meta = {
  title: 'Transcript/ClassSubjectTranscriptTable',
  component: ClassSubjectTranscriptTable,
  tags: ['autodocs'],
  parameters: { layout: 'padded' },
} satisfies Meta<typeof ClassSubjectTranscriptTable>

export default meta
type Story = StoryObj<typeof meta>

export const Mode1ATerm: Story = {
  args: {
    mode: 'TERM',
    subjectId: 1,
    subjectName: 'Toán học',
    title: 'Bảng điểm học kì 1 - Năm học 2026-2027 - Môn Toán (Mode 1A)',
    termData: sampleTermData,
  },
}

export const Mode1BAnnual: Story = {
  args: {
    mode: 'ANNUAL',
    subjectId: 1,
    subjectName: 'Toán học',
    title: 'Bảng điểm cả năm - Năm học 2026-2027 - Môn Toán (Mode 1B)',
    annualData: sampleAnnualData,
  },
}

