import { USER_ROLE, type UserRole } from '@/types/user'

export const studentWorkspacePaths = ['/v2/attendance', '/v2/transcripts', '/v2/notifications'] as const

export function isStudentWorkspace(roles: UserRole[]): boolean {
  return roles.includes(USER_ROLE.STUDENT)
    && !roles.some((role) => role === USER_ROLE.ADMIN || role === USER_ROLE.ACADEMIC_OFFICE || role === USER_ROLE.TEACHER)
}

export function isStudentWorkspacePath(path: string): boolean {
  return studentWorkspacePaths.some((allowedPath) => path === allowedPath || path === `${allowedPath}/`)
}

export function isTeacherWorkspace(roles: UserRole[]): boolean {
  return roles.includes(USER_ROLE.TEACHER)
    && !roles.some((role) => role === USER_ROLE.ADMIN || role === USER_ROLE.ACADEMIC_OFFICE)
}

export function firstPermittedWorkspacePath(roles: UserRole[]): string {
  if (isStudentWorkspace(roles)) return '/v2/attendance'
  if (isTeacherWorkspace(roles)) return '/v2/academic-catalog/classes'
  if (roles.includes(USER_ROLE.LIBRARIAN) && !roles.some((role) => role === USER_ROLE.ADMIN || role === USER_ROLE.ACADEMIC_OFFICE)) return '/v2/library/books'
  if (roles.includes(USER_ROLE.ADMIN) || roles.includes(USER_ROLE.ACADEMIC_OFFICE)) return '/v2/academic-years'
  return '/v2/attendance'
}
