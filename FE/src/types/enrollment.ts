export type EnrollmentStatus = 'ACTIVE' | 'COMPLETED' | 'WITHDRAWN'

export interface UnassignedStudent {
  studentId: number
  studentCode: string
  studentName: string
}

export interface ClassStudent {
  studentId: number
  studentCode: string
  studentName: string
  enrollmentId: number
}

export interface Enrollment {
  id: number
  studentId: number
  studentCode: string
  studentName: string
  academicYearId: number
  currentClassId: number
  currentClassCode: string
  status: EnrollmentStatus
  enrolledAt: string
  completedAt: string | null
}

export interface TransferHistory {
  transferId: number
  fromClassId: number | null
  toClassId: number
  effectiveAt: string
  reason: string | null
  approvedBy: number | null
}

export interface StudentEnrollmentHistory {
  enrollment: Enrollment
  transfers: TransferHistory[]
}

export interface CapacityWarning {
  classId: number
  academicYearId: number
  gradeLevelId: number
  activeStudentCount: number
  gradeAverage: number
  message: string
}

export interface EnrollmentMutation {
  enrollments: Enrollment[]
  warnings: CapacityWarning[]
}

export interface CreateEnrollmentRequest {
  studentId?: number | null
  studentCode?: string | null
  academicYearId: number
  classId: number
  enrolledAt?: string | null
}

export interface BulkCreateEnrollmentRequest {
  academicYearId: number
  classId: number
  studentIds?: number[] | null
  studentCodes?: string[] | null
  enrolledAt?: string | null
}

export interface TransferEnrollmentRequest {
  targetClassId: number
  effectiveAt: string
  reason?: string | null
}

export interface CreateEnrollmentFormValues {
  studentId: number | null
  enrolledAt: string
}

export interface BulkEnrollmentFormValues {
  studentIds: number[]
  enrolledAt: string
}

export interface TransferEnrollmentFormValues {
  targetClassId: number | null
  effectiveAt: string
  reason: string
}
