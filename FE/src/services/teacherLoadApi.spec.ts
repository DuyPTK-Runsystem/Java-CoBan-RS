import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  activateTeacherLoadPolicy,
  createTeacherLoadEligibility,
  createTeacherLoadPolicy,
  revokeTeacherLoadEligibility,
  updateTeacherLoadEligibility,
  getActiveTeacherLoadPolicy,
  listTeacherLoadEligibilities,
  listTeacherLoadPolicies,
  listTeacherLoadRules,
  createTeacherLoadRule,
} from './teacherLoadApi'

const fetchMock = vi.fn()

describe('teacherLoadApi', () => {
  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('handles load evaluation, policies and eligibilities', async () => {
    fetchMock.mockImplementation((input: RequestInfo | URL) => {
      const data = String(input).includes('/active')
        ? { id: 7, version: 'policy-current', source: 'Decision 01', effectiveFrom: '2026-09-01', status: 'ACTIVE', versionLock: 2 }
        : { meta: {}, result: [] }
      return Promise.resolve(new Response(JSON.stringify({ data }), { status: 200 }))
    })
    vi.stubGlobal('fetch', fetchMock)

    const active = await getActiveTeacherLoadPolicy('token')
    await listTeacherLoadPolicies('token')
     await createTeacherLoadPolicy({
       policyName: 'Chính sách 2026',
       sourceDocument: 'TT05',
       effectiveFrom: '2026-09-01',
       standardPeriodsHighSchool: 19,
       homeroomReduction: 4,
       nursingChildReduction: 3,
     }, 'token')
     await activateTeacherLoadPolicy(1, 2, 'token')
    await listTeacherLoadEligibilities(100, 'token')
    await createTeacherLoadEligibility({ teacherId: 100, ruleCode: 'HOMEROOM', validFrom: '2026-09-01', validTo: '2026-12-31', evidenceReference: 'HS-1' }, 'token')
     await revokeTeacherLoadEligibility(1, 2, 'token')
     await updateTeacherLoadEligibility(1, { expectedVersion: 2, evidenceReference: 'HS-2' }, 'token')
     await listTeacherLoadRules(1, 'token')
     await createTeacherLoadRule(1, {
       ruleCode: 'SENIORITY',
       ruleName: 'Thâm niên',
       triggerType: 'ELIGIBILITY',
       reductionPeriods: 5,
       source: 'TT05',
     }, 'token')

    const urls = fetchMock.mock.calls.map(([url]) => url)
    expect(active?.id).toBe(7)
    expect(active?.policyName).toBe('policy-current')
    expect(urls[0]).toContain('/api/v3/teacher-load-policies/active')
    expect(urls[1]).toContain('/api/v3/teacher-load-policies')
    expect(urls[2]).toContain('/api/v3/teacher-load-policies')
     expect(urls[3]).toContain('/api/v3/teacher-load-policies/1/activate')
     expect(urls[3]).toContain('expectedVersion=2')
    expect(urls[4]).toContain('/api/v3/teacher-load-eligibilities?teacherId=100')
    expect(urls[5]).toContain('/api/v3/teacher-load-eligibilities')
     expect(urls[6]).toContain('/api/v3/teacher-load-eligibilities/1')
     expect(urls[7]).toContain('/api/v3/teacher-load-eligibilities/1')
     expect(urls[8]).toContain('/api/v3/teacher-load-policies/1/rules')
     expect(urls[9]).toContain('/api/v3/teacher-load-policies/1/rules')
  })
})
