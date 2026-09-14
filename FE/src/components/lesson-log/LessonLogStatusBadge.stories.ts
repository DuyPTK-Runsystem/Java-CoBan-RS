import type { Meta, StoryObj } from '@storybook/vue3'
import LessonLogStatusBadge from './LessonLogStatusBadge.vue'

const meta = { title: 'Lesson log/Status badge', component: LessonLogStatusBadge, tags: ['autodocs'] } satisfies Meta<typeof LessonLogStatusBadge>
export default meta
type Story = StoryObj<typeof meta>
export const Draft: Story = { args: { status: 'DRAFT' } }
export const Submitted: Story = { args: { status: 'SUBMITTED' } }
export const StaleWeek: Story = { args: { status: 'STALE' } }
