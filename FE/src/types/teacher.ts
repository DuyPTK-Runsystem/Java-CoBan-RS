export type TeacherStatus = 'ACTIVE' | 'ON_LEAVE' | 'INACTIVE'

export interface Teacher {
  id: number
  userId: number | null
  teacherCode: string
  teacherName: string
  dateOfBirth: string | null
  gender: string | null
  phone: string | null
  email: string | null
  department: string | null
  joinDate: string | null
  status: TeacherStatus
}

export interface TeacherFormValues {
  userId: number | null
  teacherCode: string
  teacherName: string
  dateOfBirth: string
  gender: string
  phone: string
  email: string
  department: string
  joinDate: string
  status: TeacherStatus
}

export interface CreateTeacherRequest {
  userId: number | null
  teacherCode: string
  teacherName: string
  dateOfBirth: string | null
  gender: string | null
  phone: string | null
  email: string | null
  department: string | null
  joinDate: string | null
  status: TeacherStatus
}

export type UpdateTeacherRequest = CreateTeacherRequest
