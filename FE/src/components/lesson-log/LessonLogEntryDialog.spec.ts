import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import LessonLogEntryDialog from './LessonLogEntryDialog.vue'
import { lessonLogEntryFixture } from '@/fixtures/lessonLogFixture'

describe('LessonLogEntryDialog', () => {
  it('keeps draft available for incomplete fields but prevents submit', async () => {
    const entry = { ...lessonLogEntryFixture, entryId: 0, title: null, presentCount: null, absentCount: null, grade: null, completionStatus: null }
    const wrapper = mount(LessonLogEntryDialog, { props: { visible: true, entry }, global: { stubs: { Dialog: { template: '<div><slot /></div>' }, Button: { props: ['label', 'disabled'], template: '<button :disabled="disabled"><slot />{{ label }}</button>' }, InputText: { template: '<input />' }, Textarea: { template: '<textarea />' }, InputNumber: { template: '<input />' }, Select: { template: '<select />' }, FormAlert: { template: '<div><slot /></div>' } } } })
    expect(wrapper.find('button').exists()).toBe(true)
    const draft = wrapper.findAll('button').find((button) => button.text().includes('Lưu nháp'))
    await draft?.trigger('click')
    expect(wrapper.emitted('saveDraft')).toHaveLength(1)
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('requires roster snapshot sum before emitting submit', async () => {
    const wrapper = mount(LessonLogEntryDialog, { props: { visible: true, entry: { ...lessonLogEntryFixture, canSubmit: true } }, global: { stubs: { Dialog: { template: '<div><slot /></div>' }, Button: { props: ['label', 'disabled'], template: '<button :disabled="disabled">{{ label }}</button>' }, InputText: { template: '<input />' }, Textarea: { template: '<textarea />' }, InputNumber: { template: '<input />' }, Select: { template: '<select />' }, FormAlert: { template: '<div><slot /></div>' } } } })
    const submit = wrapper.findAll('button').find((button) => button.text().includes('Nộp sổ'))
    expect(submit).toBeDefined()
    await submit?.trigger('click')
    expect(wrapper.emitted('submit')).toHaveLength(1)
  })
})
