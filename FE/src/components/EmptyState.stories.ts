import type { Meta, StoryObj } from '@storybook/vue3'

import EmptyState from './EmptyState.vue'

const meta = { title: 'Foundation/EmptyState', component: EmptyState, tags: ['autodocs'] } satisfies Meta<typeof EmptyState>
export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}
export const WithAction: Story = { args: { heading: 'No academic years', message: 'Create the first academic year.', actionLabel: 'Create academic year' } }
