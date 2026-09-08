import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

const requireMock = vi.hoisted(() => vi.fn())
vi.mock('primevue/useconfirm', () => ({ useConfirm: () => ({ require: requireMock }) }))

import ConfirmAction from './ConfirmAction.vue'
import ButtonStub from '@/test/stubs/ButtonStub.vue'

describe('ConfirmAction', () => {
  it('configures PrimeVue confirmation and emits confirm/cancel callbacks', async () => {
    const wrapper = mount(ConfirmAction, {
      props: { message: 'Delete this record?', label: 'Delete' },
      global: { stubs: { Button: ButtonStub, ConfirmDialog: true } },
    })

    await wrapper.get('button').trigger('click')
    const options = requireMock.mock.calls[0][0] as { accept: () => void; reject: () => void; message: string }
    expect(options.message).toBe('Delete this record?')
    options.accept()
    options.reject()
    expect(wrapper.emitted('confirm')).toHaveLength(1)
    expect(wrapper.emitted('cancel')).toHaveLength(1)
  })
})
