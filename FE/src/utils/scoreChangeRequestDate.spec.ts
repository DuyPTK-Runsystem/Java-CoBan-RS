import { describe, expect, it } from 'vitest'

import { formatScoreChangeRequestDateTime } from './scoreChangeRequestDate'

describe('formatScoreChangeRequestDateTime', () => {
  it('formats ISO local datetime as dd-mm-yyyy hh:mm:ss', () => {
    expect(formatScoreChangeRequestDateTime('2026-09-03T11:27:51')).toBe('03-09-2026 11:27:51')
  })

  it('adds missing seconds without changing the received local time', () => {
    expect(formatScoreChangeRequestDateTime('2026-09-03 09:15')).toBe('03-09-2026 09:15:00')
  })

  it('keeps empty and invalid values safe', () => {
    expect(formatScoreChangeRequestDateTime(null)).toBe('-')
    expect(formatScoreChangeRequestDateTime('not-a-date')).toBe('not-a-date')
  })
})
