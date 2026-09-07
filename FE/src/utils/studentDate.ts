const dateOnlyPattern = /^(\d{4})-(\d{2})-(\d{2})$/

export function formatStudentDate(value: string | null | undefined): string {
  if (!value) {
    return '—'
  }

  const match = dateOnlyPattern.exec(value)
  if (!match) {
    return '—'
  }

  const [, year, month, day] = match
  return `${day}-${month}-${year}`
}
