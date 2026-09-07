import { afterEach, describe, expect, it, vi } from 'vitest'
import { fetchStudents, generateStudentCode, getStudent, transitionStudentStatus } from './studentApi'

const fetchMock = vi.fn()
describe('studentApi', () => {
  afterEach(() => { fetchMock.mockReset(); vi.unstubAllGlobals() })
  it('serializes server-side query and unwraps the page', async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ data: { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 } }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)
    await fetchStudents('token', {
      page: 0,
      pageSize: 10,
      sortField: 'studentName',
      sortOrder: -1,
      search: {
        studentCode: 'STU',
        studentName: 'An',
        dateOfBirth: new Date(2020, 0, 2),
        status: 'ACTIVE',
        classId: 12,
      },
    })
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8081/api/v2/students?page=0&size=10&sortField=studentName&sortDirection=desc&studentCode=STU&studentName=An&birthday=2020-01-02&status=ACTIVE&classId=12',
      expect.objectContaining({ headers: expect.objectContaining({ Authorization: 'Bearer token' }) }),
    )
  })
  it('uses the detail endpoint', async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ data: { studentId: 4 } }), { status: 200 })); vi.stubGlobal('fetch', fetchMock)
    await getStudent('token', 4)
    expect(fetchMock.mock.calls[0][0]).toBe('http://localhost:8081/api/v2/students/4')
  })
  it('uses the v2 code-generation endpoint', async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ data: { studentCode: 'STU0000001' } }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    await generateStudentCode('token')

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8081/api/v2/students/code',
      expect.objectContaining({ method: 'POST', body: JSON.stringify({}) }),
    )
  })
  it('sends an explicit lifecycle transition through the v2 PATCH endpoint', async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ data: { studentId: 4, status: 'INACTIVE' } }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    await transitionStudentStatus('token', 4, 'INACTIVE')

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8081/api/v2/students/4/status',
      expect.objectContaining({ method: 'PATCH', body: JSON.stringify({ status: 'INACTIVE' }) }),
    )
  })
})
