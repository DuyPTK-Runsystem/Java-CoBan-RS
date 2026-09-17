import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import LessonLogWeeklyMatrix from './LessonLogWeeklyMatrix.vue'
import { weeklyFixture } from '@/fixtures/lessonLogFixture'

describe('LessonLogWeeklyMatrix', () => {
  it('renders all 2x4 slots and keeps unlogged slots visible', () => {
    const wrapper = mount(LessonLogWeeklyMatrix, { props: { days: weeklyFixture.calendarDays, entries: weeklyFixture.items } })
    expect(wrapper.findAll('.matrix-label')).toHaveLength(8)
    expect(wrapper.text()).toContain('Chưa ghi')
    expect(wrapper.text()).toContain('Nghỉ')
  })
  it('emits selected entry without changing the weekly set', async () => {
    const wrapper = mount(LessonLogWeeklyMatrix, { props: { days: weeklyFixture.calendarDays, entries: weeklyFixture.items } })
    await wrapper.find('.matrix-entry').trigger('click')
    expect(wrapper.emitted('select')?.[0]?.[0]).toMatchObject({ entryId: 101 })
  })

  it('emits an unlogged occurrence so the parent can open the entry dialog', async () => {
    const unlogged = { ...weeklyFixture.items[0], entryId: 0, status: 'UNLOGGED' as const, canTeacherEdit: true }
    const wrapper = mount(LessonLogWeeklyMatrix, { props: { days: weeklyFixture.calendarDays, entries: [unlogged] } })

    await wrapper.find('.matrix-entry').trigger('click')

    expect(wrapper.emitted('select')?.[0]?.[0]).toMatchObject({
      entryId: 0,
      lessonDate: unlogged.lessonDate,
      session: unlogged.session,
      periodIndex: unlogged.periodIndex,
    })
  })
})
