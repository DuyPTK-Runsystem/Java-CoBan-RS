import type { Meta, StoryObj } from '@storybook/vue3'

import ScorebookWorkspaceReview from './ScorebookWorkspaceReview.vue'

const meta = {
  title: 'Scorebook/ScorebookWorkspaceReview',
  component: ScorebookWorkspaceReview,
  tags: ['autodocs'],
  parameters: { layout: 'fullscreen' },
  args: { status: 'OPEN', reviewState: 'READY', activeTab: 'grid' },
} satisfies Meta<typeof ScorebookWorkspaceReview>

export default meta
type Story = StoryObj<typeof meta>

export const OpenGrid: Story = {}
export const DraftLifecycle: Story = { args: { status: 'DRAFT' } }
export const PublishedReadOnly: Story = { args: { status: 'PUBLISHED', activeTab: 'columns' } }
export const Empty: Story = { args: { reviewState: 'EMPTY' } }
export const Forbidden: Story = { args: { reviewState: 'FORBIDDEN' } }
export const OptimisticConflict: Story = { args: { reviewState: 'CONFLICT' } }
