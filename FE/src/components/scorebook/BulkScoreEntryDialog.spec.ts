import { shallowMount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

import BulkScoreEntryDialog from './BulkScoreEntryDialog.vue'
import { downloadBulkScoreTemplate, previewBulkStudentScores } from '@/services/scorebookApi'

vi.mock('@/services/authSession', () => ({
  getAuthSession: vi.fn(() => ({ accessToken: 'test-token', user: { id: 1, username: 'teacher' } })),
}))
vi.mock('@/services/scorebookApi', () => ({
  downloadBulkScoreTemplate: vi.fn(),
  previewBulkStudentScores: vi.fn(),
}))

const column = { columnId: 7, assessmentType: 'KTTT' as const, columnNo: 1, columnName: 'TX1' }
const students = [{
  studentId: 11,
  studentCode: 'HS001',
  studentName: 'An',
  scores: {
    '7': {
      scoreId: 1,
      assessmentColumnId: 7,
      studentId: 11,
      studentCode: 'HS001',
      studentName: 'An',
      scoreStatus: 'SCORED' as const,
      scoreValue: 5,
      note: null,
      enteredBy: null,
      enteredAt: null,
      updatedBy: null,
      updatedAt: null,
      version: 3,
    },
  },
}]

describe('BulkScoreEntryDialog', () => {
  afterEach(() => {
    vi.clearAllMocks()
    vi.unstubAllGlobals()
  })

  it('initializes rows when mounted visible', () => {
    const wrapper = shallowMount(BulkScoreEntryDialog, {
      props: { visible: true, column, students },
    })
    const view = wrapper.vm as unknown as {
      rows: Array<{ studentId: number; scoreValue: number | null; expectedVersion: number | null }>
    }

    expect(view.rows).toEqual([
      expect.objectContaining({ studentId: 11, scoreValue: 5, expectedVersion: 3 }),
    ])
  })

  it('sends only changed rows with status and expectedVersion', async () => {
    const wrapper = shallowMount(BulkScoreEntryDialog, {
      props: { visible: false, column, students },
    })
    await wrapper.setProps({ visible: true })
    const view = wrapper.vm as unknown as {
      rows: Array<{ scoreStatus: string; scoreValue: number | null }>
      save: () => void
    }
    view.rows[0].scoreStatus = 'ABSENT'
    view.rows[0].scoreValue = 5
    view.save()

    expect(wrapper.emitted('save')?.[0]).toEqual([{
      items: [{
        studentId: 11,
        scoreStatus: 'ABSENT',
        scoreValue: null,
        note: null,
        expectedVersion: 3,
      }],
    }])
  })

  it('does not submit unchanged rows', async () => {
    const wrapper = shallowMount(BulkScoreEntryDialog, {
      props: { visible: false, column, students },
    })
    await wrapper.setProps({ visible: true })
    const view = wrapper.vm as unknown as { save: () => void; validationMessage: string }
    view.save()

    expect(wrapper.emitted('save')).toBeUndefined()
    expect(view.validationMessage).toContain('Chưa có thay đổi')
  })

  it('formats a bulk score with one decimal only after that input loses focus', () => {
    const wrapper = shallowMount(BulkScoreEntryDialog, {
      props: { visible: true, column, students },
    })
    const view = wrapper.vm as unknown as {
      focusedStudentId: number | null
      rows: Array<{ studentId: number }>
      minFractionDigits: (row: { studentId: number }) => number
    }

    view.focusedStudentId = 11
    expect(view.minFractionDigits(view.rows[0])).toBe(0)

    view.focusedStudentId = null
    expect(view.minFractionDigits(view.rows[0])).toBe(1)
  })

  it('rounds score to one decimal on blur and when saving', async () => {
    const wrapper = shallowMount(BulkScoreEntryDialog, {
      props: { visible: true, column, students },
    })
    const view = wrapper.vm as unknown as {
      rows: Array<{ scoreStatus: string; scoreValue: number | null }>
      handleScoreBlur: (row: { scoreStatus: string; scoreValue: number | null }) => void
      save: () => void
      validationMessage: string
    }

    view.rows[0].scoreValue = 8.47
    view.handleScoreBlur(view.rows[0])
    expect(view.rows[0].scoreValue).toBe(8.5)

    view.save()
    expect(view.validationMessage).toBe('')
    expect(wrapper.emitted('save')?.[0]).toEqual([{
      items: [{
        studentId: 11,
        scoreStatus: 'SCORED',
        scoreValue: 8.5,
        note: null,
        expectedVersion: 3,
      }],
    }])
  })

  it('rounds score to one decimal when saving without blur and submits successfully', async () => {
    const wrapper = shallowMount(BulkScoreEntryDialog, {
      props: { visible: true, column, students },
    })
    const view = wrapper.vm as unknown as {
      rows: Array<{ scoreStatus: string; scoreValue: number | null }>
      save: () => void
      validationMessage: string
    }

    view.rows[0].scoreValue = 0.47
    view.save()
    expect(view.validationMessage).toBe('')
    expect(wrapper.emitted('save')?.[0]).toEqual([{
      items: [{
        studentId: 11,
        scoreStatus: 'SCORED',
        scoreValue: 0.5,
        note: null,
        expectedVersion: 3,
      }],
    }])
  })

  it('allows file preview without downloading the template first', async () => {
    vi.mocked(previewBulkStudentScores).mockResolvedValue({
      assessmentColumnId: 7,
      summary: { validRows: 1, errorRows: 0, newScores: 0, updatedScores: 1 },
      rows: [],
      normalizedItems: [],
    })
    const wrapper = shallowMount(BulkScoreEntryDialog, {
      props: { visible: true, column, students, initialMode: 'file' },
    })
    const view = wrapper.vm as unknown as {
      handleFileSelected: (event: Event) => Promise<void>
    }
    const input = document.createElement('input')
    const file = new File(['xlsx'], 'scores.xlsx')
    Object.defineProperty(input, 'files', { value: [file] })

    await view.handleFileSelected({ target: input } as unknown as Event)

    expect(previewBulkStudentScores).toHaveBeenCalledWith('test-token', 7, file)
  })

  it('downloads the template before allowing a file preview', async () => {
    vi.mocked(downloadBulkScoreTemplate).mockResolvedValue(new Blob(['xlsx']))
    vi.stubGlobal('URL', { createObjectURL: vi.fn(() => 'blob:test'), revokeObjectURL: vi.fn() })
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    const wrapper = shallowMount(BulkScoreEntryDialog, {
      props: { visible: true, column, students, initialMode: 'file' },
    })
    const view = wrapper.vm as unknown as {
      downloadTemplate: () => Promise<void>
      templateDownloaded: boolean
    }

    await view.downloadTemplate()

    expect(downloadBulkScoreTemplate).toHaveBeenCalledWith('test-token', 7)
    expect(view.templateDownloaded).toBe(true)
  })

  it('opens preview automatically after a valid file is selected', async () => {
    vi.mocked(previewBulkStudentScores).mockResolvedValue({
      assessmentColumnId: 7,
      summary: { validRows: 1, errorRows: 0, newScores: 0, updatedScores: 1 },
      rows: [],
      normalizedItems: [],
    })
    const wrapper = shallowMount(BulkScoreEntryDialog, {
      props: { visible: true, column, students, initialMode: 'file' },
    })
    const view = wrapper.vm as unknown as {
      templateDownloaded: boolean
      handleFileSelected: (event: Event) => Promise<void>
    }
    const input = document.createElement('input')
    const file = new File(['xlsx'], 'scores.xlsx')
    Object.defineProperty(input, 'files', { value: [file] })
    view.templateDownloaded = true

    await view.handleFileSelected({ target: input } as unknown as Event)

    expect(previewBulkStudentScores).toHaveBeenCalledWith('test-token', 7, file)
  })

  it('emits normalized preview items through the existing bulk save event', async () => {
    vi.mocked(previewBulkStudentScores).mockResolvedValue({
      assessmentColumnId: 7,
      summary: { validRows: 1, errorRows: 0, newScores: 0, updatedScores: 1 },
      rows: [{
        rowNumber: 2,
        studentCode: 'HS001',
        studentId: 11,
        studentName: 'An',
        oldStatus: 'SCORED',
        oldValue: 5,
        oldVersion: 3,
        newStatus: 'SCORED',
        newValue: 8.5,
        note: null,
        result: 'VALID',
        errorCode: null,
        message: null,
      }],
      normalizedItems: [{ studentId: 11, scoreStatus: 'SCORED', scoreValue: 8.5, note: null, expectedVersion: 3 }],
    })
    const wrapper = shallowMount(BulkScoreEntryDialog, {
      props: { visible: true, column, students, initialMode: 'file' },
    })
    const view = wrapper.vm as unknown as {
      templateDownloaded: boolean
      selectedFile: File | null
      previewFile: () => Promise<void>
      save: () => void
    }
    view.templateDownloaded = true
    view.selectedFile = new File(['xlsx'], 'scores.xlsx')

    await view.previewFile()
    view.save()

    expect(previewBulkStudentScores).toHaveBeenCalledWith('test-token', 7, view.selectedFile)
    expect(wrapper.emitted('save')?.[0]).toEqual([{
      items: [{ studentId: 11, scoreStatus: 'SCORED', scoreValue: 8.5, note: null, expectedVersion: 3 }],
    }])
  })

  it('does not emit bulk save when preview contains row errors', async () => {
    vi.mocked(previewBulkStudentScores).mockResolvedValue({
      assessmentColumnId: 7,
      summary: { validRows: 0, errorRows: 1, newScores: 0, updatedScores: 0 },
      rows: [{
        rowNumber: 2,
        studentCode: 'HS001',
        studentId: 11,
        studentName: 'An',
        oldStatus: 'SCORED',
        oldValue: 5,
        oldVersion: 3,
        newStatus: 'SCORED',
        newValue: null,
        note: null,
        result: 'ERROR',
        errorCode: 'MISSING_SCORE',
        message: 'Thiếu điểm',
      }],
      normalizedItems: [],
    })
    const wrapper = shallowMount(BulkScoreEntryDialog, {
      props: { visible: true, column, students, initialMode: 'file' },
    })
    const view = wrapper.vm as unknown as {
      templateDownloaded: boolean
      selectedFile: File | null
      previewFile: () => Promise<void>
      save: () => void
      validationMessage: string
    }
    view.templateDownloaded = true
    view.selectedFile = new File(['xlsx'], 'scores.xlsx')

    await view.previewFile()
    view.save()

    expect(wrapper.emitted('save')).toBeUndefined()
    expect(view.validationMessage).toContain('dòng lỗi')
  })
})
