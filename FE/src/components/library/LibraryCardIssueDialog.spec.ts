import { mount, type VueWrapper } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

import ButtonStub from '@/test/stubs/ButtonStub.vue'
import LibraryCardIssueDialog from './LibraryCardIssueDialog.vue'

const selectStub = {
  props: ['modelValue', 'options'],
  emits: ['update:modelValue'],
  template: '<select v-bind="$attrs" :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><option v-for="option in options" :key="option.value" :value="option.value">{{ option.label }}</option></select>',
}
const inputTextStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: '<input v-bind="$attrs" :value="modelValue ?? \'\'" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}
const inputNumberStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: '<input v-bind="$attrs" :value="modelValue ?? \'\'" @input="$emit(\'update:modelValue\', $event.target.value === \'\' ? null : Number($event.target.value))" />',
}
const dialogStub = { props: ['visible'], template: '<div v-if="visible"><slot /></div>' }
const alertStub = { props: ['message'], template: '<p>{{ message }}</p>' }

const patron = {
  patronId: 7,
  userId: 18,
  displayName: 'Nguyễn An',
  status: 'ACTIVE' as const,
  joinedAt: '2026-10-09',
  suspensionReasons: [],
  currentCard: null,
}

function mountDialog(mode: 'issue' | 'reissue' = 'issue') {
  return mount(LibraryCardIssueDialog, {
    props: { visible: true, mode, patron },
    global: { stubs: { Button: ButtonStub, Dialog: dialogStub, FormAlert: alertStub, InputNumber: inputNumberStub, InputText: inputTextStub, Select: selectStub } },
  })
}

async function clickAction(wrapper: VueWrapper, label: string) {
  await wrapper.findAll('button').find((item) => item.text() === label)!.trigger('click')
}

describe('LibraryCardIssueDialog', () => {
  afterEach(() => vi.useRealTimers())

  it('sends the calculated inclusive expiry date when duration is entered in months', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-10-09T03:00:00.000Z'))
    const wrapper = mountDialog()

    await wrapper.get('#card-validity-months').setValue('12')
    await clickAction(wrapper, 'Phát hành thẻ')

    expect(wrapper.emitted('submit')?.[0]?.[0]).toEqual({ expiresAt: '2027-10-08' })
  })

  it('sends a directly entered date to the same expiry-date request field', async () => {
    const wrapper = mountDialog()
    await wrapper.get('#card-expiry-mode').setValue('date')
    await wrapper.get('#card-expiry-date').setValue('2099-12-31')
    await clickAction(wrapper, 'Phát hành thẻ')

    expect(wrapper.emitted('submit')?.[0]?.[0]).toEqual({ expiresAt: '2099-12-31' })
  })

  it('rejects a past direct date and requires a reason when reissuing', async () => {
    const issue = mountDialog()
    await issue.get('#card-expiry-mode').setValue('date')
    await issue.get('#card-expiry-date').setValue('2000-01-01')
    await clickAction(issue, 'Phát hành thẻ')
    expect(issue.text()).toContain('Ngày hết hạn phải là hôm nay hoặc một ngày trong tương lai.')
    expect(issue.emitted('submit')).toBeUndefined()

    const reissue = mountDialog('reissue')
    await clickAction(reissue, 'Cấp lại thẻ')
    expect(reissue.text()).toContain('Vui lòng nhập lý do cấp lại thẻ.')
    expect(reissue.emitted('submit')).toBeUndefined()
  })
})
