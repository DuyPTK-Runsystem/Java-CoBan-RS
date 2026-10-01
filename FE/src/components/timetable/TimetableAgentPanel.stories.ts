import type { Meta, StoryObj } from '@storybook/vue3'
import TimetableAgentPanel from './TimetableAgentPanel.vue'
import { agentAssignments, agentExistingEntries } from './timetableAgent.fixtures'

const meta = {
  title: 'Timetable/Agent/Input', component: TimetableAgentPanel, tags: ['autodocs'],
  args: { targetRevisionId: 33, expectedVersion: 7, defaultValidFrom: '2026-10-05', defaultValidTo: '2026-12-31', classes: [{ id: 6, name: '6A' }], assignments: agentAssignments, existingEntries: agentExistingEntries, canGenerate: true, busy: false },
} satisfies Meta<typeof TimetableAgentPanel>
export default meta
type Story = StoryObj<typeof meta>
export const Idle: Story = {}
export const Generating: Story = { args: { busy: true } }
export const Disabled: Story = { args: { canGenerate: false } }
export const AssignmentsLoading: Story = { args: { assignmentsLoading: true } }
