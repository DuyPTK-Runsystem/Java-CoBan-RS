import type { Meta, StoryObj } from '@storybook/vue3'

import type { BookDetail } from '@/types/library/catalog'
import BookForm from './BookForm.vue'

const book: BookDetail = {
  id: 101,
  isbn: '9786041234567',
  title: 'Dế Mèn phiêu lưu ký',
  author: 'Tô Hoài',
  publisher: 'Kim Đồng',
  publishedYear: 1941,
  category: 'Văn học',
  listPrice: 85000,
  coverUrl: 'https://example.com/de-men.jpg',
  totalCopyCount: 8,
  availableBorrowableCopyCount: 6,
  version: 2,
}

const meta = { title: 'Library/BookForm', component: BookForm, tags: ['autodocs'] } satisfies Meta<typeof BookForm>
export default meta
type Story = StoryObj<typeof meta>

export const Create: Story = {}
export const Edit: Story = { args: { initialValue: book } }
export const BackendValidation: Story = {
  args: {
    initialValue: book,
    errorMessage: 'Một số trường chưa hợp lệ.',
    fieldErrors: { isbn: 'ISBN không hợp lệ hoặc đã được sử dụng.' },
  },
}
