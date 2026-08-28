import { afterEach, describe, expect, it, vi } from 'vitest'
import { createTeacher, deleteTeacher, fetchTeacherById, fetchTeachers, updateTeacher } from './teacherApi'

const fetchMock = vi.fn()
describe('teacherApi', () => {
  afterEach(() => { fetchMock.mockReset(); vi.unstubAllGlobals() })
  it('uses typed teacher endpoints and optional status query', async () => {
    fetchMock.mockImplementation(() => Promise.resolve(new Response(JSON.stringify({ data: { id: 1 } }), { status: 200 }))); vi.stubGlobal('fetch', fetchMock)
    const request = { userId: null, teacherCode: 'GV01', teacherName: 'An', dateOfBirth: '', gender: '', phone: '', email: '', department: '', joinDate: '', status: 'ACTIVE' as const }
    await fetchTeachers('token', 'ACTIVE'); await fetchTeacherById('token', 1); await createTeacher('token', request); await updateTeacher('token', 1, request); await deleteTeacher('token', 1)
    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual(['http://localhost:8081/api/v2/teachers?status=ACTIVE', 'http://localhost:8081/api/v2/teachers/1', 'http://localhost:8081/api/v2/teachers', 'http://localhost:8081/api/v2/teachers/1', 'http://localhost:8081/api/v2/teachers/1'])
    expect(fetchMock.mock.calls[2][1]).toMatchObject({ method: 'POST', body: JSON.stringify(request) })
  })
})
