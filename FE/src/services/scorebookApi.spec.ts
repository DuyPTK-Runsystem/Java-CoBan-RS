import { afterEach, describe, expect, it, vi } from 'vitest'

import {
  bulkUpsertStudentScores,
  createAssessmentColumn,
  createScorebook,
  deactivateAssessmentColumn,
  downloadBulkScoreTemplate,
  fetchScorebook,
  fetchScorebookByClassSubject,
  fetchScoreGrid,
  openScorebook,
  publishScorebook,
  previewBulkStudentScores,
  updateAssessmentColumn,
  upsertSkillWeight,
  upsertStudentScore,
  upsertStudentScoreByCode,
} from './scorebookApi'

const fetchMock = vi.fn()

function jsonResponse(data: unknown): Response {
  return new Response(JSON.stringify({ data }), { status: 200 })
}

describe('scorebookApi', () => {
  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('serializes lifecycle and class-subject lookup paths', async () => {
    fetchMock.mockImplementation(() => Promise.resolve(jsonResponse({})))
    vi.stubGlobal('fetch', fetchMock)

    await createScorebook('token', { classSubjectId: 20 })
    await fetchScorebook('token', 12)
    await fetchScorebookByClassSubject('token', 20)
    await openScorebook('token', 12)
    await publishScorebook('token', 12)

    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      'http://localhost:8081/api/v2/scorebooks',
      'http://localhost:8081/api/v2/scorebooks/12',
      'http://localhost:8081/api/v2/scorebooks/by-class-subject/20',
      'http://localhost:8081/api/v2/scorebooks/12/open',
      'http://localhost:8081/api/v2/scorebooks/12/publish',
    ])
    expect(fetchMock.mock.calls[0][1]).toMatchObject({
      method: 'POST',
      body: JSON.stringify({ classSubjectId: 20 }),
    })
  })

  it('serializes assessment-column and skill-weight methods', async () => {
    fetchMock
      .mockImplementationOnce(() => Promise.resolve(jsonResponse({})))
      .mockImplementationOnce(() => Promise.resolve(jsonResponse({})))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockImplementationOnce(() => Promise.resolve(jsonResponse({})))
    vi.stubGlobal('fetch', fetchMock)

    await createAssessmentColumn('token', 12, {
      assessmentType: 'KTĐK',
      columnNo: 1,
      columnName: 'Giữa kỳ',
    })
    await updateAssessmentColumn('token', 7, { columnName: 'Giữa kỳ mới' })
    await deactivateAssessmentColumn('token', 7)
    await upsertSkillWeight('token', 12, {
      ktttWeightPercent: 20,
      ktdkWeightPercent: 30,
      ktckWeightPercent: 50,
    })

    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      'http://localhost:8081/api/v2/scorebooks/12/columns',
      'http://localhost:8081/api/v2/assessment-columns/7',
      'http://localhost:8081/api/v2/assessment-columns/7',
      'http://localhost:8081/api/v2/scorebooks/12/skill-weight',
    ])
    expect(fetchMock.mock.calls[0][1].body).toBe(JSON.stringify({
      assessmentType: 'KTĐK',
      columnNo: 1,
      columnName: 'Giữa kỳ',
    }))
    expect(fetchMock.mock.calls[3][1].method).toBe('PUT')
  })

  it('serializes pagination and single score mutations without losing zero or version', async () => {
    fetchMock.mockImplementation(() => Promise.resolve(jsonResponse({})))
    vi.stubGlobal('fetch', fetchMock)

    await fetchScoreGrid('token', 12, 2, 25)
    await upsertStudentScore('token', 7, 11, {
      scoreStatus: 'SCORED',
      scoreValue: 0,
      expectedVersion: 4,
    })
    await upsertStudentScoreByCode('token', 7, 'HS/001', {
      scoreStatus: 'ABSENT',
      scoreValue: null,
    })

    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      'http://localhost:8081/api/v2/scorebooks/12/score-entries?page=2&size=25',
      'http://localhost:8081/api/v2/assessment-columns/7/students/11/score',
      'http://localhost:8081/api/v2/assessment-columns/7/students/by-code/HS%2F001/score',
    ])
    expect(fetchMock.mock.calls[1][1].body).toBe(JSON.stringify({
      scoreStatus: 'SCORED',
      scoreValue: 0,
      expectedVersion: 4,
    }))
  })

  it('keeps bulk status/value/version payloads intact', async () => {
    fetchMock.mockImplementation(() => Promise.resolve(jsonResponse([])))
    vi.stubGlobal('fetch', fetchMock)
    const request = {
      items: [
        { studentId: 11, scoreStatus: 'SCORED' as const, scoreValue: 0, expectedVersion: 3 },
        { studentCode: 'HS-002', scoreStatus: 'EXEMPTED' as const, scoreValue: null },
      ],
    }

    await bulkUpsertStudentScores('token', 7, request)

    expect(fetchMock.mock.calls[0][1]).toMatchObject({
      method: 'POST',
      body: JSON.stringify(request),
    })
  })

  it('downloads the score template as a Blob with the spreadsheet media type', async () => {
    fetchMock.mockResolvedValue(new Response('xlsx-content', { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    const result = await downloadBulkScoreTemplate('token', 7)

    expect(await result.text()).toBe('xlsx-content')
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8081/api/v2/assessment-columns/7/scores/bulk-template',
      expect.objectContaining({
        method: 'GET',
        headers: { Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', Authorization: 'Bearer token' },
      }),
    )
  })

  it('keeps the typed preview response and normalized items from the provisional endpoint', async () => {
    const preview = {
      assessmentColumnId: 7,
      summary: { validRows: 1, errorRows: 0, newScores: 1, updatedScores: 0 },
      rows: [{
        rowNumber: 2,
        studentCode: 'HS-001',
        studentId: 11,
        studentName: 'An',
        oldStatus: null,
        oldValue: null,
        oldVersion: null,
        newStatus: 'SCORED',
        newValue: 8.5,
        note: null,
        result: 'VALID',
        errorCode: null,
        message: null,
      }],
      items: [{ studentId: 11, scoreStatus: 'SCORED', scoreValue: 8.5, note: null, expectedVersion: null }],
    }
    fetchMock.mockResolvedValue(jsonResponse(preview))
    vi.stubGlobal('fetch', fetchMock)
    const file = new File(['xlsx'], 'scores.xlsx', { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })

    await expect(previewBulkStudentScores('token', 7, file)).resolves.toEqual(preview)
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8081/api/v2/assessment-columns/7/scores/bulk/preview',
      expect.objectContaining({
        method: 'POST',
        headers: { Accept: 'application/json', Authorization: 'Bearer token' },
        body: expect.any(FormData),
      }),
    )
    const requestBody = fetchMock.mock.calls[0][1].body as FormData
    expect(requestBody.get('file')).toBe(file)
  })
})
