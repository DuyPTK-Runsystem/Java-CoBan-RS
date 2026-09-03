import { afterEach, describe, expect, it, vi } from 'vitest'
import { createHomeroomAssignment, createSubjectTeachingAssignment, endHomeroomAssignment, endSubjectTeachingAssignment, fetchHomeroomAssignmentsByClass, fetchSubjectAssignmentsByClass, fetchSubjectAssignmentsByTeacher, replaceHomeroomAssignment, replaceSubjectTeachingAssignment } from './assignmentApi'

const fetchMock = vi.fn()
describe('assignmentApi', () => {
  afterEach(() => { fetchMock.mockReset(); vi.unstubAllGlobals() })
  it('maps all assignment endpoints without changing date-only values', async () => {
    fetchMock.mockImplementation(() => Promise.resolve(new Response(JSON.stringify({ data: { id: 1 } }), { status: 200 }))); vi.stubGlobal('fetch', fetchMock)
    const request = { teacherId: 2, validFrom: '2026-09-01', validTo: null }
    await fetchHomeroomAssignmentsByClass('t', 3); await fetchSubjectAssignmentsByClass('t', 3, 1); await fetchSubjectAssignmentsByTeacher('t', 2); await createHomeroomAssignment('t', 3, request); await replaceHomeroomAssignment('t', 4, request); await endHomeroomAssignment('t', 4, { validTo: '2027-01-01' }); await createSubjectTeachingAssignment('t', 5, request); await replaceSubjectTeachingAssignment('t', 6, request); await endSubjectTeachingAssignment('t', 6, { validTo: '2027-01-01' })
    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual(['http://localhost:8081/api/v2/assignments/classes/3', 'http://localhost:8081/api/v2/assignments/classes/3/subjects?semesterId=1', 'http://localhost:8081/api/v2/assignments/teachers/2', 'http://localhost:8081/api/v2/classes/3/homeroom-assignments', 'http://localhost:8081/api/v2/homeroom-assignments/4/replace', 'http://localhost:8081/api/v2/homeroom-assignments/4/end', 'http://localhost:8081/api/v2/class-subjects/5/teaching-assignments', 'http://localhost:8081/api/v2/subject-teaching-assignments/6/replace', 'http://localhost:8081/api/v2/subject-teaching-assignments/6/end'])
  })
})
