import type { Meta, StoryObj } from '@storybook/vue3'

import SkillWeightPanel from './SkillWeightPanel.vue'

const meta = {
  title: 'Scorebook/SkillWeightPanel',
  component: SkillWeightPanel,
  tags: ['autodocs'],
  args: {
    config: {
      id: 1,
      scorebookId: 12,
      ktttWeightPercent: 20,
      ktdkWeightPercent: 30,
      ktckWeightPercent: 50,
      configuredBy: 5,
      configuredAt: '2026-09-02T08:00:00',
      lockedBy: null,
      lockedAt: null,
    },
    readOnly: false,
    saving: false,
    errorMessage: '',
  },
} satisfies Meta<typeof SkillWeightPanel>

export default meta
type Story = StoryObj<typeof meta>

export const Editable: Story = {}
export const PublishedReadOnly: Story = { args: { readOnly: true } }

