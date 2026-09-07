import type {
  CalculationStatus,
  CalculationResultSource,
  ResTermSubjectResultDTO,
  ResAnnualSubjectResultDTO,
} from '@/types/transcript'

export interface ClassTermStudentRowDTO {
  studentId: number
  studentCode: string
  fullName: string
  calculationStatus: CalculationStatus | null
  dtbhk: number | null
  subjects: ResTermSubjectResultDTO[]
}

export interface ResClassTermTranscriptDTO {
  classId: number
  classCode: string
  className: string
  academicYearId: number
  semesterId: number
  students: ClassTermStudentRowDTO[]
}

export interface ClassAnnualStudentRowDTO {
  studentId: number
  studentCode: string
  fullName: string
  calculationStatus: CalculationStatus | null
  regularDtbcn: number | null
  finalDtbcn: number | null
  resultSource: CalculationResultSource | null
  subjects: ResAnnualSubjectResultDTO[]
}

export interface ResClassAnnualTranscriptDTO {
  classId: number
  classCode: string
  className: string
  academicYearId: number
  students: ClassAnnualStudentRowDTO[]
}

export interface AccessibleClassDTO {
  id: number
  academicYearId: number
  gradeLevelId: number
  classCode: string
  className: string
  capacity: number
  status: 'ACTIVE' | 'CLOSED'
}

export type TranscriptViewMode = 'SUBJECT' | 'SUMMARY'
export type TranscriptPeriod = 'TERM' | 'ANNUAL'

