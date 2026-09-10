export type V3ReviewState = 'loading' | 'empty' | 'unauthorized' | 'forbidden' | 'not-found' | 'conflict' | 'ready'

export interface V3QueryState {
  search: string
  filters: Record<string, string>
  sort: string | null
  page: number
  pageSize: number
}

export interface V3Page<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
  appliedFilters: Record<string, string>
}

export interface V3ReviewContext {
  academicYear: string
  semester: string
  module: string
}

export interface V3ReviewItem {
  identity: string
  status: string
  scope: string
  updatedAt: string
}
