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
  fetchMyEffectiveScorebookAssignments: vi.fn(),
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
  createScoreChangeRequest: vi.fn(),
}))

vi.mock('primevue/useconfirm', () => ({ useConfirm: () => ({ require: mocks.confirmRequire }) }))
vi.mock('@/services/academicApi', () => ({
  fetchAcademicYears: mocks.fetchAcademicYears,
  fetchSemesters: mocks.fetchSemesters,
  fetchSchoolClasses: mocks.fetchSchoolClasses,
  fetchSubjects: mocks.fetchSubjects,
  fetchClassSubjects: mocks.fetchClassSubjects,
}))
vi.mock('@/services/assignmentApi', () => ({
  fetchMyEffectiveScorebookAssignments: mocks.fetchMyEffectiveScorebookAssignments,
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
vi.mock('@/services/scoreChangeRequestApi', () => ({
  createScoreChangeRequest: mocks.createScoreChangeRequest,
}))

const years = [
  { id: 1, code: '2026-2027', startDate: '2026-09-01', endDate: '2027-05-31', status: 'ACTIVE' as const, notes: null },
  { id: 5, code: '2027-2028', startDate: '2027-09-01', endDate: '2028-05-31', status: 'ACTIVE' as const, notes: null },
]
const semesters = [
  { id: 2, academicYearId: 1, code: 'HK1', name: 'Học kỳ 1', displayOrder: 1, startDate: '2026-09-01', endDate: '2026-12-31', automaticLockAt: null, status: 'ACTIVE' as const, lockedAt: null, lockedBy: null, lockReason: null, reopenUntil: null },
  { id: 4, academicYearId: 1, code: 'HK2', name: 'Học kỳ 2', displayOrder: 2, startDate: '2027-01-01', endDate: '2027-05-31', automaticLockAt: null, status: 'ACTIVE' as const, lockedAt: null, lockedBy: null, lockReason: null, reopenUntil: null },
  { id: 6, academicYearId: 5, code: 'HK1', name: 'Học kỳ 1', displayOrder: 1, startDate: '2027-09-01', endDate: '2027-12-31', automaticLockAt: null, status: 'ACTIVE' as const, lockedAt: null, lockedBy: null, lockReason: null, reopenUntil: null },
]
const classes = [
  { id: 3, academicYearId: 1, gradeLevelId: 6, classCode: '6A1', className: 'Lớp 6A1', capacity: 35, status: 'ACTIVE' as const },
  { id: 4, academicYearId: 1, gradeLevelId: 6, classCode: '6A2', className: 'Lớp 6A2', capacity: 35, status: 'ACTIVE' as const },
  { id: 5, academicYearId: 1, gradeLevelId: 6, classCode: '6A3', className: 'Lớp 6A3', capacity: 35, status: 'ACTIVE' as const },
  { id: 8, academicYearId: 5, gradeLevelId: 7, classCode: '7A1', className: 'Lớp 7A1', capacity: 35, status: 'ACTIVE' as const },
]
const subjects = [
  { id: 9, code: 'TOAN', name: 'Toán', subjectType: 'ACADEMIC' as const, applicationScope: 'GRADE' as const, status: 'ACTIVE' as const },
  { id: 10, code: 'VAN', name: 'Văn', subjectType: 'ACADEMIC' as const, applicationScope: 'GRADE' as const, status: 'ACTIVE' as const },
]
const classSubjects = [{ id: 20, classId: 3, subjectId: 9, semesterId: 2, status: 'ACTIVE' as const }]
const assignments = [
  { id: 50, classSubjectId: 20, teacherId: 5, validFrom: '2026-09-01', validTo: null, status: 'ACTIVE' as const, assignedBy: null, classId: 3, className: 'Lớp 6A1', classCode: '6A1', subjectId: 9, subjectName: 'Toán', semesterId: 2, academicYearId: 1 },
  { id: 51, classSubjectId: 21, teacherId: 5, validFrom: '2026-09-01', validTo: null, status: 'ACTIVE' as const, assignedBy: null, classId: 4, className: 'Lớp 6A2', classCode: '6A2', subjectId: 10, subjectName: 'Văn', semesterId: 2, academicYearId: 1 },
  { id: 52, classSubjectId: 22, teacherId: 5, validFrom: '2027-01-01', validTo: null, status: 'ACTIVE' as const, assignedBy: null, classId: 3, className: 'Lớp 6A1', classCode: '6A1', subjectId: 10, subjectName: 'Văn', semesterId: 4, academicYearId: 1 },
  { id: 53, classSubjectId: 23, teacherId: 5, validFrom: '2027-09-01', validTo: null, status: 'ACTIVE' as const, assignedBy: null, classId: 8, className: 'Lớp 7A1', classCode: '7A1', subjectId: 9, subjectName: 'Toán', semesterId: 6, academicYearId: 5 },
]
const scorebook = { id: 12, classSubjectId: 20, status: 'OPEN' as const, publishedAt: null, publishedBy: null, closedAt: null, columns: [], skillWeightConfig: null }
const grid = { scorebookId: 12, classSubjectId: 20, scorebookStatus: 'OPEN' as const, columns: [], page: 0, size: 10, totalElements: 21, totalPages: 3, students: [] }

const simpleStub = { template: '<div><slot /></div>' }
const scoreGridStub = { props: ['readOnly'], template: '<div data-test="score-grid" :data-read-only="String(readOnly)" />' }
const scoreEntryDialogStub = { props: ['readOnly'], template: '<div data-test="score-entry-dialog" :data-read-only="String(readOnly)" />' }
const buttonStub = { props: ['label'], template: '<button @click="$emit(\'click\')">{{ label }}</button>' }
const contextPanelStub = {
  props: ['academicYears', 'semesters', 'classes', 'classSubjects'],
  template: '<div data-test="context-panel" :data-year-ids="academicYears.map(x => x.id).join(\',\')" :data-semester-ids="semesters.map(x => x.id).join(\',\')" :data-class-ids="classes.map(x => x.id).join(\',\')" :data-class-subject-ids="classSubjects.map(x => x.id).join(\',\')" />',
}

function mountView() {
  return mount(ScorebookWorkspaceView, {
    global: {
      plugins: [router],
      stubs: {
        AssessmentColumnDialog: simpleStub,
        AssessmentColumnPanel: simpleStub,
        BulkScoreEntryDialog: simpleStub,
        Dialog: simpleStub,
        Button: buttonStub,
        ConfirmDialog: simpleStub,
        FormAlert: { props: ['message'], template: '<div>{{ message }}</div>' },
        ScorebookContextPanel: contextPanelStub,
        ScorebookStatusHeader: simpleStub,
        ScoreEntryDialog: scoreEntryDialogStub,
        ScoreChangeRequestForm: simpleStub,
        ScoreGrid: scoreGridStub,
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
    mocks.fetchSemesters.mockImplementation((_token, yearId) =>
      Promise.resolve(semesters.filter((semester) => semester.academicYearId === yearId)))
    mocks.fetchSchoolClasses.mockResolvedValue(classes)
    mocks.fetchSubjects.mockResolvedValue(subjects)
    mocks.fetchClassSubjects.mockResolvedValue(classSubjects)
    mocks.fetchMyEffectiveScorebookAssignments.mockResolvedValue(assignments)
    mocks.fetchScorebookByClassSubject.mockResolvedValue(scorebook)
    mocks.fetchScorebook.mockResolvedValue(scorebook)
    mocks.fetchScoreGrid.mockImplementation((_token, _id, page = 0, size = 10) =>
      Promise.resolve({ ...grid, page, size }))
    mocks.openScorebook.mockResolvedValue({ ...scorebook, status: 'OPEN' })
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

  it('uses only the authenticated teacher assignment context to populate dropdown choices', async () => {
    const wrapper = mountView()
    await flushPromises()
    const context = wrapper.get('[data-test="context-panel"]')

    expect(mocks.fetchMyEffectiveScorebookAssignments).toHaveBeenCalledWith('teacher-token')
    expect(mocks.fetchSchoolClasses).toHaveBeenCalledWith('teacher-token', 1)
    expect(mocks.fetchClassSubjects).not.toHaveBeenCalled()
    expect(context.attributes('data-year-ids')).toBe('1,5')
    expect(context.attributes('data-semester-ids')).toBe('2,4')
    expect(context.attributes('data-class-ids')).toBe('3,4')
    expect(context.attributes('data-class-subject-ids')).toBe('20')
  })

  it('resets dependent options and scorebook when the teacher changes semester or class', async () => {
    const wrapper = mountView()
    await flushPromises()
    const view = wrapper.vm as unknown as {
      selectedAcademicYearId: number | null
      selectedSemesterId: number | null
      selectedClassId: number | null
      classSubjects: Array<{ id: number }>
      selectedClassSubjectId: number | null
      scorebook: typeof scorebook | null
    }

    view.selectedSemesterId = 4
    await flushPromises()
    expect(view.classSubjects.map((item) => item.id)).toEqual([22])
    expect(view.selectedClassSubjectId).toBe(22)
    expect(mocks.fetchScorebookByClassSubject).toHaveBeenLastCalledWith('teacher-token', 22)

    view.selectedSemesterId = 2
    await flushPromises()
    view.selectedClassId = 4
    await flushPromises()
    expect(view.classSubjects.map((item) => item.id)).toEqual([21])
    expect(view.selectedClassSubjectId).toBe(21)
    expect(mocks.fetchScorebookByClassSubject).toHaveBeenLastCalledWith('teacher-token', 21)

    view.selectedAcademicYearId = 5
    await flushPromises()
    expect(view.classSubjects.map((item) => item.id)).toEqual([23])
    expect(view.selectedClassSubjectId).toBe(23)
    expect(mocks.fetchScorebookByClassSubject).toHaveBeenLastCalledWith('teacher-token', 23)
  })

  it('shows an empty assignment state and makes no scorebook lookup when teacher has no assignments', async () => {
    mocks.fetchMyEffectiveScorebookAssignments.mockResolvedValue([])
    const wrapper = mountView()
    await flushPromises()
    const view = wrapper.vm as unknown as { academicYears: unknown[]; classes: unknown[]; classSubjects: unknown[]; canCreate: boolean }

    expect(view.academicYears).toEqual([])
    expect(view.canCreate).toBe(false)
    expect(wrapper.text()).not.toContain('Tạo sổ điểm')
    expect(view.classes).toEqual([])
    expect(view.classSubjects).toEqual([])
    expect(wrapper.text()).toContain('Bạn chưa có phân công giảng dạy đang hiệu lực để xem sổ điểm.')
    expect(mocks.fetchScorebookByClassSubject).not.toHaveBeenCalled()
  })

  it('keeps the catalog lookup path for ADMIN', async () => {
    clearAuthSession()
    saveAuthSession({
      accessToken: 'admin-token',
      user: { id: 1, username: 'admin.demo', roles: ['ADMIN'] },
    })
    const wrapper = mountView()
    await flushPromises()

    expect(mocks.fetchMyEffectiveScorebookAssignments).not.toHaveBeenCalled()
    expect(mocks.fetchSemesters).toHaveBeenCalledWith('admin-token', 1)
    expect(mocks.fetchClassSubjects).toHaveBeenCalledWith('admin-token', 3, 2)
    expect(wrapper.vm).toBeTruthy()
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

  it('allows creation for an assigned class-subject when scorebook lookup returns 404', async () => {
    mocks.fetchScorebookByClassSubject.mockRejectedValue(new ApiError(404, 'Chưa có sổ điểm'))
    const wrapper = mountView()
    await flushPromises()
    const view = wrapper.vm as unknown as { lookupState: string; canCreate: boolean }

    expect(view.lookupState).toBe('empty')
    expect(view.canCreate).toBe(true)
    expect(mocks.createScorebook).not.toHaveBeenCalled()
  })

  it('shows the teacher create target and creates then opens an assigned scorebook', async () => {
    mocks.fetchScorebookByClassSubject.mockRejectedValue(new ApiError(404, 'Chưa có sổ điểm'))
    mocks.createScorebook.mockResolvedValue(scorebook)
    const wrapper = mountView()
    await flushPromises()
    const createButton = wrapper.findAll('button').find((button) => button.text() === 'Tạo sổ điểm')

    expect(createButton).toBeDefined()
    if (!createButton) throw new Error('Teacher create button should be visible for an assigned class-subject')
    await createButton.trigger('click')
    await flushPromises()

    expect(mocks.createScorebook).toHaveBeenCalledWith('teacher-token', { classSubjectId: 20 })
    expect(mocks.openScorebook).toHaveBeenCalledWith('teacher-token', 12)
    expect(wrapper.text()).toContain('Đã tạo và mở sổ điểm.')
  })

  it('creates then opens an absent scorebook for an academic office session', async () => {
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
    expect(mocks.fetchMyEffectiveScorebookAssignments).not.toHaveBeenCalled()
    expect(mocks.fetchSemesters).toHaveBeenCalledWith('office-token', 1)
    expect(mocks.fetchClassSubjects).toHaveBeenCalledWith('office-token', 3, 2)
    await view.create()

    expect(mocks.createScorebook).toHaveBeenCalledWith('office-token', { classSubjectId: 20 })
    expect(mocks.openScorebook).toHaveBeenCalledWith('office-token', 12)
  })

  it('reloads the created scorebook and reports when opening it fails', async () => {
    clearAuthSession()
    saveAuthSession({
      accessToken: 'office-token',
      user: { id: 2, username: 'office.demo', roles: ['ACADEMIC_OFFICE'] },
    })
    mocks.fetchScorebookByClassSubject.mockRejectedValue(new ApiError(404, 'Chưa có sổ điểm'))
    mocks.createScorebook.mockResolvedValue(scorebook)
    mocks.openScorebook.mockRejectedValue(new ApiError(409, 'Sổ điểm chưa thể mở'))
    mocks.fetchScorebook.mockResolvedValue(scorebook)
    const wrapper = mountView()
    await flushPromises()
    const view = wrapper.vm as unknown as {
      create: () => Promise<void>
      scorebook: typeof scorebook | null
      errorMessage: string
    }

    await view.create()

    expect(mocks.fetchScorebook).toHaveBeenCalledWith('office-token', 12)
    expect(view.scorebook?.status).toBe('OPEN')
    expect(view.errorMessage).toContain('Đã tạo sổ điểm nhưng chưa thể mở.')
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

  it('keeps published score entry editable while configuration remains separate', async () => {
    mocks.fetchScorebookByClassSubject.mockResolvedValue({ ...scorebook, status: 'PUBLISHED' })
    mocks.fetchScorebook.mockResolvedValue({ ...scorebook, status: 'PUBLISHED' })
    mocks.fetchScoreGrid.mockResolvedValue({ ...grid, scorebookStatus: 'PUBLISHED' })
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.get('[data-test="score-grid"]').attributes('data-read-only')).toBe('false')
    expect(wrapper.get('[data-test="score-entry-dialog"]').attributes('data-read-only')).toBe('false')
  })

  it('opens the correction-request dialog with the score-entry proposal', async () => {
    const wrapper = mountView()
    await flushPromises()
    const selectedStudent = { studentId: 11, studentCode: 'HS-001', studentName: 'Nguyễn Minh An', scores: {} }
    const selectedColumn = { columnId: 7, assessmentType: 'KTTT' as const, columnNo: 1, columnName: 'Thường xuyên 1' }
    const selectedScore = {
      scoreId: 1, assessmentColumnId: 7, studentId: 11, studentCode: 'HS-001', studentName: 'Nguyễn Minh An',
      scoreStatus: 'SCORED' as const, scoreValue: 6.5, note: null, enteredBy: 5, enteredAt: '2026-09-02T08:00:00', updatedBy: null, updatedAt: null, version: 3,
    }
    const view = wrapper.vm as unknown as {
      selectedStudent: typeof selectedStudent
      selectedGridColumn: typeof selectedColumn
      scoreChangeRequestDialogVisible: boolean
      scoreChangeRequestContext: Record<string, unknown> | null
      openScoreChangeRequest: (context: { studentName: string; score: typeof selectedScore; proposedStatus: 'SCORED'; proposedValue: number; reason: string }) => Promise<void>
    }
    view.selectedStudent = selectedStudent
    view.selectedGridColumn = selectedColumn

    await view.openScoreChangeRequest({
      studentName: selectedStudent.studentName,
      score: selectedScore,
      proposedStatus: 'SCORED',
      proposedValue: 8,
      reason: 'Điều chỉnh theo phiếu chấm.',
    })

    expect(view.scoreChangeRequestDialogVisible).toBe(true)
    expect(view.scoreChangeRequestContext).toMatchObject({
      studentCode: 'HS-001',
      columnId: 7,
      proposedStatus: 'SCORED',
      proposedValue: 8,
      reason: 'Điều chỉnh theo phiếu chấm.',
    })
  })

  it('requires confirmation before stopping use of a column', async () => {
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
      header: 'Xác nhận ngừng sử dụng cột điểm',
      accept: expect.any(Function),
    }))
  })
})
