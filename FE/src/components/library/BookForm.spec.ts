import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ButtonStub from '@/test/stubs/ButtonStub.vue'
import BookForm from './BookForm.vue'

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
const formAlertStub = { props: ['message'], template: '<p>{{ message }}</p>' }

function mountForm() {
  return mount(BookForm, {
    global: { stubs: { Button: ButtonStub, FormAlert: formAlertStub, InputText: inputTextStub, InputNumber: inputNumberStub } },
  })
}

describe('BookForm', () => {
  it('requires title and author and rejects values beyond the frozen field limits', async () => {
    const wrapper = mountForm()
    await wrapper.get('#book-title').setValue('x'.repeat(201))
    await wrapper.get('#book-author').setValue('')
    await wrapper.get('#book-publisher').setValue('p'.repeat(201))
    await wrapper.get('#book-category').setValue('c'.repeat(101))
    await wrapper.get('#book-isbn').setValue('i'.repeat(21))
    await wrapper.get('#book-year').setValue('10000')
    await wrapper.get('#book-price').setValue('-1')
    await wrapper.get('#book-cover').setValue('javascript:alert(1)')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.text()).toContain('Tên sách tối đa 200 ký tự.')
    expect(wrapper.text()).toContain('Tác giả là bắt buộc.')
    expect(wrapper.text()).toContain('Nhà xuất bản tối đa 200 ký tự.')
    expect(wrapper.text()).toContain('Thể loại tối đa 100 ký tự.')
    expect(wrapper.text()).toContain('ISBN tối đa 20 ký tự.')
    expect(wrapper.text()).toContain('Năm xuất bản phải từ 1 đến 9999.')
    expect(wrapper.text()).toContain('Giá bìa phải không âm')
    expect(wrapper.text()).toContain('URL HTTP hoặc HTTPS hợp lệ.')
    expect(wrapper.emitted('save')).toBeUndefined()
  })

  it('trims valid form values and shows field-level server validation', async () => {
    const wrapper = mount(BookForm, {
      props: { fieldErrors: { isbn: 'ISBN này đã được sử dụng.' } },
      global: { stubs: { Button: ButtonStub, FormAlert: formAlertStub, InputText: inputTextStub, InputNumber: inputNumberStub } },
    })
    await wrapper.get('#book-title').setValue('  Dế Mèn  ')
    await wrapper.get('#book-author').setValue('  Tô Hoài  ')
    await wrapper.get('#book-isbn').setValue('  978-604-1  ')
    expect(wrapper.text()).toContain('ISBN này đã được sử dụng.')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('save')?.[0]?.[0]).toMatchObject({ title: 'Dế Mèn', author: 'Tô Hoài', isbn: '978-604-1' })
  })
})
