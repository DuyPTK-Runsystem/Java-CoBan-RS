import { describe, expect, it } from 'vitest'

import { expiryDateAfterMonths, formatLibraryDate, libraryToday } from './libraryCardDates'

describe('library card expiry dates', () => {
  it('computes the inclusive date one month before the anniversary', () => {
    expect(expiryDateAfterMonths(1, '2026-10-09')).toBe('2026-11-08')
  })

  it('clamps month-end and leap-day anniversaries before applying inclusive expiry', () => {
    expect(expiryDateAfterMonths(1, '2025-01-31')).toBe('2025-02-27')
    expect(expiryDateAfterMonths(12, '2024-02-29')).toBe('2025-02-27')
  })

  it('handles year rollover and rejects a non-positive or fractional duration', () => {
    expect(expiryDateAfterMonths(2, '2026-11-30')).toBe('2027-01-29')
    expect(() => expiryDateAfterMonths(0, '2026-10-09')).toThrow('Thời hạn phải từ 1 tháng trở lên.')
    expect(() => expiryDateAfterMonths(1.5, '2026-10-09')).toThrow('Thời hạn phải từ 1 tháng trở lên.')
  })

  it('derives today using the library timezone and formats date-only values without shifting them', () => {
    expect(libraryToday(new Date('2026-10-08T17:30:00.000Z'))).toBe('2026-10-09')
    expect(formatLibraryDate('2026-10-09')).toBe('09/10/2026')
    expect(formatLibraryDate(null)).toBe('—')
  })
})
