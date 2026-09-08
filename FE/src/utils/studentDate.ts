import { formatApiDate } from './dateFormat'

export function formatStudentDate(value: string | null | undefined): string {
  return formatApiDate(value, { dateSeparator: '-', empty: '—', invalid: 'empty' })
}
