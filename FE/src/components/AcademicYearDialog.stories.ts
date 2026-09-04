import type { Meta, StoryObj } from '@storybook/vue3'

import AcademicYearDialog from './AcademicYearDialog.vue'

const meta = {
  title: 'Academic/AcademicYearDialog',
  component: AcademicYearDialog,
  tags: ['autodocs'],
  args: { visible: true, mode: 'create', saving: false, errorMessage: '' },
  parameters: { layout: 'fullscreen' },
} satisfies Meta<typeof AcademicYearDialog>

export default meta
type Story = StoryObj<typeof meta>

export const Create: Story = {}
export const Edit: Story = {
  args: {
    mode: 'edit',
    initialValue: { id: 1, code: '2026-2027', startDate: '2026-09-01', endDate: '2027-05-31', status: 'ACTIVE', notes: 'Năm học hiện tại' },
  },
}
export const ClosedReadOnly: Story = {
  args: {
    mode: 'edit',
    initialValue: { id: 2, code: '2025-2026', startDate: '2025-09-01', endDate: '2026-05-31', status: 'CLOSED', notes: 'Đã lưu trữ' },
  },
}
export const BackendConflict: Story = {
  args: {
    mode: 'create',
    errorMessage: 'Năm học đã tồn tại hoặc chỉ được phép có một năm học đang hoạt động.',
  },
}

export const InvalidCharacters: Story = {
  args: {
    mode: 'create',
    initialValue: { code: '2026/2027' },
  },
}
