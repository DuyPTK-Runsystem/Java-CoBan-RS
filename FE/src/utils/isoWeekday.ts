export interface IsoWeekday {
  value: number
  label: string
  shortLabel: string
}

export const ISO_WEEKDAYS: readonly IsoWeekday[] = [
  { value: 1, label: 'Thứ Hai', shortLabel: 'Thứ 2' },
  { value: 2, label: 'Thứ Ba', shortLabel: 'Thứ 3' },
  { value: 3, label: 'Thứ Tư', shortLabel: 'Thứ 4' },
  { value: 4, label: 'Thứ Năm', shortLabel: 'Thứ 5' },
  { value: 5, label: 'Thứ Sáu', shortLabel: 'Thứ 6' },
  { value: 6, label: 'Thứ Bảy', shortLabel: 'Thứ 7' },
  { value: 7, label: 'Chủ Nhật', shortLabel: 'Chủ Nhật' },
]

export const SCHOOL_WEEKDAYS = ISO_WEEKDAYS.slice(0, 6)

export function getIsoWeekdayLabel(day: number, compact = false): string {
  const weekday = ISO_WEEKDAYS[day - 1]
  return weekday?.value === day ? (compact ? weekday.shortLabel : weekday.label) : 'Không xác định'
}
