import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, h, type PropType, type VNodeChild } from 'vue'
import { describe, expect, it } from 'vitest'

import BookCopyTable from './BookCopyTable.vue'
import type { BookCopy } from '@/types/library/catalog'

const statuses = ['AVAILABLE', 'DAMAGED', 'ON_LOAN', 'RESERVED', 'LOST', 'WITHDRAWN'] as const
const copies = statuses.map((status, index) => ({
  id: index + 1,
  barcode: `LIB-${String(index + 1).padStart(9, '0')}`,
  status,
  shelfLocation: null,
  referenceOnly: index === 0,
  version: 0,
}))
const button = { props: ['ariaLabel', 'title'], emits: ['click'], template: '<button v-bind="$attrs" :aria-label="ariaLabel" :title="title" @click="$emit(\'click\', $event)">{{ ariaLabel }}</button>' }
const statusTag = { props: ['label'], template: '<span>{{ label }}</span>' }
type ColumnSlots = { body?: (context: { data: BookCopy }) => VNodeChild }
const dataTable = defineComponent({
  props: { value: { type: Array as PropType<BookCopy[]>, default: () => [] }, selection: { type: Array as PropType<BookCopy[]>, default: () => [] } },
  emits: ['update:selection'],
  setup(props, { slots }) {
    return () => h('table', [h('tbody', props.value.map((row) => h('tr', { key: row.barcode }, (slots.default?.() ?? []).map((column) => {
      const columnSlots = column.children && typeof column.children === 'object' && !Array.isArray(column.children)
        ? column.children as ColumnSlots
        : undefined
      const field = column.props?.field
      return h('td', columnSlots?.body ? columnSlots.body({ data: row }) : field ? String(row[field as keyof BookCopy] ?? '') : '')
    }))))])
  },
})

describe('BookCopyTable role and lifecycle actions', () => {
  it('limits manager mutations to AVAILABLE and DAMAGED copies and labels reference-only inventory clearly', async () => {
    const wrapper = mount(BookCopyTable, { props: { copies, canManage: true }, global: { stubs: { Button: button, Column: true, DataTable: dataTable, StatusTag: statusTag } } })
    await flushPromises()
    const rows = wrapper.findAll('tbody tr')
    expect(rows).toHaveLength(6)
    expect(rows[0]?.text()).toContain('Có sẵn')
    expect(rows[0]?.text()).toContain('Chỉ đọc tại chỗ')
    expect(rows[0]?.text()).not.toContain('Có thể cho mượn')
    for (const rowIndex of [0, 1]) {
      const row = rows[rowIndex]!
      expect(row.find('[aria-label="Sửa thông tin bản sao"]').exists()).toBe(true)
      expect(row.find('[aria-label="Rút bản sao khỏi kho"]').exists()).toBe(true)
    }
    expect(rows[0]?.find('[aria-label="Đánh dấu hư hỏng"]').exists()).toBe(true)
    expect(rows[1]?.find('[aria-label="Đánh dấu có sẵn"]').exists()).toBe(true)
    for (const rowIndex of [2, 3, 4, 5]) {
      expect(rows[rowIndex]?.find('[aria-label="Sửa thông tin bản sao"]').exists()).toBe(false)
      expect(rows[rowIndex]?.find('[aria-label="Rút bản sao khỏi kho"]').exists()).toBe(false)
    }
  })

  it('hides every mutation control from a reader while keeping barcode viewing available', async () => {
    const wrapper = mount(BookCopyTable, { props: { copies, canManage: false }, global: { stubs: { Button: button, Column: true, DataTable: dataTable, StatusTag: statusTag } } })
    await flushPromises()
    expect(wrapper.findAll('tbody tr')).toHaveLength(6)
    expect(wrapper.findAll('[aria-label="Sửa thông tin bản sao"]')).toHaveLength(0)
    expect(wrapper.findAll('[aria-label="Đánh dấu hư hỏng"]')).toHaveLength(0)
    expect(wrapper.findAll('[aria-label="Rút bản sao khỏi kho"]')).toHaveLength(0)
    expect(wrapper.findAll('[aria-label="Xem mã vạch"]')).toHaveLength(6)
  })

  it('disables every row action while another catalog mutation is pending', async () => {
    const wrapper = mount(BookCopyTable, { props: { copies: [copies[0]!], canManage: true, actionsDisabled: true }, global: { stubs: { Button: button, Column: true, DataTable: dataTable, StatusTag: statusTag } } })
    await flushPromises()
    expect(wrapper.findAll('tbody button')).toHaveLength(4)
    expect(wrapper.findAll('tbody button').every((item) => item.attributes('disabled') !== undefined)).toBe(true)
  })

  it('forwards selection updates when the table selection changes', async () => {
    const wrapper = mount(BookCopyTable, { props: { copies, selection: [] }, global: { stubs: { Button: button, Column: true, DataTable: dataTable, StatusTag: statusTag } } })
    await flushPromises()
    wrapper.findComponent(dataTable).vm.$emit('update:selection', [copies[0]])
    expect(wrapper.emitted('update:selection')).toEqual([[[copies[0]]]])
  })
})
