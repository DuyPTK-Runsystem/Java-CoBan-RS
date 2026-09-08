export interface ApiDateTimeParts {
  year: string
  month: string
  day: string
  hour?: string
  minute?: string
  second?: string
}

const apiDateTimePattern = /^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2})(?::(\d{2}))?)?/

export function parseApiDateTime(value: string | null | undefined): ApiDateTimeParts | null {
  if (!value) return null
  const match = apiDateTimePattern.exec(value)
  if (!match) return null
  const [, year, month, day, hour, minute, second] = match
  const date = new Date(Number(year), Number(month) - 1, Number(day))
  if (date.getFullYear() !== Number(year)
    || date.getMonth() !== Number(month) - 1
    || date.getDate() !== Number(day)) return null
  if (hour !== undefined && (Number(hour) > 23 || Number(minute) > 59 || (second !== undefined && Number(second) > 59))) {
    return null
  }
  return { year, month, day, hour, minute, second }
}

export function formatApiDate(
  value: string | null | undefined,
  options: { dateSeparator?: string; empty?: string; invalid?: 'value' | 'empty' } = {},
): string {
  if (!value) return options.empty ?? '-'
  const parts = parseApiDateTime(value)
  if (!parts) return options.invalid === 'empty' ? options.empty ?? '-' : value
  const separator = options.dateSeparator ?? '/'
  return `${parts.day}${separator}${parts.month}${separator}${parts.year}`
}

export function formatApiDateTime(
  value: string | null | undefined,
  options: { dateSeparator?: string; empty?: string; includeSeconds?: boolean } = {},
): string {
  if (!value) return options.empty ?? '-'
  const parts = parseApiDateTime(value)
  if (!parts) return value
  const date = formatApiDate(value, { dateSeparator: options.dateSeparator, empty: options.empty })
  if (!parts.hour || !parts.minute) return date
  const seconds = options.includeSeconds === false ? '' : `:${parts.second ?? '00'}`
  return `${date} ${parts.hour}:${parts.minute}${seconds}`
}
