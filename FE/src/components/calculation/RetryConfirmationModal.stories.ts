import type { Meta, StoryObj } from '@storybook/vue3'

import RetryConfirmationModal from './RetryConfirmationModal.vue'
import type { ResCalculationTaskDTO } from '@/types/calculationTask'

const sampleTask: ResCalculationTaskDTO = {
  taskId: 1048,
  studentId: 101,
  studentCode: 'HS0001',
  academicYearId: 2,
  taskType: 'STUDENT_YEAR_RECALC',
  requestedVersion: 11,
  status: 'FAILED',
  attemptCount: 3,
  maxAttempts: 3,
  availableAt: null,
  lockedAt: null,
  workerId: null,
  lastError: 'Timeout khi đọc transcript HK2',
  createdAt: '2026-09-03T09:12:44',
  startedAt: null,
  completedAt: null,
}

const meta = {
  title: 'Operations/RetryConfirmationModal',
  component: RetryConfirmationModal,
  tags: ['autodocs'],
  args: {
    visible: true,
    mode: 'single',
    task: sampleTask,
    failedCount: 4,
    loading: false,
  },
} satisfies Meta<typeof RetryConfirmationModal>

export default meta
type Story = StoryObj<typeof meta>

export const SingleTaskRetry: Story = {}

export const BulkRetry: Story = {
  args: {
    mode: 'bulk',
    failedCount: 4,
  },
}

export const LoadingState: Story = {
  args: {
    loading: true,
  },
}
