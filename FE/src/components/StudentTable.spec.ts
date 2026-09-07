import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import StudentTable from './StudentTable.vue'

const buttonStub = {
  props: ['label', 'disabled'],
  emits: ['click'],
  template: '<button :disabled="disabled" @click="$emit(\'click\')">{{ label }}</button>',
}

const inputNumberStub = {
  props: ['modelValue', 'disabled'],
  emits: ['update:modelValue'],
  template: '<input data-testid="go-to-page-input" :value="modelValue" :disabled="disabled" @input="$emit(\'update:modelValue\', Number($event.target.value))">',
}

const paginatorStub = {
  emits: ['page'],
  template: '<button data-testid="change-page-size" @click="$emit(\'page\', { first: 0, page: 0, rows: 20 })">Paginator</button>',
}

const dataTableStub = {
  emits: ['sort'],
  template: '<div><button data-testid="sort-birthday" @click="$emit(\'sort\', { sortField: \'dateOfBirth\', sortOrder: -1 })">Sort birthday</button><slot /></div>',
}

function mountTable(totalPages = 3) {
  return mount(StudentTable, {
    props: { totalRecords: 45, totalPages, page: 0, rowsPerPage: 10 },
    global: {
      stubs: {
        Button: buttonStub,
        Column: true,
        DataTable: dataTableStub,
        InputNumber: inputNumberStub,
        Paginator: paginatorStub,
      },
    },
  })
}

describe('StudentTable pagination', () => {
  it('does not request a server-side sort for birthday', async () => {
    const wrapper = mountTable()

    await wrapper.get('[data-testid="sort-birthday"]').trigger('click')

    expect(wrapper.emitted('sortChange')).toBeUndefined()
  })

  it('emits the selected page and paginator page size', async () => {
    const wrapper = mountTable()

    await wrapper.get('[data-testid="change-page-size"]').trigger('click')

    expect(wrapper.emitted('pageChange')).toEqual([[0, 20]])
  })

  it('converts a valid one-based Go to page value to a zero-based request', async () => {
    const wrapper = mountTable()

    await wrapper.get('[data-testid="go-to-page-input"]').setValue('2')
    await wrapper.get('button:last-child').trigger('click')

    expect(wrapper.emitted('pageChange')).toEqual([[1, 10]])
  })

  it('does not emit a request for an out-of-range page', async () => {
    const wrapper = mountTable(2)

    await wrapper.get('[data-testid="go-to-page-input"]').setValue('3')
    await wrapper.get('button:last-child').trigger('click')

    expect(wrapper.emitted('pageChange')).toBeUndefined()
    expect(wrapper.get('[role="alert"]').text()).toContain('Enter a page from 1 to 2.')
  })
})
