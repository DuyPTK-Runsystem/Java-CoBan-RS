import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  activateTeacherLoadPolicy,
  createTeacherLoadEligibility,
  createTeacherLoadPolicy,
  deleteTeacherLoadEligibility,
  getActiveTeacherLoadPolicy,
  getTeacherLoads,
  listTeacherLoadEligibilities,
  listTeacherLoadPolicies,
} from './teacherLoadApi'

const fetchMock = vi.fn()

describe('teacherLoadApi', () => {
  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('handles load evaluation, policies and eligibilities', async () => {
    fetchMock.mockImplementation(() =>
      Promise.resolve(new Response(JSON.stringify({ data: [] }), { status: 200 })),
    )
    vi.stubGlobal('fetch', fetchMock)

    await getTeacherLoads({ revisionId: 10, teacherId: 100 }, 'token')
    await getActiveTeacherLoadPolicy('token')
    await listTeacherLoadPolicies('token')
    await createTeacherLoadPolicy({ policyName: 'Chính sách 2026' }, 'token')
    await activateTeacherLoadPolicy(1, 'token')
    await listTeacherLoadEligibilities(100, 'token')
    await createTeacherLoadEligibility({ teacherId: 100, conditionType: 'HOMEROOM' }, 'token')
    await deleteTeacherLoadEligibility(1, 'token')

    const urls = fetchMock.mock.calls.map(([url]) => url)
    expect(urls[0]).toContain('/api/v2/teacher-loads?revisionId=10&teacherId=100')
    expect(urls[1]).toContain('/api/v2/teacher-load-policies/active')
    expect(urls[2]).toContain('/api/v2/teacher-load-policies')
    expect(urls[3]).toContain('/api/v2/teacher-load-policies')
    expect(urls[4]).toContain('/api/v2/teacher-load-policies/1/activate')
    expect(urls[5]).toContain('/api/v2/teacher-load-eligibilities?teacherId=100')
    expect(urls[6]).toContain('/api/v2/teacher-load-eligibilities')
    expect(urls[7]).toContain('/api/v2/teacher-load-eligibilities/1')
  })
})

