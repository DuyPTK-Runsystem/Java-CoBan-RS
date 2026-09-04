import type { Meta, StoryObj } from '@storybook/vue3'

import ClassSummaryTranscriptTable from './ClassSummaryTranscriptTable.vue'
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
      dtbhk: 8.2,
      subjects: [
        { subjectId: 1, subjectName: 'Toán', subjectType: 'ACADEMIC', dtbmh: 8.5, skillScore: null, calculatedVersion: 1, calculatedAt: null, assessmentColumns: [] },
        { subjectId: 2, subjectName: 'Vật lí', subjectType: 'ACADEMIC', dtbmh: 7.0, skillScore: null, calculatedVersion: 1, calculatedAt: null, assessmentColumns: [] },
        { subjectId: 3, subjectName: 'Hóa học', subjectType: 'ACADEMIC', dtbmh: 8.0, skillScore: null, calculatedVersion: 1, calculatedAt: null, assessmentColumns: [] },
        { subjectId: 4, subjectName: 'Sinh học', subjectType: 'ACADEMIC', dtbmh: 9.0, skillScore: null, calculatedVersion: 1, calculatedAt: null, assessmentColumns: [] },
      ],
    },
    {
      studentId: 102,
      studentCode: 'HS002',
      fullName: 'Lê Thị B',
      calculationStatus: 'FINISH',
      dtbhk: 5.0,
      subjects: [
        { subjectId: 1, subjectName: 'Toán', subjectType: 'ACADEMIC', dtbmh: 7.0, skillScore: null, calculatedVersion: 1, calculatedAt: null, assessmentColumns: [] },
        { subjectId: 2, subjectName: 'Vật lí', subjectType: 'ACADEMIC', dtbmh: 3.0, skillScore: null, calculatedVersion: 1, calculatedAt: null, assessmentColumns: [] },
        { subjectId: 3, subjectName: 'Hóa học', subjectType: 'ACADEMIC', dtbmh: 5.0, skillScore: null, calculatedVersion: 1, calculatedAt: null, assessmentColumns: [] },
        { subjectId: 4, subjectName: 'Sinh học', subjectType: 'ACADEMIC', dtbmh: 5.0, skillScore: null, calculatedVersion: 1, calculatedAt: null, assessmentColumns: [] },
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
        { subjectId: 1, subjectName: 'Toán', subjectType: 'ACADEMIC', hk1: 8.0, hk2: 8.5, regularDtbmhCn: 8.3, officialDtbmhCn: 8.3, calculationSource: 'REGULAR', calculatedVersion: 1, calculatedAt: null, retake: null },
        { subjectId: 2, subjectName: 'Vật lí', subjectType: 'ACADEMIC', hk1: 7.0, hk2: 7.0, regularDtbmhCn: 7.0, officialDtbmhCn: 7.0, calculationSource: 'REGULAR', calculatedVersion: 1, calculatedAt: null, retake: null },
        { subjectId: 3, subjectName: 'Hóa học', subjectType: 'ACADEMIC', hk1: 8.0, hk2: 8.0, regularDtbmhCn: 8.0, officialDtbmhCn: 8.0, calculationSource: 'REGULAR', calculatedVersion: 1, calculatedAt: null, retake: null },
        { subjectId: 4, subjectName: 'Sinh học', subjectType: 'ACADEMIC', hk1: 9.0, hk2: 9.0, regularDtbmhCn: 9.0, officialDtbmhCn: 9.0, calculationSource: 'REGULAR', calculatedVersion: 1, calculatedAt: null, retake: null },
      ],
    },
    {
      studentId: 102,
      studentCode: 'HS002',
      fullName: 'Lê Thị B',
      calculationStatus: 'FINISH',
      regularDtbcn: 4.8,
      finalDtbcn: 6.2,
      resultSource: 'RETAKE',
      subjects: [
        { subjectId: 1, subjectName: 'Toán', subjectType: 'ACADEMIC', hk1: 7.0, hk2: 7.0, regularDtbmhCn: 7.0, officialDtbmhCn: 7.0, calculationSource: 'REGULAR', calculatedVersion: 1, calculatedAt: null, retake: null },
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
          calculatedAt: null,
          retake: {
            retakeId: 99,
            preRetakeScore: 2.8,
            retakeScore: 5.5,
            examDate: '2026-08-15',
            status: 'SCORED',
            note: 'Đạt',
          },
        },
        { subjectId: 3, subjectName: 'Hóa học', subjectType: 'ACADEMIC', hk1: 6.5, hk2: 6.5, regularDtbmhCn: 6.5, officialDtbmhCn: 6.5, calculationSource: 'REGULAR', calculatedVersion: 1, calculatedAt: null, retake: null },
        { subjectId: 4, subjectName: 'Sinh học', subjectType: 'ACADEMIC', hk1: 6.0, hk2: 6.0, regularDtbmhCn: 6.0, officialDtbmhCn: 6.0, calculationSource: 'REGULAR', calculatedVersion: 1, calculatedAt: null, retake: null },
      ],
    },
  ],
}

const meta = {
  title: 'Transcript/ClassSummaryTranscriptTable',
  component: ClassSummaryTranscriptTable,
  tags: ['autodocs'],
  parameters: { layout: 'padded' },
} satisfies Meta<typeof ClassSummaryTranscriptTable>

export default meta
type Story = StoryObj<typeof meta>

export const Mode2ATerm: Story = {
  args: {
    mode: 'TERM',
    title: 'Bảng điểm học kì 1 - Năm học 2026-2027 (Mode 2A)',
    termData: sampleTermData,
  },
}

export const Mode2BAnnualWithInlineRetake: Story = {
  args: {
    mode: 'ANNUAL',
    title: 'Bảng điểm cả năm - Năm học 2026-2027 (Mode 2B có inline thi lại)',
    annualData: sampleAnnualData,
  },
}

