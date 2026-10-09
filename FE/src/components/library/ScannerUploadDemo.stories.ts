import type { Meta, StoryObj } from '@storybook/vue3-vite'

import ScannerUploadDemo from './ScannerUploadDemo.vue'

const meta = {
  title: 'Library/ScannerUploadDemo',
  component: ScannerUploadDemo,
  args: {
    id: 'patron-card-scan',
    label: 'Ảnh QR thẻ hoặc mã bạn đọc',
  },
} satisfies Meta<typeof ScannerUploadDemo>

export default meta
type Story = StoryObj<typeof meta>

export const PatronCard: Story = {}

export const BookBarcode: Story = {
  args: {
    id: 'book-barcode-scan',
    label: 'Ảnh barcode sách',
    formats: ['code_128'],
  },
}
