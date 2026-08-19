import { afterEach, describe, expect, it, vi } from 'vitest'
import { downloadStudentsCsv, fetchStudents, getStudent } from './studentApi'

const fetchMock = vi.fn()
describe('studentApi', () => {
  afterEach(() => { fetchMock.mockReset(); vi.unstubAllGlobals() })
  it('serializes server-side query and unwraps the page', async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ data: { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 } }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)
    await fetchStudents('token', { page: 0, pageSize: 10, sortField: 'studentName', sortOrder: -1, search: { studentCode: 'STU', studentName: 'An', dateOfBirth: new Date(2020, 0, 2) } })
    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('sortDirection=desc'), expect.objectContaining({ headers: expect.objectContaining({ Authorization: 'Bearer token' }) }))
    expect(fetchMock.mock.calls[0][0]).toContain('birthday=2020-01-02')
  })
  it('uses the detail endpoint', async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ data: { studentId: 4 } }), { status: 200 })); vi.stubGlobal('fetch', fetchMock)
    await getStudent('token', 4)
    expect(fetchMock.mock.calls[0][0]).toBe('http://localhost:8081/api/v1/students/4')
  })
  it('downloads the raw CSV export with a bearer token', async () => {
    fetchMock.mockResolvedValue(new Response('student_id,student_name\r\n1,An\r\n', { status: 200, headers: { 'Content-Type': 'text/csv' } }))
    vi.stubGlobal('fetch', fetchMock)

    const csv = await downloadStudentsCsv('token')

    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8081/api/v1/students/export', expect.objectContaining({ headers: { Accept: 'text/csv', Authorization: 'Bearer token' } }))
    expect(await csv.text()).toContain('student_id,student_name')
  })
})
