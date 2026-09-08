import type { Meta, StoryObj } from '@storybook/vue3'

import TransferScoreAssistDialog from './TransferScoreAssistDialog.vue'
import type { TransferScoreAssistSnapshot } from '@/types/enrollment'

const snapshot: TransferScoreAssistSnapshot = {
  enrollmentId: 701, studentId: 11, studentCode: 'HS011', studentName: 'Nguyễn An',
  academicYearId: 1, semesterId: 7,
  sourceClass: { id: 101, academicYearId: 1, classCode: '6A1', className: 'Lớp 6A1' },
  targetClass: { id: 102, academicYearId: 1, classCode: '6A2', className: 'Lớp 6A2' }, hasExistingScores: true, warnings: [],
  subjects: [{
    subjectId: 31, subjectCode: 'MATH', subjectName: 'Toán',
    sourceEvidence: [{ assessmentColumnId: 51, scorebookId: 501, assessmentType: 'KTTT', columnNo: 1, columnName: 'Thường xuyên 1', scoreStatus: 'SCORED', scoreValue: 7.5, note: null, version: 2 }],
    targetColumns: [
      { assessmentColumnId: 61, scorebookId: 601, assessmentType: 'KTTT', columnNo: 1, columnName: 'Thường xuyên 1', status: 'ACTIVE', mappingKey: 'MATH/KTTT/1', suggestedSourceColumnId: 51, existingScoreStatus: null, existingScoreValue: null, existingNote: null, existingVersion: null },
      { assessmentColumnId: 62, scorebookId: 601, assessmentType: 'KTĐK', columnNo: 1, columnName: 'Giữa kỳ', status: 'ACTIVE', mappingKey: 'MATH/KTĐK/1', suggestedSourceColumnId: null, existingScoreStatus: null, existingScoreValue: null, existingNote: null, existingVersion: null },
    ],
  }],
}

const meta = {
  title: 'Enrollment/TransferScoreAssistDialog', component: TransferScoreAssistDialog, tags: ['autodocs'],
  parameters: { layout: 'fullscreen' }, args: { visible: true, snapshot, effectiveAt: '2026-09-07T09:00:00', reason: 'Điều chỉnh sĩ số' },
} satisfies Meta<typeof TransferScoreAssistDialog>
export default meta
type Story = StoryObj<typeof meta>

export const DefaultWithEvidence: Story = {}
export const NoExistingScores: Story = { args: { snapshot: { ...snapshot, hasExistingScores: false, subjects: [] } } }
export const NoTargetColumns: Story = { args: { snapshot: { ...snapshot, subjects: [{ ...snapshot.subjects[0], targetColumns: [] }] } } }
export const UnmappedColumns: Story = { args: { snapshot: { ...snapshot, subjects: [{ ...snapshot.subjects[0], targetColumns: [snapshot.subjects[0].targetColumns[1]] }] } } }
export const Loading: Story = { args: { snapshot: null, loading: true } }
export const Forbidden403: Story = { args: { snapshot: null, errorMessage: 'Bạn không có quyền xem bằng chứng điểm của học sinh này.' } }
export const Conflict409: Story = { args: { errorMessage: 'Dữ liệu điểm hoặc enrollment đã thay đổi. Bản nháp vẫn được giữ.' } }
