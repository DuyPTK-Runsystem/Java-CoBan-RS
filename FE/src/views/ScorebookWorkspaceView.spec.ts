import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import router from '@/router'
import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import { ApiError } from '@/types/api'
import ScorebookWorkspaceView from './ScorebookWorkspaceView.vue'

const mocks = vi.hoisted(() => ({
  confirmRequire: vi.fn(),
  fetchAcademicYears: vi.fn(),
  fetchSemesters: vi.fn(),
  fetchSchoolClasses: vi.fn(),
  fetchSubjects: vi.fn(),
  fetchClassSubjects: vi.fn(),
  fetchScorebookByClassSubject: vi.fn(),
  fetchScorebook: vi.fn(),
  fetchScoreGrid: vi.fn(),
  createScorebook: vi.fn(),
  openScorebook: vi.fn(),
  publishScorebook: vi.fn(),
  createAssessmentColumn: vi.fn(),
  updateAssessmentColumn: vi.fn(),
  deactivateAssessmentColumn: vi.fn(),
  upsertSkillWeight: vi.fn(),
  upsertStudentScore: vi.fn(),
  bulkUpsertStudentScores: vi.fn(),
}))

vi.mock('primevue/useconfirm', () => ({ useConfirm: () => ({ require: mocks.confirmRequire }) }))
vi.mock('@/services/academicApi', () => ({
  fetchAcademicYears: mocks.fetchAcademicYears,
  fetchSemesters: mocks.fetchSemesters,
  fetchSchoolClasses: mocks.fetchSchoolClasses,
  fetchSubjects: mocks.fetchSubjects,
  fetchClassSubjects: mocks.fetchClassSubjects,
}))
vi.mock('@/services/scorebookApi', () => ({
  fetchScorebookByClassSubject: mocks.fetchScorebookByClassSubject,
  fetchScorebook: mocks.fetchScorebook,
  fetchScoreGrid: mocks.fetchScoreGrid,
  createScorebook: mocks.createScorebook,
  openScorebook: mocks.openScorebook,
  publishScorebook: mocks.publishScorebook,
  createAssessmentColumn: mocks.createAssessmentColumn,
  updateAssessmentColumn: mocks.updateAssessmentColumn,
  deactivateAssessmentColumn: mocks.deactivateAssessmentColumn,
  upsertSkillWeight: mocks.upsertSkillWeight,
  upsertStudentScore: mocks.upsertStudentScore,
  bulkUpsertStudentScores: mocks.bulkUpsertStudentScores,
}))

const years = [{ id: 1, code: '2026-2027', startDate: '2026-09-01', endDate: '2027-05-31', status: 'ACTIVE' as const, notes: null }]
const semesters = [{ id: 2, academicYearId: 1, code: 'HK1', name: 'Học kỳ 1', displayOrder: 1, startDate: '2026-09-01', endDate: '2026-12-31', automaticLockAt: null, status: 'ACTIVE' as const, lockedAt: null, lockedBy: null, lockReason: null, reopenUntil: null }]
const classes = [{ id: 3, academicYearId: 1, gradeLevelId: 6, classCode: '6A1', className: 'Lớp 6A1', capacity: 35, status: 'ACTIVE' as const }]
const subjects = [{ id: 9, code: 'TOAN', name: 'Toán', subjectType: 'ACADEMIC' as const, applicationScope: 'GRADE' as const, status: 'ACTIVE' as const }]
const classSubjects = [{ id: 20, classId: 3, subjectId: 9, semesterId: 2, status: 'ACTIVE' as const }]
const scorebook = { id: 12, classSubjectId: 20, status: 'OPEN' as const, publishedAt: null, publishedBy: null, closedAt: null, columns: [], skillWeightConfig: null }
const grid = { scorebookId: 12, classSubjectId: 20, scorebookStatus: 'OPEN' as const, columns: [], page: 0, size: 10, totalElements: 21, totalPages: 3, students: [] }

const simpleStub = { template: '<div><slot /></div>' }
const buttonStub = { props: ['label'], template: '<button @click="$emit(\'click\')">{{ label }}</button>' }

function mountView() {
  return mount(ScorebookWorkspaceView, {
    global: {
      plugins: [router],
      stubs: {
        AssessmentColumnDialog: simpleStub,
        AssessmentColumnPanel: simpleStub,
        BulkScoreEntryDialog: simpleStub,
        Button: buttonStub,
        ConfirmDialog: simpleStub,
        FormAlert: { props: ['message'], template: '<div>{{ message }}</div>' },
        ScorebookContextPanel: simpleStub,
        ScorebookStatusHeader: simpleStub,
        ScoreEntryDialog: simpleStub,
        ScoreGrid: simpleStub,
        SkillWeightPanel: simpleStub,
      },
    },
  })
}

describe('ScorebookWorkspaceView', () => {
  beforeEach(async () => {
    clearAuthSession()
    saveAuthSession({
      accessToken: 'teacher-token',
      user: { id: 5, username: 'teacher.demo', roles: ['TEACHER'] },
    })
    Object.values(mocks).forEach((mock) => mock.mockReset())
    mocks.fetchAcademicYears.mockResolvedValue(years)
    mocks.fetchSemesters.mockResolvedValue(semesters)
    mocks.fetchSchoolClasses.mockResolvedValue(classes)
    mocks.fetchSubjects.mockResolvedValue(subjects)
    mocks.fetchClassSubjects.mockResolvedValue(classSubjects)
    mocks.fetchScorebookByClassSubject.mockResolvedValue(scorebook)
    mocks.fetchScorebook.mockResolvedValue(scorebook)
    mocks.fetchScoreGrid.mockImplementation((_token, _id, page = 0, size = 10) =>
      Promise.resolve({ ...grid, page, size }))
    await router.push({ name: 'v2-scorebooks' })
  })

  afterEach(() => clearAuthSession())

  it('looks up the existing scorebook from class-subject and loads page zero', async () => {
    mountView()
    await flushPromises()

    expect(mocks.fetchScorebookByClassSubject).toHaveBeenCalledWith('teacher-token', 20)
    expect(mocks.fetchScoreGrid).toHaveBeenCalledWith('teacher-token', 12, 0, 10)
    expect(mocks.createScorebook).not.toHaveBeenCalled()
  })

  it('loads requested server pages', async () => {
    const wrapper = mountView()
    await flushPromises()
    const view = wrapper.vm as unknown as { changePage: (page: number, size: number) => void }

    view.changePage(2, 20)
    await flushPromises()

    expect(mocks.fetchScoreGrid).toHaveBeenLastCalledWith('teacher-token', 12, 2, 20)
  })

  it('ignores a stale scorebook lookup response after the context changes', async () => {
    const wrapper = mountView()
    await flushPromises()
    let resolveFirst: ((value: typeof scorebook) => void) | undefined
    const firstResponse = new Promise<typeof scorebook>((resolve) => { resolveFirst = resolve })
    const secondScorebook = { ...scorebook, id: 13, classSubjectId: 22 }
    mocks.fetchScorebookByClassSubject.mockImplementation((_token, classSubjectId) =>
      classSubjectId === 21 ? firstResponse : Promise.resolve(secondScorebook))
    const view = wrapper.vm as unknown as {
      loading: boolean
      selectedClassSubjectId: number | null
      scorebook: typeof scorebook | null
      lookupSelectedScorebook: () => Promise<void>
    }
    view.loading = true
    view.selectedClassSubjectId = 21
    const firstLookup = view.lookupSelectedScorebook()
    view.selectedClassSubjectId = 22
    const secondLookup = view.lookupSelectedScorebook()

    await secondLookup
    resolveFirst?.(scorebook)
    await firstLookup

    expect(view.scorebook?.id).toBe(13)
  })

  it('treats lookup 404 as an empty state without duplicate create', async () => {
    mocks.fetchScorebookByClassSubject.mockRejectedValue(new ApiError(404, 'Chưa có sổ điểm'))
    const wrapper = mountView()
    await flushPromises()
    const view = wrapper.vm as unknown as { lookupState: string; canCreate: boolean }

    expect(view.lookupState).toBe('empty')
    expect(view.canCreate).toBe(false)
    expect(mocks.createScorebook).not.toHaveBeenCalled()
  })

  it('allows an academic office session to create an absent scorebook', async () => {
    clearAuthSession()
    saveAuthSession({
      accessToken: 'office-token',
      user: { id: 2, username: 'office.demo', roles: ['ACADEMIC_OFFICE'] },
    })
    mocks.fetchScorebookByClassSubject.mockRejectedValue(new ApiError(404, 'Chưa có sổ điểm'))
    mocks.createScorebook.mockResolvedValue(scorebook)
    const wrapper = mountView()
    await flushPromises()
    const view = wrapper.vm as unknown as { canCreate: boolean; create: () => Promise<void> }

    expect(view.canCreate).toBe(true)
    await view.create()

    expect(mocks.createScorebook).toHaveBeenCalledWith('office-token', { classSubjectId: 20 })
  })

  it('reloads authoritative metadata and sets conflictMessage from API after a 409', async () => {
    const wrapper = mountView()
    await flushPromises()
    const view = wrapper.vm as unknown as {
      handleConflict: (error: unknown) => Promise<boolean>
      conflictMessage: string
      dialogError: string
    }

    await view.handleConflict(new ApiError(409, 'Môn thường chỉ được phép có đúng một cột KTCK'))

    expect(view.conflictMessage).toBe('Môn thường chỉ được phép có đúng một cột KTCK')
    expect(view.dialogError).toBe('Môn thường chỉ được phép có đúng một cột KTCK')
    expect(mocks.fetchScorebook).toHaveBeenCalledWith('teacher-token', 12)
    expect(mocks.fetchScoreGrid.mock.calls.length).toBeGreaterThan(1)
  })

  it('requires confirmation before publish', async () => {
    const wrapper = mountView()
    await flushPromises()
      ; (wrapper.vm as unknown as { confirmPublish: () => void }).confirmPublish()

    expect(mocks.confirmRequire).toHaveBeenCalledWith(expect.objectContaining({
      header: 'Xác nhận công bố sổ điểm',
      accept: expect.any(Function),
    }))
  })

  it('requires confirmation before deactivating a column', async () => {
    const wrapper = mountView()
    await flushPromises()
    const column = {
      id: 7,
      scorebookId: 12,
      assessmentType: 'KTTT' as const,
      columnNo: 1,
      columnName: 'Thường xuyên 1',
      weightFactor: null,
      required: false,
      status: 'ACTIVE' as const,
    }
      ; (wrapper.vm as unknown as { confirmDeactivateColumn: (value: typeof column) => void })
        .confirmDeactivateColumn(column)

    expect(mocks.confirmRequire).toHaveBeenCalledWith(expect.objectContaining({
      header: 'Xác nhận vô hiệu hóa cột',
      accept: expect.any(Function),
    }))
  })
})
