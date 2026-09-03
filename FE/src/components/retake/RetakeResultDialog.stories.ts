import type { Meta, StoryObj } from '@storybook/vue3'

import RetakeResultDialog from './RetakeResultDialog.vue'
import type { RetakeRowItem } from '@/types/retake'

const sampleStudents = [
  { id: 1001, code: 'HS0001', name: 'Nguyễn Minh An' },
  { id: 1002, code: 'HS0002', name: 'Trần Gia Bảo' },
]

const sampleYears = [
  { id: 1, code: '2026–2027' },
]

const sampleSubjects = [
  { id: 21, name: 'Toán' },
  { id: 22, name: 'Ngữ văn' },
  { id: 23, name: 'Vật lý' },
]

const sampleItem: RetakeRowItem = {
  retakeId: 7001,
  studentId: 1001,
  studentCode: 'HS0001',
  studentName: 'Nguyễn Minh An',
  academicYearId: 1,
  academicYearCode: '2026–2027',
  subjectId: 21,
  subjectName: 'Toán',
  preRetakeScore: 4.0,
  retakeScore: 6.5,
  officialDtbmhCn: 6.5,
  examDate: '2027-06-15',
  status: 'SCORED',
  calculationStatus: 'FINISH',
  calculationSource: 'RETAKE',
  lastTaskId: 8801,
  note: 'Điểm đã đối chiếu biên bản.',
}

const meta = {
  title: 'Scorebook/RetakeResultDialog',
  component: RetakeResultDialog,
  tags: ['autodocs'],
  args: {
    visible: true,
    mode: 'create',
    students: sampleStudents,
    academicYears: sampleYears,
    subjects: sampleSubjects,
    item: null,
    saving: false,
    errorMessage: '',
  },
} satisfies Meta<typeof RetakeResultDialog>

export default meta
type Story = StoryObj<typeof meta>

export const CreatePlanned: Story = {}

export const ScoreEntry: Story = {
  args: {
    mode: 'score',
    item: {
      ...sampleItem,
      retakeScore: null,
      officialDtbmhCn: null,
      calculationStatus: null,
    },
  },
}

export const BeforeAfterScored: Story = {
  args: {
    mode: 'score',
    item: sampleItem,
  },
}

export const CalculationInProgress: Story = {
  args: {
    mode: 'score',
    item: {
      ...sampleItem,
      calculationStatus: 'IN_PROGRESS',
      officialDtbmhCn: null,
    },
  },
}

export const ValidationError: Story = {
  args: {
    mode: 'score',
    item: {
      ...sampleItem,
      retakeScore: 12.5,
    },
    errorMessage: 'Điểm phải nằm trong 0.0–10.0 và tối đa 1 chữ số thập phân; note không quá 1000 ký tự.',
  },
}

export const CancelConfirm: Story = {
  args: {
    mode: 'cancel',
    item: sampleItem,
  },
}

export const ConflictError: Story = {
  args: {
    mode: 'create',
    errorMessage: '409 Conflict: Record cùng student/year/subject đã tồn tại hoặc lifecycle không cho phép thao tác.',
  },
}

export const ForbiddenError: Story = {
  args: {
    mode: 'score',
    item: sampleItem,
    errorMessage: '403 Forbidden: Bạn không có quyền quản lý kỳ thi lại.',
  },
}

export const NotFoundError: Story = {
  args: {
    mode: 'score',
    item: sampleItem,
    errorMessage: '404 Not Found: Kỳ thi lại không tồn tại hoặc đã không còn truy cập được.',
  },
}

export const CancelledReadOnly: Story = {
  args: {
    mode: 'score',
    item: {
      ...sampleItem,
      status: 'CANCELLED',
      retakeScore: null,
      officialDtbmhCn: null,
      calculationStatus: null,
    },
  },
}
