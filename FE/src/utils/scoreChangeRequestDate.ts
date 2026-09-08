import { formatApiDateTime } from './dateFormat'

export function formatScoreChangeRequestDateTime(value: string | null | undefined): string {
  return formatApiDateTime(value, { dateSeparator: '-', empty: '-' })
}
