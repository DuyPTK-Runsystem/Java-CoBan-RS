export type AssignmentStatus = 'ACTIVE' | 'ENDED'

export interface HomeroomAssignment {
  id: number
  classId: number
  teacherId: number
  validFrom: string
  validTo: string | null
  status: AssignmentStatus
  assignedBy: number | null
}

export interface SubjectTeachingAssignment {
  id: number
  classSubjectId: number
  teacherId: number
  validFrom: string
  validTo: string | null
  status: AssignmentStatus
  assignedBy: number | null
}

export interface AssignmentDateValues {
  teacherId: number | null
  validFrom: string
  validTo: string
}

export type CreateHomeroomAssignmentRequest = { teacherId: number; validFrom: string; validTo: string | null }
export type ReplaceHomeroomAssignmentRequest = CreateHomeroomAssignmentRequest
export type EndHomeroomAssignmentRequest = { validTo: string }
export type CreateSubjectTeachingAssignmentRequest = CreateHomeroomAssignmentRequest
export type ReplaceSubjectTeachingAssignmentRequest = CreateHomeroomAssignmentRequest
export type EndSubjectTeachingAssignmentRequest = EndHomeroomAssignmentRequest

export interface AssignmentFormValues extends AssignmentDateValues {
  mode: 'create' | 'replace' | 'end'
}
