import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import router from '@/router'
import UnassignedStudentTable from '@/components/UnassignedStudentTable.vue'
import EnrollmentMutationDialog from '@/components/EnrollmentMutationDialog.vue'
import EnrollmentListView from './EnrollmentListView.vue'

const mocks = vi.hoisted(() => ({
  fetchAcademicYears: vi.fn(),
  fetchGrades: vi.fn(),
  fetchSchoolClasses: vi.fn(),
  fetchUnassignedStudents: vi.fn(),
  fetchClassStudents: vi.fn(),
  createBulkEnrollment: vi.fn(),
  createEnrollment: vi.fn(),
  transferEnrollment: vi.fn(),
  fetchStudentEnrollmentHistory: vi.fn(),
}))

vi.mock('@/services/academicApi', () => ({ fetchAcademicYears: mocks.fetchAcademicYears, fetchGrades: mocks.fetchGrades, fetchSchoolClasses: mocks.fetchSchoolClasses }))
vi.mock('@/services/enrollmentApi', () => ({
  fetchUnassignedStudents: mocks.fetchUnassignedStudents,
  fetchClassStudents: mocks.fetchClassStudents,
  createBulkEnrollment: mocks.createBulkEnrollment,
  createEnrollment: mocks.createEnrollment,
  transferEnrollment: mocks.transferEnrollment,
  fetchStudentEnrollmentHistory: mocks.fetchStudentEnrollmentHistory,
}))

const academicYears = [{ id: 1, code: '2026-2027', startDate: '2026-09-01', endDate: '2027-05-31', status: 'ACTIVE' as const, notes: null }]
const grades = [{ id: 1, code: 'GRADE_6', name: 'Khối 6', gradeLevel: 6 as const, displayOrder: 1, nextGradeId: 2, active: true, description: null }]
const classes = [
  { id: 101, academicYearId: 1, gradeLevelId: 1, classCode: '6A1', className: 'Lớp 6A1', capacity: 35, status: 'ACTIVE' as const },
  { id: 102, academicYearId: 1, gradeLevelId: 1, classCode: '6A2', className: 'Lớp 6A2', capacity: 35, status: 'ACTIVE' as const },
]
const students = [{ studentId: 11, studentCode: 'HS011', studentName: 'Nguyễn An' }, { studentId: 12, studentCode: 'HS012', studentName: 'Trần Bình' }]

const buttonStub = { props: ['label', 'disabled'], emits: ['click'], template: '<button :disabled="disabled" @click="$emit(\'click\')">{{ label }}</button>' }
const pageStateStub = { template: '<div><slot /></div>' }
const formAlertStub = { props: ['message'], template: '<div>{{ message }}</div>' }
const contextStub = { props: ['academicYearId', 'classId'], template: '<div data-testid="context-panel" />' }
const warningStub = { template: '<div data-testid="capacity-warning" />' }
const unassignedStub = { emits: ['update:selected-students', 'place', 'history'], template: '<div data-testid="unassigned-table" />' }
const rosterStub = { template: '<div data-testid="roster-table" />' }
const mutationStub = { emits: ['submit'], template: '<div data-testid="mutation-dialog" />' }
const transferStub = { template: '<div data-testid="transfer-dialog" />' }
const historyStub = { template: '<div data-testid="history-dialog" />' }

function mountView() {
  return mount(EnrollmentListView, {
    global: {
      plugins: [router],
      stubs: {
        Button: buttonStub,
        CapacityWarningBanner: warningStub,
        ClassStudentTable: rosterStub,
        EnrollmentContextPanel: contextStub,
        EnrollmentMutationDialog: mutationStub,
        FormAlert: formAlertStub,
        PageState: pageStateStub,
        StudentEnrollmentHistoryDialog: historyStub,
        TransferEnrollmentDialog: transferStub,
        UnassignedStudentTable: unassignedStub,
      },
    },
  })
}

describe('EnrollmentListView', () => {
  beforeEach(async () => {
    clearAuthSession()
    saveAuthSession({ accessToken: 'jwt-token', user: { id: 1, username: 'academic.admin' } })
    mocks.fetchAcademicYears.mockReset().mockResolvedValue(academicYears)
    mocks.fetchGrades.mockReset().mockResolvedValue(grades)
    mocks.fetchSchoolClasses.mockReset().mockResolvedValue(classes)
    mocks.fetchUnassignedStudents.mockReset().mockResolvedValue(students)
    mocks.fetchClassStudents.mockReset().mockResolvedValue([{ studentId: 21, studentCode: 'HS021', studentName: 'Lê Chi', enrollmentId: 701 }])
    mocks.createBulkEnrollment.mockReset().mockResolvedValue({ enrollments: [], warnings: [] })
    mocks.createEnrollment.mockReset()
    mocks.transferEnrollment.mockReset().mockResolvedValue({ enrollments: [], warnings: [] })
    mocks.fetchStudentEnrollmentHistory.mockReset()
    await router.push({ name: 'v2-enrollments' })
  })

  afterEach(() => clearAuthSession())

  it('loads active academic-year context, unassigned students and roster', async () => {
    mountView()
    await flushPromises()

    expect(mocks.fetchAcademicYears).toHaveBeenCalledWith('jwt-token')
    expect(mocks.fetchAcademicYears).toHaveBeenCalledTimes(1)
    expect(mocks.fetchGrades).toHaveBeenCalledWith('jwt-token')
    expect(mocks.fetchGrades).toHaveBeenCalledTimes(1)
    expect(mocks.fetchSchoolClasses).toHaveBeenCalledWith('jwt-token', 1)
    expect(mocks.fetchSchoolClasses).toHaveBeenCalledTimes(1)
    expect(mocks.fetchUnassignedStudents).toHaveBeenCalledWith('jwt-token', 1)
    expect(mocks.fetchUnassignedStudents).toHaveBeenCalledTimes(1)
    expect(mocks.fetchClassStudents).toHaveBeenCalledWith('jwt-token', 101)
  })

  it('sends selected student ids for bulk placement and reloads both lists', async () => {
    const wrapper = mountView()
    await flushPromises()
    const unassigned = wrapper.findComponent(UnassignedStudentTable)

    unassigned.vm.$emit('update:selected-students', students)
    const view = wrapper.vm as unknown as { openBulkPlacement: () => void }
    view.openBulkPlacement()
    await flushPromises()
    wrapper.findComponent(EnrollmentMutationDialog).vm.$emit('submit', { studentIds: [11, 12], enrolledAt: '' })
    await flushPromises()

    expect(mocks.createBulkEnrollment).toHaveBeenCalledWith('jwt-token', { academicYearId: 1, classId: 101, studentIds: [11, 12], enrolledAt: null })
    expect(mocks.fetchUnassignedStudents.mock.calls.length).toBeGreaterThan(1)
    expect(mocks.fetchClassStudents.mock.calls.length).toBeGreaterThan(1)
  })

  it('shows student and source/target class names after transfer', async () => {
    const wrapper = mountView()
    await flushPromises()
    const view = wrapper.vm as unknown as {
      openTransfer: (student: { studentId: number; studentCode: string; studentName: string; enrollmentId: number }) => void
      submitTransfer: (values: { targetClassId: number; effectiveAt: string; reason: string }) => Promise<void>
    }

    view.openTransfer({ studentId: 21, studentCode: 'STU2600001', studentName: 'Lê Chi', enrollmentId: 701 })
    await view.submitTransfer({ targetClassId: 102, effectiveAt: '2026-08-28T09:00:00', reason: '' })
    await flushPromises()

    expect(wrapper.text()).toContain('Đã chuyển STU2600001-Lê Chi từ lớp Lớp 6A1 sang Lớp 6A2.')
  })
})
