import { describe, expect, it } from 'vitest'

import { formatStudentDate } from './studentDate'

describe('formatStudentDate', () => {
  it('formats an API date-only string for the Student UI', () => {
    expect(formatStudentDate('2026-08-19')).toBe('19-08-2026')
  })

  it.each([null, undefined, '', '19/08/2026', 'invalid'])('uses a safe fallback for %s', (value) => {
    expect(formatStudentDate(value)).toBe('—')
  })
})
