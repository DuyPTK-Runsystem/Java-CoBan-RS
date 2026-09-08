export type StudentAcademicStatus = 'ACTIVE' | 'INACTIVE' | 'GRADUATED'

export interface StudentAccountSummary {
  userId: number
  username: string
  role: string
}

export interface Student {
  studentId: number
  studentCode: string
  studentName: string
  dateOfBirth: string | null
  address: string | null
  status: StudentAcademicStatus
  currentClassCode: string | null
  currentClassId: number | null
  account: StudentAccountSummary | null
}

export interface StudentSearchValues {
  studentCode: string
  studentName: string
  dateOfBirth: Date | null
  status?: StudentAcademicStatus | ''
  classId?: number | null
}

export interface StudentFormValues {
  studentId?: number
  studentCode: string
  studentName: string
  dateOfBirth: Date | null
  address: string
  status: StudentAcademicStatus
  provisionAccount?: boolean
  username?: string
  password?: string
}

export interface StudentQuery {
  page: number
  pageSize: number
  sortField: keyof Student
  sortOrder: 1 | -1
  search: StudentSearchValues
}

export interface StudentPage {
  content: Student[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface StudentV2Payload {
  studentCode: string
  studentName: string
  dateOfBirth: string | null
  address: string | null
}

export interface StudentV3CreateRequest {
  studentCode: string
  studentName: string
  dateOfBirth: string | null
  address?: string | null
  username?: string | null
  password?: string | null
}

export interface StudentV3CreateResponse {
  studentId: number
  studentCode: string
  studentName: string
  dateOfBirth?: string | null
  address?: string | null
  account: StudentAccountSummary
}
