import type { Meta, StoryObj } from '@storybook/vue3'

import ScoreEntryDialog from './ScoreEntryDialog.vue'

const meta = {
  title: 'Scorebook/ScoreEntryDialog',
  component: ScoreEntryDialog,
  tags: ['autodocs'],
  args: {
    visible: true,
    studentName: 'Nguyễn Minh An',
    score: null,
    saving: false,
    errorMessage: '',
  },
} satisfies Meta<typeof ScoreEntryDialog>

export default meta
type Story = StoryObj<typeof meta>

export const NewScore: Story = {}
export const ExistingZero: Story = {
  args: {
    score: {
      scoreId: 1,
      assessmentColumnId: 7,
      studentId: 11,
      studentCode: 'HS001',
      studentName: 'Nguyễn Minh An',
      scoreStatus: 'SCORED',
      scoreValue: 0,
      note: 'Điểm hợp lệ',
      enteredBy: 5,
      enteredAt: '2026-09-02T08:00:00',
      updatedBy: null,
      updatedAt: null,
      version: 3,
    },
  },
}
export const Conflict: Story = {
  args: { errorMessage: 'Dữ liệu đã thay đổi. Vui lòng kiểm tra phiên bản mới trước khi lưu lại.' },
}

