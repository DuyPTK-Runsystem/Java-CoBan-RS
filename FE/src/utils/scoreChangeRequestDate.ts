export function formatScoreChangeRequestDateTime(value: string | null | undefined): string {
  if (!value) return '-'
  const match = /^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2})(?::(\d{2}))?)?/.exec(value)
  if (!match) return value
  const [, year, month, day, hour, minute, second] = match
  if (!hour || !minute) return `${day}-${month}-${year}`
  return `${day}-${month}-${year} ${hour}:${minute}:${second ?? '00'}`
}
