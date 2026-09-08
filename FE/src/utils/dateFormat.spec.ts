import { describe, expect, it } from 'vitest'

import { formatApiDate, formatApiDateTime, parseApiDateTime } from './dateFormat'

describe('dateFormat', () => {
  it('formats date-only and local datetime values without timezone conversion', () => {
    expect(formatApiDate('2026-09-03')).toBe('03/09/2026')
    expect(formatApiDateTime('2026-09-03T09:12')).toBe('03/09/2026 09:12:00')
    expect(formatApiDateTime('2026-09-03 09:12:44.123Z')).toBe('03/09/2026 09:12:44')
  })

  it('rejects impossible calendar and clock values', () => {
    expect(parseApiDateTime('2026-02-30')).toBeNull()
    expect(parseApiDateTime('2026-09-03T24:00:00')).toBeNull()
    expect(formatApiDate('not-a-date')).toBe('not-a-date')
  })
})
