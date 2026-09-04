import type { Meta, StoryObj } from '@storybook/vue3'

import RetakeResultTable from './RetakeResultTable.vue'
import type { RetakeRowItem } from '@/types/retake'

const sampleItems: RetakeRowItem[] = [
  {
    retakeId: 7001,
    studentId: 1001,
    studentCode: 'HS0001',
    studentName: 'Nguyễn Minh An',
    academicYearId: 1,
    academicYearCode: '2026–2027',
    subjectId: 21,
    subjectName: 'Toán',
    preRetakeScore: 4.0,
    retakeScore: 6.5,
    officialDtbmhCn: 6.5,
    examDate: '2027-06-15',
    status: 'SCORED',
    calculationStatus: 'FINISH',
    calculationSource: 'RETAKE',
    lastTaskId: 8801,
    note: 'Điểm đã đối chiếu biên bản.',
  },
  {
    retakeId: 7002,
    studentId: 1002,
    studentCode: 'HS0002',
    studentName: 'Trần Gia Bảo',
    academicYearId: 1,
    academicYearCode: '2026–2027',
    subjectId: 22,
    subjectName: 'Ngữ văn',
    preRetakeScore: 4.5,
    retakeScore: null,
    officialDtbmhCn: null,
    examDate: '2027-06-15',
    status: 'PLANNED',
    calculationStatus: null,
    calculationSource: null,
    lastTaskId: null,
    note: null,
  },
  {
    retakeId: 7003,
    studentId: 1003,
    studentCode: 'HS0003',
    studentName: 'Lê Hoàng Chi',
    academicYearId: 1,
    academicYearCode: '2026–2027',
    subjectId: 23,
    subjectName: 'Vật lý',
    preRetakeScore: 3.8,
    retakeScore: 5.0,
    officialDtbmhCn: 5.0,
    examDate: '2027-06-15',
    status: 'SCORED',
    calculationStatus: 'IN_PROGRESS',
    calculationSource: 'RETAKE',
    lastTaskId: null,
    note: null,
  },
  {
    retakeId: 7004,
    studentId: 1004,
    studentCode: 'HS0004',
    studentName: 'Phạm Anh Dũng',
    academicYearId: 1,
    academicYearCode: '2026–2027',
    subjectId: 24,
    subjectName: 'Hóa học',
    preRetakeScore: 4.0,
    retakeScore: null,
    officialDtbmhCn: null,
    examDate: null,
    status: 'CANCELLED',
    calculationStatus: null,
    calculationSource: null,
    lastTaskId: null,
    note: null,
  },
]

const meta = {
  title: 'Scorebook/RetakeResultTable',
  component: RetakeResultTable,
  tags: ['autodocs'],
  args: {
    items: sampleItems,
    loading: false,
  },
} satisfies Meta<typeof RetakeResultTable>

export default meta
type Story = StoryObj<typeof meta>

export const Normal: Story = {}

export const Empty: Story = {
  args: {
    items: [],
  },
}

export const CalculationInProgress: Story = {
  args: {
    items: [
      {
        ...sampleItems[2],
        calculationStatus: 'IN_PROGRESS',
      },
    ],
  },
}

export const AllScored: Story = {
  args: {
    items: [sampleItems[0]],
  },
}

export const AllPlanned: Story = {
  args: {
    items: [sampleItems[1]],
  },
}

export const AllCancelled: Story = {
  args: {
    items: [sampleItems[3]],
  },
}
