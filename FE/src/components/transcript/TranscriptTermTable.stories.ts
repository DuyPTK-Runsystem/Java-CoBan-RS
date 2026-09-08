import type { Meta, StoryObj } from '@storybook/vue3'

import TranscriptTermTable from './TranscriptTermTable.vue'
import type { ResTermSubjectResultDTO } from '@/types/transcript'

const sampleSubjects: ResTermSubjectResultDTO[] = [
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

const meta = {
  title: 'Transcript/TranscriptTermTable',
  component: TranscriptTermTable,
  tags: ['autodocs'],
  args: {
    subjects: sampleSubjects,
    dtbhk: 7.8,
    excusedAbsences: 3,
    unexcusedAbsences: 1,
  },
  parameters: { layout: 'padded' },
} satisfies Meta<typeof TranscriptTermTable>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}
export const Empty: Story = { args: { subjects: [], dtbhk: null, excusedAbsences: null, unexcusedAbsences: null } }
