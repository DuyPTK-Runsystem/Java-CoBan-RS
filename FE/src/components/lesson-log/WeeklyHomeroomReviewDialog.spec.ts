import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import WeeklyHomeroomReviewDialog from './WeeklyHomeroomReviewDialog.vue'
import { weeklyFixture } from '@/fixtures/lessonLogFixture'

describe('WeeklyHomeroomReviewDialog', () => {
  it('requires a reason when signing a stale week', async () => {
    const wrapper = mount(WeeklyHomeroomReviewDialog, { props: { visible: true, review: { ...weeklyFixture.weeklyReview, status: 'STALE', canSignWeek: true, requiresReason: true } }, global: { stubs: { Dialog: { template: '<div><slot /></div>' }, Button: { props: ['label', 'disabled'], template: '<button :disabled="disabled">{{ label }}</button>' }, Textarea: { template: '<textarea :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />', props: ['modelValue'], emits: ['update:modelValue'] }, Select: { template: '<select />' }, LessonLogStatusBadge: { template: '<span />' } } } })
    const buttons = wrapper.findAll('button')
    const sign = buttons.find((button) => button.text().includes('Ký tuần'))
    await sign?.trigger('click')
    expect(wrapper.emitted('sign')).toBeUndefined()
    const reason = wrapper.find('#weekly-reason')
    await reason.setValue('Đã rà soát lại sau khi điều chỉnh tiết.')
    await sign?.trigger('click')
    expect(wrapper.emitted('sign')).toHaveLength(1)
    expect(wrapper.emitted('sign')?.[0]?.[2]).toContain('rà soát')
  })
})
