import type { Meta, StoryObj } from '@storybook/vue3'

import ScoreChangeRequestForm from './ScoreChangeRequestForm.vue'

const meta = { title: 'Score change/ScoreChangeRequestForm', component: ScoreChangeRequestForm, tags: ['autodocs'], args: { columns: [{ id: 4, scorebookId: 12, assessmentType: 'KTTT', columnNo: 1, columnName: 'Thường xuyên 1', weightFactor: null, required: true, status: 'ACTIVE' }] } } satisfies Meta<typeof ScoreChangeRequestForm>
export default meta
type Story = StoryObj<typeof meta>
export const Empty: Story = {}
export const Disabled: Story = { args: { disabled: true } }
