import type { Meta, StoryObj } from '@storybook/vue3'

import type { GradeLevel } from '@/types/academic'
import GradeDialog from './GradeDialog.vue'

const grades: GradeLevel[] = [
  { id: 1, code: 'GRADE_6', name: 'Khối 6', gradeLevel: 6, displayOrder: 1, nextGradeId: 2, active: true, description: 'Đầu cấp THCS' },
  { id: 2, code: 'GRADE_7', name: 'Khối 7', gradeLevel: 7, displayOrder: 2, nextGradeId: null, active: true, description: null },
]

const meta = { title: 'AcademicCatalog/GradeDialog', component: GradeDialog, tags: ['autodocs'], args: { visible: true, grades }, parameters: { layout: 'fullscreen' } } satisfies Meta<typeof GradeDialog>
export default meta
type Story = StoryObj<typeof meta>

export const Create: Story = { args: { mode: 'create' } }
export const Edit: Story = { args: { mode: 'edit', initialValue: grades[0] } }
export const ValidationConflict: Story = { args: { mode: 'edit', initialValue: { ...grades[0], nextGradeId: 1 }, errorMessage: 'Khối đang được tham chiếu nên chưa thể xóa.' } }
