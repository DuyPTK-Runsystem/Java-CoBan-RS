import { formatApiDateTime, parseApiDateTime } from './dateFormat'

export function formatCalculationDateTime(value: string | null | undefined): string {
  return formatApiDateTime(value, { empty: '—' })
}

export function formatShortDateTime(value: string | null | undefined): string {
  if (!value) return '—'
  const parts = parseApiDateTime(value)
  if (!parts) return value
  if (!parts.hour || !parts.minute) return `${parts.day}/${parts.month}`
  return `${parts.day}/${parts.month} ${parts.hour}:${parts.minute}`
}
