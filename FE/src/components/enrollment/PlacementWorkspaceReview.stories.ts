import type { Meta, StoryObj } from '@storybook/vue3'

import { placementReviewFixture } from '@/fixtures/placementFixture'
import PlacementWorkspaceReview from './PlacementWorkspaceReview.vue'

const meta = { title: 'Enrollment/PlacementWorkspaceReview', component: PlacementWorkspaceReview, tags: ['autodocs'], parameters: { layout: 'fullscreen' }, args: { session: placementReviewFixture } } satisfies Meta<typeof PlacementWorkspaceReview>
export default meta
type Story = StoryObj<typeof meta>

export const PreviewReady: Story = {}
export const ManualHandlingStillConfirmable: Story = { args: { session: placementReviewFixture } }
export const DraftPlacementOptions: Story = { args: { editable: true, session: { ...placementReviewFixture, status: 'DRAFT' } } }
export const ClassSizeLimitBlocksConfirmation: Story = {
  args: {
    session: {
      ...placementReviewFixture,
      status: 'SIMULATED',
      results: [
        {
          id: 1,
          studentId: 1,
          targetClassId: 81,
          resultStatus: 'AUTO_ASSIGNED',
          score: 8.5,
          issueCode: 'CAPACITY_EXCEEDED',
          issueSeverity: 'BLOCKING',
          explanation: 'Lớp 8A1 đã vượt sĩ số cho phép khi xếp lớp tự động.',
        },
      ],
    },
  },
}
export const Conflict409: Story = { args: { reviewState: 'conflict' } }
export const Forbidden403: Story = { args: { reviewState: 'forbidden' } }
export const Empty: Story = { args: { session: null, reviewState: 'empty' } }
