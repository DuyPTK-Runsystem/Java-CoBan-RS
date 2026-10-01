import type { TimetableAgentAssignmentOption, TimetableAgentProposal, TimetableAgentReceipt } from '@/types/timetableAgent'
import type { TimetableEntry, TimetablePeriod } from '@/types/timetable'

export const agentAssignments: TimetableAgentAssignmentOption[] = [
  { id: 101, classId: 6, className: '6A', subjectName: 'Toán', teacherName: 'Cô Lan' },
  { id: 102, classId: 6, className: '6A', subjectName: 'Ngữ văn', teacherName: 'Thầy Minh' },
]
export const agentPeriods: TimetablePeriod[] = [
  { id: 201, dayOfWeek: 2, session: 'MORNING', periodIndex: 1, periodName: 'Tiết 1', startTime: '07:00', endTime: '07:45' },
  { id: 202, dayOfWeek: 3, session: 'MORNING', periodIndex: 2, periodName: 'Tiết 2', startTime: '07:50', endTime: '08:35' },
]
const held = { assignmentId: 101, periodId: 201, functionalRoomId: null, validFrom: '2026-10-05', validTo: '2026-12-31' }
const added = { assignmentId: 102, periodId: 202, functionalRoomId: null, validFrom: '2026-10-05', validTo: '2026-12-31' }
export const agentExistingEntries: TimetableEntry[] = [{ ...held, id: 1, entryId: 1, revisionId: 33, classId: 6, className: '6A', subjectId: 1, subjectName: 'Toán', teacherId: 1, teacherName: 'Cô Lan', dayOfWeek: 2, session: 'MORNING', periodIndex: 1 }]
export const agentProposal: TimetableAgentProposal = {
  proposalId: 'proposal-demo', proposalVersion: 1, proposalHash: 'demo-hash', targetRevisionId: 33, expectedVersion: 7,
  status: 'READY_FOR_REVIEW', snapshotId: 'snapshot-demo', expiresAt: '2099-01-01T00:00:00Z',
  entries: [held, added], issues: [], explanation: 'Phương án minh hoạ giữ nguyên tiết đã khoá, ưu tiên tránh lịch bận. Dữ liệu dùng để review giao diện.',
  diff: { added: [added], removed: [], unchanged: [{ ...held, entryId: 1 }] },
  capabilities: { canGenerate: true, canApprove: true, canExecute: false },
}
export const agentReceipt: TimetableAgentReceipt = { actionId: 'action-demo', proposalId: 'proposal-demo', targetRevisionId: 33, newVersion: 8, savedEntryCount: 2, committedAt: '2026-10-01T09:00:00Z', status: 'SAVED_DRAFT' }
