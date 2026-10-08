import type { Meta, StoryObj } from '@storybook/vue3'

import AddBookCopiesDialog from './AddBookCopiesDialog.vue'

const meta = { title: 'Library/AddBookCopiesDialog', component: AddBookCopiesDialog, tags: ['autodocs'], args: { visible: true } } satisfies Meta<typeof AddBookCopiesDialog>
export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}
export const Saving: Story = { args: { saving: true } }
export const RequestError: Story = { args: { errorMessage: 'Không thể thêm bản sao. Hãy gửi lại cùng thông tin để giữ nguyên yêu cầu.' } }
