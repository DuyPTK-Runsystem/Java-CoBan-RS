import { describe, expect, it } from 'vitest'

import { readV3QueryState, writeV3QueryState } from './v3QueryState'

describe('v3QueryState', () => {
  it('reads only supported query keys and applies safe pagination defaults', () => {
    const state = readV3QueryState(
      new URLSearchParams('search=%20An%20&sort=studentName%2Casc&page=-1&pageSize=0&filter.status=ACTIVE&ignored=value'),
      20,
    )

    expect(state).toEqual({
      search: 'An',
      sort: 'studentName,asc',
      page: 0,
      pageSize: 20,
      filters: { status: 'ACTIVE' },
    })
  })

  it('writes a deterministic URL query without empty filters', () => {
    const params = writeV3QueryState({
      search: '  Minh ',
      sort: 'updatedAt,desc',
      page: -2,
      pageSize: 0,
      filters: { grade: '6', status: ' ', classId: '12' },
    }, 20)

    expect(params.toString()).toBe('search=Minh&sort=updatedAt%2Cdesc&page=0&pageSize=20&filter.classId=12&filter.grade=6')
  })

  it('does not invent a page size when state or default is invalid', () => {
    const params = writeV3QueryState({ search: '', sort: null, page: Number.MAX_SAFE_INTEGER + 1, pageSize: 0, filters: {} }, 50)

    expect(params.toString()).toBe('page=0&pageSize=50')
    expect(() => readV3QueryState(new URLSearchParams(), 0)).toThrow(RangeError)
    expect(() => writeV3QueryState({ search: '', sort: null, page: 0, pageSize: 20, filters: {} }, 0)).toThrow(RangeError)
  })
})
