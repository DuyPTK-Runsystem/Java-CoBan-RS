import type { Meta, StoryObj } from '@storybook/vue3'

import type { GradeLevel, Semester, Subject } from '@/types/academic'
import SubjectApplicabilityDialog from './SubjectApplicabilityDialog.vue'

const subject: Subject = { id: 101, code: 'MAT', name: 'Toán', subjectType: 'ACADEMIC', applicationScope: 'GRADE', status: 'ACTIVE' }
const semesters: Semester[] = [{ id: 11, academicYearId: 1, code: 'HK1', name: 'HK1 · 2026-2027', displayOrder: 1, startDate: '2026-09-01', endDate: '2026-12-31', automaticLockAt: null, status: 'ACTIVE', lockedAt: null, lockedBy: null, lockReason: null, reopenUntil: null }]
const grades: GradeLevel[] = [{ id: 1, code: 'GRADE_6', name: 'Khối 6', gradeLevel: 6, displayOrder: 1, nextGradeId: null, active: true, description: null }]
const meta = { title: 'AcademicCatalog/SubjectApplicabilityDialog', component: SubjectApplicabilityDialog, tags: ['autodocs'], args: { visible: true, subject, semesters, grades }, parameters: { layout: 'fullscreen' } } satisfies Meta<typeof SubjectApplicabilityDialog>
export default meta
type Story = StoryObj<typeof meta>

export const CreateGradeScope: Story = {}
export const CreatedResultAndContractGap: Story = { args: { createdApplicability: 'Đã tạo cấu hình áp dụng cho Toán · Khối 6 · HK1.' } }
export const Conflict: Story = { args: { errorMessage: 'Cấu hình áp dụng bị trùng trong học kỳ đã chọn.' } }
