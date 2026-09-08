import type { Meta, StoryObj } from '@storybook/vue3'
import TeacherTable from './TeacherTable.vue'
const teachers = [{ id: 1, userId: 2, teacherCode: 'GV001', teacherName: 'Nguyễn Văn An', dateOfBirth: '1985-05-15', gender: 'Nam', phone: '0912345678', email: 'an@school.edu', department: 'Toán - Tin', joinDate: '2015-09-01', status: 'ACTIVE' as const }]
const meta = { title: 'Teacher/TeacherTable', component: TeacherTable, args: { teachers }, parameters: { layout: 'padded' } } satisfies Meta<typeof TeacherTable>
export default meta; type Story = StoryObj<typeof meta>; export const Loaded: Story = {}; export const Empty: Story = { args: { teachers: [] } }; export const Loading: Story = { args: { loading: true } }
