import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import TeachingAssignmentView from './TeachingAssignmentView.vue'

const mocks = vi.hoisted(() => ({
  fetchTeachers: vi.fn(),
  fetchSubjectAssignmentsByTeacher: vi.fn(),
  fetchSubjectAssignmentsByClass: vi.fn(),
  fetchHomeroomAssignmentsByTeacher: vi.fn(),
  fetchAcademicYears: vi.fn(),
  fetchGrades: vi.fn(),
  fetchSchoolClasses: vi.fn(),
  fetchSemesters: vi.fn(),
  replace: vi.fn(),
}))

vi.mock('@/services/teacherApi', () => ({ fetchTeachers: mocks.fetchTeachers }))
vi.mock('@/services/assignmentApi', () => ({ fetchSubjectAssignmentsByTeacher: mocks.fetchSubjectAssignmentsByTeacher, fetchSubjectAssignmentsByClass: mocks.fetchSubjectAssignmentsByClass, fetchHomeroomAssignmentsByTeacher: mocks.fetchHomeroomAssignmentsByTeacher, fetchHomeroomAssignmentsByClass: vi.fn() }))
vi.mock('@/services/academicApi', () => ({
  fetchAcademicYears: mocks.fetchAcademicYears, fetchClassSubjects: vi.fn(), fetchGrades: mocks.fetchGrades, fetchSchoolClasses: mocks.fetchSchoolClasses, fetchSemesters: mocks.fetchSemesters, fetchSubjects: vi.fn(),
}))
vi.mock('vue-router', () => ({ useRouter: () => ({ currentRoute: { value: { query: {} } }, replace: mocks.replace }) }))

const buttonStub = { props: ['label'], template: '<button>{{ label }}</button>' }
const noopStub = { template: '<div />' }

describe('TeachingAssignmentView', () => {
  beforeEach(() => {
    clearAuthSession()
    saveAuthSession({ accessToken: 'teacher-token', user: { id: 5, username: 'teacher.demo', roles: ['TEACHER'] } })
    mocks.fetchTeachers.mockReset().mockResolvedValue([{ id: 20, userId: 5, teacherCode: 'GV020', teacherName: 'Nguyễn Văn A', dateOfBirth: null, gender: null, phone: null, email: null, department: null, joinDate: null, status: 'ACTIVE' }])
    mocks.fetchSubjectAssignmentsByTeacher.mockReset().mockResolvedValue([])
    mocks.fetchSubjectAssignmentsByClass.mockReset().mockResolvedValue([])
    mocks.fetchHomeroomAssignmentsByTeacher.mockReset().mockResolvedValue([])
    mocks.fetchAcademicYears.mockReset().mockResolvedValue([{ id: 1, code: '2026-2027', startDate: '2026-09-01', endDate: '2027-05-31', status: 'ACTIVE' }])
    mocks.fetchGrades.mockReset().mockResolvedValue([])
    mocks.fetchSchoolClasses.mockReset().mockResolvedValue([])
    mocks.fetchSemesters.mockReset().mockResolvedValue([])
  })

  it('opens Teacher in own read-only schedule and does not load class context', async () => {
    const wrapper = mount(TeachingAssignmentView, {
      global: {
        stubs: {
          Button: buttonStub,
          FormAlert: noopStub,
          PageState: noopStub,
          AssignmentContextPanel: noopStub,
          HomeroomAssignmentCard: noopStub,
          HomeroomHistoryDialog: noopStub,
          ClassSubjectAssignmentTable: noopStub,
          TeacherAssignmentScheduleTable: noopStub,
          HomeroomAssignmentDialog: noopStub,
          SubjectAssignmentDialog: noopStub,
          Select: noopStub,
        },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Phân công của tôi')
    expect(wrapper.text()).not.toContain('Phân công theo lớp')
    expect(mocks.fetchSubjectAssignmentsByTeacher).toHaveBeenCalledWith('teacher-token', 20)
    expect(mocks.fetchTeachers).toHaveBeenCalledTimes(1)
  })

  it('exports the teaching assignment route view', () => {
    expect(TeachingAssignmentView).toBeTruthy()
  })
})
