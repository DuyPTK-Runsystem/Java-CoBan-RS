import type { Meta, StoryObj } from '@storybook/vue3'

import TranscriptAnnualTable from './TranscriptAnnualTable.vue'
import type { ResAnnualSubjectResultDTO } from '@/types/transcript'

const sampleAnnualSubjects: ResAnnualSubjectResultDTO[] = [
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

const meta = {
  title: 'Transcript/TranscriptAnnualTable',
  component: TranscriptAnnualTable,
  tags: ['autodocs'],
  args: {
    subjects: sampleAnnualSubjects,
    regularDtbcn: 5.8,
    finalDtbcn: 6.6,
  },
  parameters: { layout: 'padded' },
} satisfies Meta<typeof TranscriptAnnualTable>

export default meta
type Story = StoryObj<typeof meta>

export const WithRetake: Story = {}
export const Empty: Story = { args: { subjects: [], regularDtbcn: null, finalDtbcn: null } }
