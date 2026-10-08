import type { Meta, StoryObj } from '@storybook/vue3'

import type { BookCopy } from '@/types/library/catalog'
import BookCopyTable from './BookCopyTable.vue'

const copies: BookCopy[] = [
  { id: 1, bookId: 101, barcode: 'LIB-000000001', shelfLocation: 'A1-02', status: 'AVAILABLE', referenceOnly: false, version: 0, createdAt: '2026-10-08T08:00:00' },
  { id: 2, bookId: 101, barcode: 'LIB-000000002', shelfLocation: 'A1-03', status: 'DAMAGED', referenceOnly: false, version: 1, createdAt: '2026-10-08T08:00:00' },
  { id: 3, bookId: 101, barcode: 'LIB-000000003', shelfLocation: 'R1-01', status: 'AVAILABLE', referenceOnly: true, version: 0, createdAt: '2026-10-08T08:00:00' },
]

const meta = { title: 'Library/BookCopyTable', component: BookCopyTable, tags: ['autodocs'], args: { copies } } satisfies Meta<typeof BookCopyTable>
export default meta
type Story = StoryObj<typeof meta>

export const Reader: Story = {}
export const Librarian: Story = { args: { canManage: true } }
export const Loading: Story = { args: { loading: true, copies: [] } }
