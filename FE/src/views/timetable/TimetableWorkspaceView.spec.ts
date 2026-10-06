import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TimetableWorkspaceView from './TimetableWorkspaceView.vue'

const mocks = vi.hoisted(() => ({
  requireAccessToken: vi.fn(),
  getTimetableDetail: vi.fn(),
  fetchSemesters: vi.fn(),
  fetchSchoolClasses: vi.fn(),
  fetchTeachers: vi.fn(),
  lookupFunctionalRooms: vi.fn(),
  getTimetablePeriods: vi.fn(),
  getTimetableEntries: vi.fn(),
  getTimetableReview: vi.fn(),
  listTeacherUnavailabilities: vi.fn(),
  fetchSubjectAssignmentsByClass: vi.fn(),
  useConfirm: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { timetableId: '42' } }),
  useRouter: () => ({ push: vi.fn() }),
}))
vi.mock('primevue/useconfirm', () => ({ useConfirm: mocks.useConfirm }))
vi.mock('@/composables/useAuthSession', () => ({
  useAuthSession: () => ({ roles: { value: ['ADMIN'] }, requireAccessToken: mocks.requireAccessToken }),
}))
vi.mock('@/services/academicApi', () => ({
  fetchSemesters: mocks.fetchSemesters,
  fetchSchoolClasses: mocks.fetchSchoolClasses,
}))
vi.mock('@/services/teacherApi', () => ({ fetchTeachers: mocks.fetchTeachers }))
vi.mock('@/services/functionalRoomApi', () => ({ lookupFunctionalRooms: mocks.lookupFunctionalRooms }))
vi.mock('@/services/assignmentApi', () => ({ fetchSubjectAssignmentsByClass: mocks.fetchSubjectAssignmentsByClass }))
vi.mock('@/services/teacherUnavailabilityApi', () => ({ listTeacherUnavailabilities: mocks.listTeacherUnavailabilities }))
vi.mock('@/services/timetableApi', () => ({
  getTimetableDetail: mocks.getTimetableDetail,
  getTimetablePeriods: mocks.getTimetablePeriods,
  getTimetableEntries: mocks.getTimetableEntries,
  getTimetableReview: mocks.getTimetableReview,
  createTimetableRevision: vi.fn(),
  publishTimetableRevision: vi.fn(),
  updateTimetableEntries: vi.fn(),
  validateTimetableRevision: vi.fn(),
}))

const timetableDetail = {
  timetableId: 42,
  semesterId: 7,
  semesterName: 'HK1 2026 - 2027',
  revisionId: 43,
  revisionNumber: 1,
  status: 'DRAFT',
  effectiveFrom: '2026-10-02',
  effectiveTo: '2026-12-31',
  version: 1,
  headVersion: 1,
  blockingCount: 0,
  warningCount: 0,
  canUseTimetableAgent: true,
  capabilities: { canEdit: true, canPublish: false, canRevise: false },
}

beforeEach(() => {
  vi.clearAllMocks()
  mocks.requireAccessToken.mockReturnValue('session-token')
  mocks.useConfirm.mockReturnValue({ require: vi.fn() })
  mocks.getTimetableDetail.mockResolvedValue(timetableDetail)
  mocks.fetchSemesters.mockResolvedValue([
    { id: 6, academicYearId: 2025, name: 'HK2 2025 - 2026' },
    { id: 7, academicYearId: 2026, name: 'HK1 2026 - 2027' },
    { id: 8, academicYearId: 2027, name: 'HK2 2026 - 2027' },
  ])
  mocks.fetchSchoolClasses.mockResolvedValue([])
  mocks.fetchTeachers.mockResolvedValue([])
  mocks.lookupFunctionalRooms.mockResolvedValue([])
  mocks.getTimetablePeriods.mockResolvedValue([])
  mocks.getTimetableEntries.mockResolvedValue([])
  mocks.getTimetableReview.mockResolvedValue({ issues: [] })
  mocks.listTeacherUnavailabilities.mockResolvedValue([])
  mocks.fetchSubjectAssignmentsByClass.mockResolvedValue([])
})

describe('TimetableWorkspaceView class scope for agent picker', () => {
  it('loads classes using the academic year of the timetable semester', async () => {
    shallowMount(TimetableWorkspaceView, {
      global: { stubs: { Button: true, ConfirmDialog: true, Select: true, Tag: true } },
    })
    await flushPromises()

    expect(mocks.fetchSemesters).toHaveBeenCalledWith('session-token')
    expect(mocks.fetchSchoolClasses).toHaveBeenCalledExactlyOnceWith('session-token', 2026)
  })

  it('does not load unfiltered classes when the timetable semester cannot be resolved', async () => {
    mocks.fetchSemesters.mockResolvedValue([{ id: 6, academicYearId: 2025, name: 'HK2 2025 - 2026' }])
    shallowMount(TimetableWorkspaceView, {
      global: { stubs: { Button: true, ConfirmDialog: true, Select: true, Tag: true } },
    })
    await flushPromises()

    expect(mocks.fetchSchoolClasses).not.toHaveBeenCalled()
  })
})
