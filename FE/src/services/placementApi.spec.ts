import { afterEach, describe, expect, it, vi } from 'vitest'

import {
  cancelPlacementSession,
  confirmPlacementSession,
  createPlacementSession,
  fetchPlacementResults,
  getPlacementSession,
  simulatePlacementSession,
  updatePlacementSession,
} from './placementApi'

const fetchMock = vi.fn()

describe('placementApi', () => {
  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('reads the zero-based placement result envelope and serializes page parameters with targetClassId', async () => {
    const page = {
      meta: { page: 2, pageSize: 25, totalPages: 4, totalItems: 76 },
      result: [{
        id: 7,
        studentId: 101,
        targetClassId: 81,
        resultStatus: 'AUTO_ASSIGNED',
        score: 8.5,
        issueCode: null,
        issueSeverity: null,
        explanation: 'Phân bổ theo profile REGULAR.',
      }],
    }
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ data: page }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(fetchPlacementResults('token', 74, 2, 25)).resolves.toEqual(page)
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8081/api/v3/placement-sessions/74/results?page=2&size=25',
      expect.objectContaining({
        method: 'GET',
        headers: expect.objectContaining({ Authorization: 'Bearer token' }),
      }),
    )
  })

  it('defaults result requests to the first zero-based page', async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ data: { meta: { page: 0, pageSize: 20, totalPages: 0, totalItems: 0 }, result: [] } }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    await fetchPlacementResults('token', 74)

    expect(fetchMock.mock.calls[0]?.[0]).toBe('http://localhost:8081/api/v3/placement-sessions/74/results?page=0&size=20')
  })

  it('creates placement session with candidate sources and evidence', async () => {
    const session = { id: 74, status: 'DRAFT', version: 1 }
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ data: session }), { status: 201 }))
    vi.stubGlobal('fetch', fetchMock)

    const payload = {
      academicYearId: 2026,
      targetGradeId: 8,
      ruleVersion: '074-v1',
      targetClasses: [{ classId: 81, profile: 'REGULAR' as const }],
      candidates: [{
        studentId: 101,
        targetGradeId: 8,
        sourceType: 'NEW_ADMISSION' as const,
        score: null,
        scoreSourceReference: null,
        genderSnapshot: null,
        eligibilityEvidence: 'Chuyển trường từ tỉnh khác',
        approvalReference: 'CV-1234',
      }],
    }

    await expect(createPlacementSession('token', payload)).resolves.toEqual(session)
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8081/api/v3/placement-sessions',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({ Authorization: 'Bearer token', 'Content-Type': 'application/json' }),
        body: JSON.stringify(payload),
      }),
    )
  })

  it('gets placement session by id', async () => {
    const session = { id: 74, status: 'DRAFT', version: 1 }
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ data: session }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(getPlacementSession('token', 74)).resolves.toEqual(session)
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8081/api/v3/placement-sessions/74',
      expect.objectContaining({
        method: 'GET',
        headers: expect.objectContaining({ Authorization: 'Bearer token' }),
      }),
    )
  })

  it('updates draft placement session target classes with expectedVersion', async () => {
    const session = { id: 74, status: 'DRAFT', version: 2 }
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ data: session }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    const payload = {
      expectedVersion: 1,
      targetClasses: [{ classId: 81, profile: 'ADVANCED' as const }],
    }

    await expect(updatePlacementSession('token', 74, payload)).resolves.toEqual(session)
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8081/api/v3/placement-sessions/74',
      expect.objectContaining({
        method: 'PUT',
        headers: expect.objectContaining({ Authorization: 'Bearer token', 'Content-Type': 'application/json' }),
        body: JSON.stringify(payload),
      }),
    )
  })

  it('simulates placement session with expectedVersion', async () => {
    const session = { id: 74, status: 'READY_FOR_CONFIRM', version: 2 }
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ data: session }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(simulatePlacementSession('token', 74, 1)).resolves.toEqual(session)
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8081/api/v3/placement-sessions/74/simulate',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({ Authorization: 'Bearer token', 'Content-Type': 'application/json' }),
        body: JSON.stringify({ expectedVersion: 1 }),
      }),
    )
  })

  it('confirms placement session with expectedVersion and idempotencyKey', async () => {
    const session = { id: 74, status: 'CONFIRMED', version: 3 }
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ data: session }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    const payload = { expectedVersion: 2, idempotencyKey: 'intent-uuid-123' }
    await expect(confirmPlacementSession('token', 74, payload)).resolves.toEqual(session)
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8081/api/v3/placement-sessions/74/confirm',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({ Authorization: 'Bearer token', 'Content-Type': 'application/json' }),
        body: JSON.stringify(payload),
      }),
    )
  })

  it('cancels placement session with expectedVersion', async () => {
    const session = { id: 74, status: 'CANCELLED', version: 3 }
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ data: session }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(cancelPlacementSession('token', 74, 2)).resolves.toEqual(session)
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8081/api/v3/placement-sessions/74/cancel',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({ Authorization: 'Bearer token', 'Content-Type': 'application/json' }),
        body: JSON.stringify({ expectedVersion: 2 }),
      }),
    )
  })
})
