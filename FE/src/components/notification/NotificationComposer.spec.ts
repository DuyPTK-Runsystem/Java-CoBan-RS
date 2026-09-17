import { mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { describe, expect, it } from 'vitest'
import NotificationComposer from './NotificationComposer.vue'
import ButtonStub from '@/test/stubs/ButtonStub.vue'
import { DEFAULT_SCHOOL } from '@/types/notification'

const InputTextStub = defineComponent({
  props: { modelValue: { type: String, default: '' } },
  emits: ['update:modelValue'],
  template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)">',
})

const SelectStub = defineComponent({
  props: {
    modelValue: { type: String, default: 'IN_APP' },
    options: { type: Array, default: () => [] },
  },
  emits: ['update:modelValue'],
  template: '<select id="notification-channel" :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><option v-for="option in options" :key="option.value" :value="option.value">{{ option.label }}</option></select>',
})

const composerStubs = {
  Button: ButtonStub,
  InputText: InputTextStub,
  Select: SelectStub,
  Textarea: InputTextStub,
  NotificationAudienceSelector: true,
}

describe('NotificationComposer', () => {
  it('validates empty title and body on submit', async () => {
    const wrapper = mount(NotificationComposer, {
      global: {
        stubs: {
          Button: ButtonStub,
          InputText: true,
          Select: SelectStub,
          Textarea: true,
          NotificationAudienceSelector: true,
        },
      },
    })

    await wrapper.find('form').trigger('submit.prevent')
    expect(wrapper.text()).toContain('Tiêu đề thông báo không được để trống')
    expect(wrapper.emitted('submit')).toBeFalsy()
  })

  it('toggles preview mode when preview button is clicked', async () => {
    const wrapper = mount(NotificationComposer, {
      global: {
        stubs: {
          Button: ButtonStub,
          InputText: true,
          Select: SelectStub,
          Textarea: true,
          NotificationAudienceSelector: true,
        },
      },
    })

    const buttons = wrapper.findAllComponents(ButtonStub)
    const previewBtn = buttons.find((b) => b.text().includes('Xem trước'))
    expect(previewBtn).toBeDefined()

    await previewBtn!.trigger('click')
    expect(wrapper.text()).toContain('Xem trước thông báo')
  })

  it('emits the single-school scope and a stable idempotency key for the same payload', async () => {
    const wrapper = mount(NotificationComposer, { global: { stubs: composerStubs } })

    const fields = wrapper.findAll('input')
    await fields[0]!.setValue('Thông báo kiểm tra')
    await fields[1]!.setValue('Nội dung kiểm tra')
    await wrapper.find('form').trigger('submit.prevent')

    const firstPayload = wrapper.emitted('submit')?.[0]?.[0] as Record<string, unknown>
    expect(firstPayload).toMatchObject({
      title: 'Thông báo kiểm tra',
      body: 'Nội dung kiểm tra',
      audienceType: 'SCHOOL',
      schoolScope: DEFAULT_SCHOOL,
    })
    expect(firstPayload.channel).toBeUndefined()
    expect(firstPayload.idempotencyKey).toEqual(expect.any(String))

    await wrapper.find('form').trigger('submit.prevent')
    const secondPayload = wrapper.emitted('submit')?.[1]?.[0] as Record<string, unknown>
    expect(secondPayload.idempotencyKey).toBe(firstPayload.idempotencyKey)
  })

  it('emits EMAIL when the email channel is selected', async () => {
    const wrapper = mount(NotificationComposer, { global: { stubs: composerStubs } })
    await wrapper.find('#notification-channel').setValue('EMAIL')
    const fields = wrapper.findAll('input')
    await fields[0]!.setValue('Thông báo email')
    await fields[1]!.setValue('Nội dung email')
    await wrapper.find('form').trigger('submit.prevent')

    const payload = wrapper.emitted('submit')?.[0]?.[0] as Record<string, unknown>
    expect(payload.channel).toBe('EMAIL')
  })

  it('creates a new idempotency key when the payload changes', async () => {
    const wrapper = mount(NotificationComposer, { global: { stubs: composerStubs } })
    const fields = wrapper.findAll('input')
    await fields[0]!.setValue('Thông báo thứ nhất')
    await fields[1]!.setValue('Nội dung thứ nhất')
    await wrapper.find('form').trigger('submit.prevent')
    const firstKey = (wrapper.emitted('submit')?.[0]?.[0] as Record<string, unknown>).idempotencyKey

    await fields[0]!.setValue('Thông báo thứ hai')
    await wrapper.find('form').trigger('submit.prevent')
    const secondKey = (wrapper.emitted('submit')?.[1]?.[0] as Record<string, unknown>).idempotencyKey

    expect(secondKey).not.toBe(firstKey)
  })
})
