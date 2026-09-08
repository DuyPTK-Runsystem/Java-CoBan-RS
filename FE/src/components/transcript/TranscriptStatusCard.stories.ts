import type { Meta, StoryObj } from '@storybook/vue3'

import TranscriptStatusCard from './TranscriptStatusCard.vue'
import type { ResTranscriptCalculationStatusDTO } from '@/types/transcript'

const sampleStatus: ResTranscriptCalculationStatusDTO = {
  studentId: 101,
  studentCode: 'HS0001',
  academicYearId: 2,
  semesterId: null,
  calculationStatus: 'IN_PROGRESS',
  sourceVersion: 12,
  calculatedVersion: 11,
  isUpToDate: false,
  calculatedAt: '2026-09-03T08:45:00',
  lastError: null,
}

const meta = {
  title: 'Operations/TranscriptStatusCard',
  component: TranscriptStatusCard,
  tags: ['autodocs'],
  args: {
    status: sampleStatus,
    studentName: 'Nguyễn Minh An',
    loading: false,
  },
} satisfies Meta<typeof TranscriptStatusCard>

export default meta
type Story = StoryObj<typeof meta>

export const InProgress: Story = {}

export const FinishedUpToDate: Story = {
  args: {
    status: {
      ...sampleStatus,
      calculationStatus: 'FINISH',
      calculatedVersion: 12,
      isUpToDate: true,
      calculatedAt: '2026-09-03T09:00:00',
    },
    studentName: 'Trần Gia Bảo',
  },
}

export const FinishedOutOfDateWithError: Story = {
  args: {
    status: {
      ...sampleStatus,
      calculationStatus: 'FINISH',
      isUpToDate: false,
      lastError: 'Điểm hệ số môn Văn chưa hợp lệ',
    },
  },
}
