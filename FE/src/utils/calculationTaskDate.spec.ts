import { describe, expect, it } from 'vitest'

import { formatCalculationDateTime, formatShortDateTime } from './calculationTaskDate'

describe('calculationTaskDate', () => {
  it('formats full date time correctly', () => {
    expect(formatCalculationDateTime(null)).toBe('—')
    expect(formatCalculationDateTime('2026-09-03T09:12:44')).toBe('03/09/2026 09:12:44')
    expect(formatCalculationDateTime('2026-09-03T09:12')).toBe('03/09/2026 09:12:00')
    expect(formatCalculationDateTime('2026-09-03')).toBe('03/09/2026')
  })

  it('formats short date time correctly', () => {
    expect(formatShortDateTime(null)).toBe('—')
    expect(formatShortDateTime('2026-09-03T09:12:44')).toBe('03/09 09:12')
  })
})
