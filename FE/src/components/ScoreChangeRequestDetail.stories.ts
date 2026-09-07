import type { Meta, StoryObj } from '@storybook/vue3'

import ScoreChangeRequestDetail from './ScoreChangeRequestDetail.vue'

const detail = { requestId: 9, assessmentColumnId: 4, studentId: 21, studentCode: 'HS-001', studentName: 'Nguyễn Minh An', studentScoreId: 31, beforeStatus: 'SCORED' as const, beforeValue: 6.5, proposedStatus: 'SCORED' as const, proposedValue: 8, reason: 'Nhập nhầm điểm sau khi đối chiếu bài kiểm tra.', requestedBy: 5, requestedAt: '2026-09-03 09:15', status: 'PENDING' as const, reviewedBy: null, reviewedAt: null, rejectionReason: null, appliedAt: null }
const meta = { title: 'Score change/ScoreChangeRequestDetail', component: ScoreChangeRequestDetail, tags: ['autodocs'], args: { detail, canReview: true, canCancel: false } } satisfies Meta<typeof ScoreChangeRequestDetail>
export default meta
type Story = StoryObj<typeof meta>
export const OfficeReview: Story = {}
export const Applied: Story = { args: { detail: { ...detail, status: 'APPLIED', reviewedBy: 2, reviewedAt: '2026-09-03 10:00', appliedAt: '2026-09-03 10:00' }, canReview: false } }
