import type { Meta, StoryObj } from '@storybook/vue3'

import CalculationTaskTable from './CalculationTaskTable.vue'
import type { ResCalculationTaskDTO } from '@/types/calculationTask'

const mockTasks: ResCalculationTaskDTO[] = [
  {
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
  },
  {
    taskId: 1047,
    studentId: 102,
    studentCode: 'HS0002',
    academicYearId: 2,
    taskType: 'STUDENT_YEAR_RECALC',
    requestedVersion: 8,
    status: 'FAILED',
    attemptCount: 2,
    maxAttempts: 3,
    availableAt: null,
    lockedAt: null,
    workerId: 'worker-2',
    lastError: 'Source version không khớp',
    createdAt: '2026-09-03T08:50:00',
    startedAt: '2026-09-03T08:50:10',
    completedAt: '2026-09-03T08:52:00',
  },
  {
    taskId: 1046,
    studentId: 103,
    studentCode: 'HS0003',
    academicYearId: 2,
    taskType: 'STUDENT_YEAR_RECALC',
    requestedVersion: 5,
    status: 'PENDING',
    attemptCount: 0,
    maxAttempts: 3,
    availableAt: null,
    lockedAt: null,
    workerId: null,
    lastError: null,
    createdAt: '2026-09-03T08:30:00',
    startedAt: null,
    completedAt: null,
  },
  {
    taskId: 1045,
    studentId: 104,
    studentCode: 'HS0004',
    academicYearId: 2,
    taskType: 'STUDENT_YEAR_RECALC',
    requestedVersion: 3,
    status: 'RUNNING',
    attemptCount: 1,
    maxAttempts: 3,
    availableAt: null,
    lockedAt: '2026-09-03T08:10:00',
    workerId: 'worker-1',
    lastError: null,
    createdAt: '2026-09-03T08:10:00',
    startedAt: '2026-09-03T08:10:02',
    completedAt: null,
  },
  {
    taskId: 1044,
    studentId: 105,
    studentCode: 'HS0005',
    academicYearId: 2,
    taskType: 'STUDENT_YEAR_RECALC',
    requestedVersion: 9,
    status: 'SUCCEEDED',
    attemptCount: 1,
    maxAttempts: 3,
    availableAt: null,
    lockedAt: null,
    workerId: 'worker-3',
    lastError: null,
    createdAt: '2026-09-03T08:00:00',
    startedAt: '2026-09-03T08:00:05',
    completedAt: '2026-09-03T08:01:20',
  },
]

const meta = {
  title: 'Operations/CalculationTaskTable',
  component: CalculationTaskTable,
  tags: ['autodocs'],
  args: {
    tasks: mockTasks,
    totalElements: 5,
    canRetry: true,
    loading: false,
    retryingTaskId: null,
    statusFilter: 'FAILED',
    studentCodeFilter: '',
  },
} satisfies Meta<typeof CalculationTaskTable>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const NoRetryPermission: Story = {
  args: {
    canRetry: false,
  },
}

export const Empty: Story = {
  args: {
    tasks: [],
    totalElements: 0,
  },
}

export const Loading: Story = {
  args: {
    loading: true,
  },
}
