export interface Student {
  studentId: number
  studentCode: string
  studentName: string
  dateOfBirth: string
  address: string
  averageScore: number | null
}

export interface StudentSearchValues {
  studentCode: string
  studentName: string
  dateOfBirth: Date | null
}

export interface StudentFormValues {
  studentId?: number
  studentCode: string
  studentName: string
  dateOfBirth: Date | null
  address: string
  averageScore: number | null
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

export interface StudentPayload {
  studentCode?: string
  studentName: string
  dateOfBirth: string | null
  address: string
  averageScore: number | null
}
