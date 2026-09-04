export type RetakeExamStatus = 'PLANNED' | 'SCORED' | 'CANCELLED'

export interface ResRetakeExamDTO {
  retakeId: number
  studentId: number
  academicYearId: number
  subjectId: number
  preRetakeScore: number | null
  retakeScore: number | null
  examDate: string | null
  status: RetakeExamStatus
  note: string | null
  createdBy?: number | null
  updatedBy?: number | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface ReqFilterRetakeExamDTO {
  studentId?: number
  academicYearId?: number
  subjectId?: number
  status?: RetakeExamStatus
  page: number
  size: number
}

export interface ReqCreateRetakeExamDTO {
  studentId: number
  academicYearId: number
  subjectId: number
  examDate?: string
  retakeScore?: number
  note?: string
}

export interface ReqUpdateRetakeScoreDTO {
  retakeScore: number
  examDate?: string
  note?: string
}

export interface RetakeExamPage {
  content: ResRetakeExamDTO[]
  page?: number
  number?: number
  size: number
  totalElements: number
  totalPages: number
}

export interface RetakeRowItem extends ResRetakeExamDTO {
  studentCode?: string
  studentName?: string
  academicYearCode?: string
  subjectName?: string
  officialDtbmhCn?: number | null
  calculationStatus?: 'IN_PROGRESS' | 'FINISH' | null
  calculationSource?: 'REGULAR' | 'RETAKE' | null
  lastTaskId?: number | null
}
