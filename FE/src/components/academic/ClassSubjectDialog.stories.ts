import type { Meta, StoryObj } from '@storybook/vue3'

import type { ClassSubject, Subject } from '@/types/academic'
import ClassSubjectDialog from './ClassSubjectDialog.vue'

const subjects: Subject[] = [
  { id: 101, code: 'MAT', name: 'Toán', subjectType: 'ACADEMIC', applicationScope: 'GRADE', status: 'ACTIVE' },
  { id: 108, code: 'SKL', name: 'Kỹ năng sống', subjectType: 'SKILL', applicationScope: 'CLASS', status: 'ACTIVE' },
]
const classSubject: ClassSubject = { id: 1001, classId: 101, subjectId: 101, semesterId: 11, status: 'ACTIVE' }
const meta = { title: 'AcademicCatalog/ClassSubjectDialog', component: ClassSubjectDialog, tags: ['autodocs'], args: { visible: true, availableSubjects: subjects, classLabel: '6A1 · Khối 6', semesterLabel: 'HK1 · 2026-2027' }, parameters: { layout: 'fullscreen' } } satisfies Meta<typeof ClassSubjectDialog>
export default meta
type Story = StoryObj<typeof meta>

export const Create: Story = {}
export const ChangeStatus: Story = { args: { mode: 'edit', initialValue: classSubject } }
export const ClosedReadOnly: Story = { args: { mode: 'edit', initialValue: classSubject, classClosed: true } }
export const ApplicabilityConflict: Story = { args: { conflictMessage: 'Môn chưa có applicability cho lớp/khối và học kỳ này. Backend trả 409 khi gán.', errorMessage: 'Không thể tạo class-subject.' } }
