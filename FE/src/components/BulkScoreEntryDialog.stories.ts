import type { Meta, StoryObj } from '@storybook/vue3'

import BulkScoreEntryDialog from './BulkScoreEntryDialog.vue'

const meta = {
  title: 'Scorebook/BulkScoreEntryDialog',
  component: BulkScoreEntryDialog,
  tags: ['autodocs'],
  args: {
    visible: true,
    column: { columnId: 7, assessmentType: 'KTĐK', columnNo: 1, columnName: 'Giữa kỳ' },
    students: [
      { studentId: 11, studentCode: 'HS001', studentName: 'Nguyễn Minh An', scores: {} },
      {
        studentId: 12,
        studentCode: 'HS002',
        studentName: 'Trần Khánh Linh',
        scores: {
          '7': {
            scoreId: 2,
            assessmentColumnId: 7,
            studentId: 12,
            studentCode: 'HS002',
            studentName: 'Trần Khánh Linh',
            scoreStatus: 'ABSENT',
            scoreValue: null,
            note: null,
            enteredBy: 5,
            enteredAt: '2026-09-02T08:00:00',
            updatedBy: null,
            updatedAt: null,
            version: 2,
          },
        },
      },
    ],
    saving: false,
    errorMessage: '',
  },
} satisfies Meta<typeof BulkScoreEntryDialog>

export default meta
type Story = StoryObj<typeof meta>

export const CurrentPage: Story = {}
export const Saving: Story = { args: { saving: true } }

