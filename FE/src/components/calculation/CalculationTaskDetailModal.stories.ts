import type { Meta, StoryObj } from '@storybook/vue3'

import CalculationTaskDetailModal from './CalculationTaskDetailModal.vue'
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
  availableAt: '2026-09-03T09:15:12',
  lockedAt: null,
  workerId: 'worker-1',
  lastError: 'Timeout khi đọc transcript HK2',
  createdAt: '2026-09-03T09:12:44',
  startedAt: '2026-09-03T09:12:45',
  completedAt: '2026-09-03T09:15:12',
}

const meta = {
  title: 'Operations/CalculationTaskDetailModal',
  component: CalculationTaskDetailModal,
  tags: ['autodocs'],
  args: {
    visible: true,
    task: sampleTask,
    canRetry: true,
    retrying: false,
  },
} satisfies Meta<typeof CalculationTaskDetailModal>

export default meta
type Story = StoryObj<typeof meta>

export const FailedTask: Story = {}

export const SucceededTask: Story = {
  args: {
    task: {
      ...sampleTask,
      taskId: 1044,
      status: 'SUCCEEDED',
      attemptCount: 1,
      lastError: null,
    },
  },
}

export const RetryingState: Story = {
  args: {
    retrying: true,
  },
}
