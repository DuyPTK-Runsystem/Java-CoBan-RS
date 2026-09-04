export type SubjectType = 'ACADEMIC' | 'SKILL'
export type CalculationStatus = 'IN_PROGRESS' | 'FINISH'
export type CalculationResultSource = 'REGULAR' | 'RETAKE'
export type AssessmentType = 'KTTT' | 'KTTX' | 'KTDK' | 'KTĐK' | 'KTCK'
export type RetakeExamStatus = 'PLANNED' | 'SCORED' | 'CANCELLED'

export interface ResAssessmentColumnDTO {
  columnId: number
  assessmentType: AssessmentType
  columnNo: number
  columnName: string
  scoreStatus: string
  scoreValue: number | null
}

export interface ResTransferNoteDTO {
  fromClassId: number
  toClassId: number
  effectiveAt: string
}

export interface ResTermSubjectResultDTO {
  subjectId: number
  subjectName: string
  subjectType: SubjectType
  dtbmh: number | null
  skillScore: number | null
  calculatedVersion: number | null
  calculatedAt: string | null
  assessmentColumns: ResAssessmentColumnDTO[]
}

export interface ResStudentTermTranscriptDTO {
  studentId: number
  academicYearId: number
  semesterId: number
  calculationStatus: CalculationStatus
  sourceVersion: number | null
  calculatedVersion: number | null
  calculatedAt: string | null
  dtbhk: number | null
  transferNotes: ResTransferNoteDTO[]
  subjects: ResTermSubjectResultDTO[]
}

export interface ResRetakeDetailDTO {
  retakeId: number
  preRetakeScore: number | null
  retakeScore: number | null
  examDate: string | null
  status: RetakeExamStatus
  note: string | null
}

export interface ResAnnualSubjectResultDTO {
  subjectId: number
  subjectName: string
  subjectType: SubjectType
  hk1: number | null
  hk2: number | null
  regularDtbmhCn: number | null
  officialDtbmhCn: number | null
  calculationSource: CalculationResultSource | null
  calculatedVersion: number | null
  calculatedAt: string | null
  retake: ResRetakeDetailDTO | null
}

export interface ResStudentAnnualTranscriptDTO {
  studentId: number
  academicYearId: number
  calculationStatus: CalculationStatus
  sourceVersion: number | null
  calculatedVersion: number | null
  calculatedAt: string | null
  regularDtbcn: number | null
  finalDtbcn: number | null
  resultSource: CalculationResultSource | null
  lastCalculationTaskId: number | null
  transferNotes: ResTransferNoteDTO[]
  subjects: ResAnnualSubjectResultDTO[]
}

export interface ResTranscriptCalculationStatusDTO {
  studentId: number
  studentCode?: string
  academicYearId?: number
  semesterId?: number | null
  targetType?: 'SEMESTER' | 'ACADEMIC_YEAR'
  targetId?: number
  calculationStatus: CalculationStatus
  sourceVersion: number | null
  calculatedVersion: number | null
  isUpToDate?: boolean
  calculatedAt: string | null
  lastError?: string | null
  lastTaskId?: number | null
}
