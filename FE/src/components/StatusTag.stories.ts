import type { Meta, StoryObj } from '@storybook/vue3'

import StatusTag from './StatusTag.vue'

const meta = { title: 'Foundation/StatusTag', component: StatusTag, tags: ['autodocs'] } satisfies Meta<typeof StatusTag>
export default meta
type Story = StoryObj<typeof meta>

export const InProgress: Story = { args: { status: 'IN_PROGRESS' } }
export const Finished: Story = { args: { status: 'FINISH' } }
export const CustomLabel: Story = { args: { label: 'Needs review', severity: 'info' } }
