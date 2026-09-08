import type { Meta, StoryObj } from '@storybook/vue3'

import type { Subject } from '@/types/academic'
import SubjectDialog from './SubjectDialog.vue'

const subject: Subject = { id: 101, code: 'MAT', name: 'Toán', subjectType: 'ACADEMIC', applicationScope: 'GRADE', status: 'ACTIVE' }
const meta = { title: 'AcademicCatalog/SubjectDialog', component: SubjectDialog, tags: ['autodocs'], args: { visible: true }, parameters: { layout: 'fullscreen' } } satisfies Meta<typeof SubjectDialog>
export default meta
type Story = StoryObj<typeof meta>

export const Create: Story = {}
export const Edit: Story = { args: { mode: 'edit', initialValue: subject } }
export const SkillForClass: Story = { args: { mode: 'create', initialValue: { subjectType: 'SKILL', applicationScope: 'CLASS' } } }
export const BackendConflict: Story = { args: { mode: 'edit', initialValue: subject, errorMessage: 'Mã môn đã tồn tại hoặc môn không còn được phép cập nhật.' } }
