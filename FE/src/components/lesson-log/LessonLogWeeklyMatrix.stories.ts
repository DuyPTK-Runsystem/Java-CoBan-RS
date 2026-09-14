import type { Meta, StoryObj } from '@storybook/vue3'
import LessonLogWeeklyMatrix from './LessonLogWeeklyMatrix.vue'
import { weeklyFixture } from '@/fixtures/lessonLogFixture'

const meta = { title: 'Lesson log/Weekly matrix', component: LessonLogWeeklyMatrix, tags: ['autodocs'] } satisfies Meta<typeof LessonLogWeeklyMatrix>
export default meta
type Story = StoryObj<typeof meta>
export const MixedStates: Story = { args: { days: weeklyFixture.calendarDays, entries: weeklyFixture.items } }
export const EmptyWeek: Story = { args: { days: weeklyFixture.calendarDays, entries: [] } }
