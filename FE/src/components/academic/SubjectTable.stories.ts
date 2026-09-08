import type { Meta, StoryObj } from '@storybook/vue3'

import type { Subject } from '@/types/academic'
import SubjectTable from './SubjectTable.vue'

const subjects: Subject[] = [
  { id: 101, code: 'MAT', name: 'Toán', subjectType: 'ACADEMIC', applicationScope: 'GRADE', status: 'ACTIVE' },
  { id: 108, code: 'SKL', name: 'Kỹ năng sống', subjectType: 'SKILL', applicationScope: 'CLASS', status: 'ACTIVE' },
  { id: 109, code: 'MUS', name: 'Âm nhạc', subjectType: 'ACADEMIC', applicationScope: 'GRADE', status: 'INACTIVE' },
]

const meta = { title: 'AcademicCatalog/SubjectTable', component: SubjectTable, tags: ['autodocs'], args: { subjects }, parameters: { layout: 'padded' } } satisfies Meta<typeof SubjectTable>
export default meta
type Story = StoryObj<typeof meta>

export const TypeScopeAndStatusMatrix: Story = {}
export const Loading: Story = { args: { loading: true } }
export const Empty: Story = { args: { subjects: [] } }
