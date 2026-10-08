import type { Meta, StoryObj } from '@storybook/vue3'

import type { BookCopy } from '@/types/library/catalog'
import BookBarcodeDialog from './BookBarcodeDialog.vue'

const copy: BookCopy = { id: 1, bookId: 101, barcode: 'LIB-000000001', shelfLocation: 'A1-02', status: 'AVAILABLE', referenceOnly: false, version: 0, createdAt: '2026-10-08T08:00:00' }
const meta = { title: 'Library/BookBarcodeDialog', component: BookBarcodeDialog, tags: ['autodocs'], args: { visible: true, copy } } satisfies Meta<typeof BookBarcodeDialog>
export default meta
type Story = StoryObj<typeof meta>

export const Loading: Story = { args: { loading: true } }
export const RetryableError: Story = { args: { errorMessage: 'Không thể tạo ảnh mã vạch.' } }
