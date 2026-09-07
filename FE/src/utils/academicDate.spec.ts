import { describe, expect, it } from 'vitest'

import { formatAcademicDate, formatAcademicDateInput, formatAcademicDateTime, formatAcademicDateTimeInput, parseAcademicDate, parseAcademicDateTime } from './academicDate'

describe('academic date formatting', () => {
  it('formats date-only values as dd/mm/yyyy', () => {
    expect(formatAcademicDate('2027-01-15')).toBe('15/01/2027')
  })

  it('round-trips a date-only value without timezone conversion', () => {
    expect(formatAcademicDateInput(parseAcademicDate('2027-01-15'))).toBe('2027-01-15')
  })
})

describe('formatAcademicDateTime', () => {
  it('formats local datetime without timezone conversion', () => {
    expect(formatAcademicDateTime('2027-01-15T17:00:00')).toBe('15/01/2027 17:00:00')
  })

  it('round-trips a local datetime without timezone conversion', () => {
    expect(formatAcademicDateTimeInput(parseAcademicDateTime('2027-01-15T17:00:00'))).toBe('2027-01-15T17:00:00')
  })

  it('keeps empty values readable', () => {
    expect(formatAcademicDateTime(null)).toBe('-')
  })
})
