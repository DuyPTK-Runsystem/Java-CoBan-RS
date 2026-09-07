import type { UserRole } from '@/types/user'

export const studentWorkspacePaths = ['/v2/attendance', '/v2/transcripts'] as const

export function isStudentWorkspace(roles: UserRole[]): boolean {
  return roles.includes('STUDENT')
    && !roles.some((role) => role === 'ADMIN' || role === 'ACADEMIC_OFFICE' || role === 'TEACHER')
}

export function isStudentWorkspacePath(path: string): boolean {
  return studentWorkspacePaths.some((allowedPath) => path === allowedPath || path === `${allowedPath}/`)
}

export function isTeacherWorkspace(roles: UserRole[]): boolean {
  return roles.includes('TEACHER')
    && !roles.some((role) => role === 'ADMIN' || role === 'ACADEMIC_OFFICE')
}

export function firstPermittedWorkspacePath(roles: UserRole[]): string {
  if (isStudentWorkspace(roles)) return '/v2/attendance'
  if (isTeacherWorkspace(roles)) return '/v2/academic-catalog/classes'
  if (roles.includes('ADMIN') || roles.includes('ACADEMIC_OFFICE')) return '/v2/academic-years'
  return '/v2/attendance'
}
