import type { Meta, StoryObj } from '@storybook/vue3'

import ScoreAuditLogTable from './ScoreAuditLogTable.vue'
import type { ResScoreAuditLogDTO } from '@/types/scoreAudit'

const sampleLogs: ResScoreAuditLogDTO[] = [
  {
    auditLogId: 101,
    actorUserId: 2,
    actorUsername: 'academic.office',
    action: 'CALCULATION_TASK_RETRIED',
    entityType: 'CALCULATION_TASK',
    entityId: '1048',
    beforeData: { status: 'FAILED', attempt: 3 },
    afterData: { status: 'PENDING', attempt: 0 },
    requestId: 'req-7a91',
    ipAddress: '127.0.0.1',
    occurredAt: '2026-09-03T09:16:04',
  },
  {
    auditLogId: 102,
    actorUserId: 5,
    actorUsername: 'teacher.math',
    action: 'SCORE_UPDATED',
    entityType: 'STUDENT_SCORE',
    entityId: '882',
    beforeData: { score: 4.0 },
    afterData: { score: 6.0 },
    requestId: 'req-7a20',
    ipAddress: '127.0.0.1',
    occurredAt: '2026-09-03T08:46:10',
  },
  {
    auditLogId: 103,
    actorUserId: 2,
    actorUsername: 'academic.office',
    action: 'TRANSCRIPT_CALCULATION',
    entityType: 'CALCULATION_TASK',
    entityId: '1041',
    beforeData: { status: 'IN_PROGRESS' },
    afterData: { status: 'FINISH' },
    requestId: 'req-79fe',
    ipAddress: '127.0.0.1',
    occurredAt: '2026-09-02T17:20:44',
  },
]

const meta = {
  title: 'Operations/ScoreAuditLogTable',
  component: ScoreAuditLogTable,
  tags: ['autodocs'],
  args: {
    logs: sampleLogs,
    totalElements: 3,
    loading: false,
    page: 0,
    size: 10,
    entityTypeFilter: '',
    actionFilter: '',
    studentCodeFilter: '',
  },
} satisfies Meta<typeof ScoreAuditLogTable>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const Empty: Story = {
  args: {
    logs: [],
    totalElements: 0,
  },
}

export const Loading: Story = {
  args: {
    loading: true,
  },
}
