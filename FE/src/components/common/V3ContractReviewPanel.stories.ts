import type { Meta, StoryObj } from '@storybook/vue3'

import V3ContractReviewPanel from './V3ContractReviewPanel.vue'
import { v3FoundationReviewContext, v3FoundationReviewItems } from '@/fixtures/v3FoundationFixture'

const meta = {
  title: 'V3/Foundation Contract Review Panel',
  component: V3ContractReviewPanel,
  tags: ['autodocs'],
  args: { context: v3FoundationReviewContext, items: v3FoundationReviewItems },
} satisfies Meta<typeof V3ContractReviewPanel>

export default meta
type Story = StoryObj<typeof meta>

export const Ready: Story = { args: { state: 'ready' } }
export const Loading: Story = { args: { state: 'loading' } }
export const Empty: Story = { args: { state: 'empty' } }
export const AuthenticationRequired: Story = { args: { state: 'unauthorized' } }
export const PermissionDenied: Story = { args: { state: 'forbidden' } }
export const NotFound: Story = { args: { state: 'not-found' } }
export const Conflict: Story = { args: { state: 'conflict' } }
