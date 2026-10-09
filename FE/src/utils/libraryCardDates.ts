const LIBRARY_TIME_ZONE = 'Asia/Ho_Chi_Minh'

function datePartsInLibraryTimeZone(date: Date): { year: number; month: number; day: number } {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: LIBRARY_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(date)
  const value = (type: string) => Number(parts.find((part) => part.type === type)?.value)
  return { year: value('year'), month: value('month'), day: value('day') }
}

function dateString(year: number, month: number, day: number): string {
  return `${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`
}

function daysInMonth(year: number, month: number): number {
  return new Date(Date.UTC(year, month, 0)).getUTCDate()
}

/** Returns the issue date in the library business timezone as an API LocalDate. */
export function libraryToday(date = new Date()): string {
  const { year, month, day } = datePartsInLibraryTimeZone(date)
  return dateString(year, month, day)
}

/**
 * Calculates an inclusive expiry date. A duration ends the day before its
 * anniversary; month-end dates are clamped before subtracting that final day.
 */
export function expiryDateAfterMonths(months: number, issuedOn = libraryToday()): string {
  if (!Number.isInteger(months) || months < 1) throw new Error('Thời hạn phải từ 1 tháng trở lên.')
  const [year, month, day] = issuedOn.split('-').map(Number)
  const absoluteMonth = year * 12 + month - 1 + months
  const targetYear = Math.floor(absoluteMonth / 12)
  const targetMonth = (absoluteMonth % 12) + 1
  const anniversaryDay = Math.min(day, daysInMonth(targetYear, targetMonth))
  const inclusiveExpiry = new Date(Date.UTC(targetYear, targetMonth - 1, anniversaryDay - 1))
  return dateString(inclusiveExpiry.getUTCFullYear(), inclusiveExpiry.getUTCMonth() + 1, inclusiveExpiry.getUTCDate())
}

export function formatLibraryDate(value: string | null | undefined): string {
  if (!value) return '—'
  const [year, month, day] = value.slice(0, 10).split('-').map(Number)
  if (!year || !month || !day) return value
  return `${day.toString().padStart(2, '0')}/${month.toString().padStart(2, '0')}/${year}`
}
