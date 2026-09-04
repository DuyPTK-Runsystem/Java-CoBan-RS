import type { Meta, StoryObj } from '@storybook/vue3'

import ScoreChangeRequestForm from './ScoreChangeRequestForm.vue'

const meta = { title: 'Score change/ScoreChangeRequestForm', component: ScoreChangeRequestForm, tags: ['autodocs'] } satisfies Meta<typeof ScoreChangeRequestForm>
export default meta
type Story = StoryObj<typeof meta>
export const Standalone: Story = {}
export const FromScorebookCell: Story = { args: { context: { studentCode: 'HS-001', studentName: 'Nguyễn Minh An', columnId: 4, columnName: 'Thường xuyên 1', currentStatus: 'SCORED', currentValue: 6.5 } } }
export const FromAbsentScorebookCell: Story = { args: { context: { studentCode: 'HS-002', studentName: 'Trần Ngọc Mai', columnId: 4, columnName: 'Thường xuyên 1', currentStatus: 'ABSENT', currentValue: null } } }
export const Disabled: Story = { args: { disabled: true } }
