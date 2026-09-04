export function formatAcademicDate(value: string | null | undefined): string {
  if (!value) return '-'
  const [date] = value.split('T')
  const [year, month, day] = date.split('-')
  return year && month && day ? `${day}/${month}/${year}` : value
}

export function formatAcademicDateTime(value: string | null | undefined): string {
  if (!value) return '-'
  const [date, time] = value.split('T')
  const [year, month, day] = date.split('-')
  if (!year || !month || !day) return value.replace('T', ' ')
  return `${day}/${month}/${year}${time ? ` ${time}` : ''}`
}

function isValidDate(value: Date): boolean {
  return !Number.isNaN(value.getTime())
}

export function parseAcademicDate(value: string | null | undefined): Date | null {
  if (!value) return null
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value)
  if (!match) return null
  const date = new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]))
  return isValidDate(date)
    && date.getFullYear() === Number(match[1])
    && date.getMonth() === Number(match[2]) - 1
    && date.getDate() === Number(match[3])
    ? date
    : null
}

export function formatAcademicDateInput(value: Date | null): string {
  if (!value || !isValidDate(value)) return ''
  const pad = (part: number) => String(part).padStart(2, '0')
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`
}

export function parseAcademicDateTime(value: string | null | undefined): Date | null {
  if (!value) return null
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$/.exec(value)
  if (!match) return null
  const date = new Date(
    Number(match[1]),
    Number(match[2]) - 1,
    Number(match[3]),
    Number(match[4]),
    Number(match[5]),
    Number(match[6] ?? 0),
  )
  return isValidDate(date) ? date : null
}

export function formatAcademicDateTimeInput(value: Date | null): string {
  if (!value || !isValidDate(value)) return ''
  const pad = (part: number) => String(part).padStart(2, '0')
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}T${pad(value.getHours())}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`
}
