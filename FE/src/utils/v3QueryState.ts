import type { V3QueryState } from '@/types/v3Foundation'

const FILTER_PREFIX = 'filter.'

function assertPositivePageSize(value: number): void {
  if (!Number.isSafeInteger(value) || value <= 0) {
    throw new RangeError('defaultPageSize must be a positive safe integer')
  }
}

function readNonNegativeInteger(value: string | null, fallback: number): number {
  if (value === null || !/^\d+$/.test(value)) return fallback
  const parsed = Number(value)
  return Number.isSafeInteger(parsed) ? parsed : fallback
}

function readPositiveInteger(value: string | null, fallback: number): number {
  const parsed = readNonNegativeInteger(value, fallback)
  return parsed > 0 ? parsed : fallback
}

export function readV3QueryState(params: URLSearchParams, defaultPageSize: number): V3QueryState {
  assertPositivePageSize(defaultPageSize)
  const filters: Record<string, string> = {}
  params.forEach((value, key) => {
    if (key.startsWith(FILTER_PREFIX) && value.trim()) {
      filters[key.slice(FILTER_PREFIX.length)] = value.trim()
    }
  })

  const sort = params.get('sort')?.trim() || null
  return {
    search: params.get('search')?.trim() ?? '',
    filters,
    sort,
    page: readNonNegativeInteger(params.get('page'), 0),
    pageSize: readPositiveInteger(params.get('pageSize'), defaultPageSize),
  }
}

export function writeV3QueryState(state: V3QueryState, defaultPageSize: number): URLSearchParams {
  assertPositivePageSize(defaultPageSize)
  const params = new URLSearchParams()
  if (state.search.trim()) params.set('search', state.search.trim())
  if (state.sort?.trim()) params.set('sort', state.sort.trim())
  const page = Number.isSafeInteger(state.page) && state.page >= 0 ? state.page : 0
  const pageSize = Number.isSafeInteger(state.pageSize) && state.pageSize > 0 ? state.pageSize : defaultPageSize
  params.set('page', String(page))
  params.set('pageSize', String(pageSize))

  Object.entries(state.filters)
    .filter(([, value]) => value.trim())
    .sort(([left], [right]) => left.localeCompare(right))
    .forEach(([key, value]) => params.set(`${FILTER_PREFIX}${key}`, value.trim()))

  return params
}
