import type { Meta, StoryObj } from '@storybook/vue3'
import TimetableAgentReview from './TimetableAgentReview.vue'
import { agentAssignments, agentPeriods, agentProposal, agentReceipt } from './timetableAgent.fixtures'

const meta = {
  title: 'Timetable/Agent/Review', component: TimetableAgentReview, tags: ['autodocs'],
  args: { proposal: agentProposal, phase: 'idle', periods: agentPeriods, assignments: agentAssignments, receipt: null, canSave: true },
} satisfies Meta<typeof TimetableAgentReview>
export default meta
type Story = StoryObj<typeof meta>
export const Ready: Story = {}
export const Warnings: Story = { args: { proposal: { ...agentProposal, issues: [{ code: 'PREFERENCE_UNMET', severity: 'WARNING', path: '', message: 'Cô Lan còn một khoảng trống giữa buổi.' }] } } }
export const NeedsInput: Story = { args: { proposal: { ...agentProposal, status: 'NEEDS_INPUT', entries: [], issues: [{ code: 'DEMAND_MISSING', severity: 'BLOCKING', path: 'demands', message: 'Bổ sung số tiết môn Toán.' }] }, canSave: false } }
export const Conflicts: Story = { args: { proposal: { ...agentProposal, status: 'CONFLICTS', issues: [{ code: 'TEACHER_OVERLAP', severity: 'BLOCKING', path: 'entries', message: 'Giáo viên bị trùng lịch trong cùng tiết.' }] }, canSave: false } }
export const NoSolutionFound: Story = { args: { proposal: { ...agentProposal, status: 'NO_SOLUTION_FOUND', entries: [], explanation: 'Chưa tìm được phương án trong giới hạn. Giữ yêu cầu để điều chỉnh.' }, canSave: false } }
export const Approved: Story = { args: { proposal: { ...agentProposal, status: 'APPROVED', capabilities: { canGenerate: true, canApprove: false, canExecute: true } }, canSave: true } }
export const Executing: Story = { args: { ...Approved.args, phase: 'executing' } }
export const Stale: Story = { args: { phase: 'stale', canSave: false } }
export const Expired: Story = { args: { phase: 'expired', canSave: false } }
export const Denied: Story = { args: { phase: 'denied', canSave: false } }
export const ProviderTimeout: Story = { args: { phase: 'provider-timeout', canSave: false } }
export const InvalidSchema: Story = { args: { phase: 'invalid-schema', canSave: false } }
export const Saved: Story = { args: { receipt: agentReceipt, canSave: false } }
export const CommittedResponseLost: Story = { args: { phase: 'response-lost', pendingRecovery: true, canSave: false } }
export const SaveInProgress: Story = { args: { ...CommittedResponseLost.args, error: 'Yêu cầu lưu đang được xử lý. Kiểm tra lại trạng thái trước khi tiếp tục.' } }
export const RetryPriorSave: Story = { args: { ...CommittedResponseLost.args, canRetryPending: true, error: 'Lần lưu trước chưa hoàn tất. Có thể thử lại chính yêu cầu đó khi hệ thống khả dụng.' } }
