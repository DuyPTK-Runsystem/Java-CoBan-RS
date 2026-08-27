import type { Meta, StoryObj } from '@storybook/vue3'

import type { ClassSubject, Semester, Subject } from '@/types/academic'
import ClassSubjectTable from './ClassSubjectTable.vue'

const subjects: Subject[] = [
  { id: 101, code: 'MAT', name: 'Toán', subjectType: 'ACADEMIC', applicationScope: 'GRADE', status: 'ACTIVE' },
  { id: 108, code: 'SKL', name: 'Kỹ năng sống', subjectType: 'SKILL', applicationScope: 'CLASS', status: 'ACTIVE' },
]
const classSubjects: ClassSubject[] = [
  { id: 1001, classId: 101, subjectId: 101, semesterId: 11, status: 'ACTIVE' },
  { id: 1002, classId: 101, subjectId: 108, semesterId: 11, status: 'INACTIVE' },
  { id: 1003, classId: 101, subjectId: 999, semesterId: 11, status: 'COMPLETED' },
]
const semesters: Semester[] = [{ id: 11, academicYearId: 1, code: 'HK1', name: 'Học kỳ I', displayOrder: 1, startDate: '2026-09-01', endDate: '2026-12-31', automaticLockAt: null, status: 'ACTIVE', lockedAt: null, lockedBy: null, lockReason: null, reopenUntil: null }]

const meta = { title: 'AcademicCatalog/ClassSubjectTable', component: ClassSubjectTable, tags: ['autodocs'], args: { classSubjects, subjects, semesters }, parameters: { layout: 'padded' } } satisfies Meta<typeof ClassSubjectTable>
export default meta
type Story = StoryObj<typeof meta>

export const StatusAndMissingSubjectFallback: Story = {}
export const Loading: Story = { args: { loading: true } }
export const Empty: Story = { args: { classSubjects: [] } }
export const ClosedReadOnly: Story = { args: { readOnly: true, classSubjects: classSubjects.slice(0, 2) } }
