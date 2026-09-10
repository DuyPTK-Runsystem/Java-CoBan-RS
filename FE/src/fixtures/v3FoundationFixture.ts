import type { V3ReviewContext, V3ReviewItem } from '@/types/v3Foundation'

export const v3FoundationReviewContext: V3ReviewContext = {
  academicYear: '2026–2027',
  semester: 'Học kỳ I',
  module: 'Xếp lớp theo tiêu chí',
}

export const v3FoundationReviewItems: V3ReviewItem[] = [
  { identity: 'HS240001 · Nguyễn Minh An', status: 'Đủ dữ liệu', scope: 'Khối 6', updatedAt: '10/09/2026 09:30' },
  { identity: 'HS240002 · Trần Gia Huy', status: 'Cần xem lại', scope: 'Khối 6', updatedAt: '10/09/2026 09:18' },
]
