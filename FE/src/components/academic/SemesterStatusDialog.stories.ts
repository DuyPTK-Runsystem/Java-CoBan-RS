import type { Meta, StoryObj } from '@storybook/vue3'

import type { Semester, SemesterCompletenessReport } from '@/types/academic'
import SemesterStatusDialog from './SemesterStatusDialog.vue'

const activeSemester: Semester = { id: 11, academicYearId: 1, code: 'HK1', name: 'Học kỳ I', displayOrder: 1, startDate: '2026-09-01', endDate: '2026-12-31', automaticLockAt: '2027-01-15T17:00:00', status: 'ACTIVE', lockedAt: null, lockedBy: null, lockReason: null, reopenUntil: null }
const lockedSemester: Semester = { ...activeSemester, status: 'LOCKED', lockedAt: '2027-01-16T08:15:00', lockedBy: 42, lockReason: 'Khóa sau khi hoàn tất rà soát', reopenUntil: '2027-01-23T23:59:59' }
const incompleteReport: SemesterCompletenessReport = {
  reportId: 101, runId: 201, semesterId: 11, checkpointCode: 'PRE_LOCK', reportStatus: 'INCOMPLETE', evaluatedAt: '2027-01-15T16:30:00', scopeType: 'SEMESTER',
  summary: { complete: false, missingKtdkCount: 3, invalidKtckCount: 1, missingSkillColumnsCount: 2, unenteredScoreCount: 8, studentWithoutScoreDataCount: 1, unpublishedScorebookCount: 2, pendingScoreChangeRequestCount: 1, details: ['Lớp 7A · Toán: thiếu điểm KTĐK', 'Lớp 8B · Ngữ văn: sổ điểm chưa công bố'] },
  failureReason: null, correlationId: 'corr-2027-001',
}
const completeReport: SemesterCompletenessReport = { ...incompleteReport, reportId: 102, reportStatus: 'COMPLETE', evaluatedAt: '2027-01-16T08:00:00', summary: { complete: true, missingKtdkCount: 0, invalidKtckCount: 0, missingSkillColumnsCount: 0, unenteredScoreCount: 0, studentWithoutScoreDataCount: 0, unpublishedScorebookCount: 0, pendingScoreChangeRequestCount: 0, details: [] } }
const failedReport: SemesterCompletenessReport = { ...incompleteReport, reportId: null, runId: null, reportStatus: 'FAILED', failureReason: 'Dịch vụ đánh giá dữ liệu điểm tạm thời không khả dụng.', summary: { ...incompleteReport.summary, details: [] } }

const meta = {
  title: 'Academic/SemesterStatusDialog',
  component: SemesterStatusDialog,
  tags: ['autodocs'],
  args: { visible: true, semester: activeSemester, report: incompleteReport, loading: false, actionLoading: false, errorMessage: '' },
  parameters: { layout: 'fullscreen' },
} satisfies Meta<typeof SemesterStatusDialog>

export default meta
type Story = StoryObj<typeof meta>

export const ActiveIncomplete: Story = {}
export const LockedComplete: Story = { args: { semester: lockedSemester, report: completeReport } }
export const ReportFailed: Story = { args: { report: failedReport } }
export const ReportLoading: Story = { args: { report: null, loading: true } }
