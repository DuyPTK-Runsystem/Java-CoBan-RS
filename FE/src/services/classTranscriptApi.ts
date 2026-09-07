import { apiClient } from '@/services/apiClient'
import type {
  AccessibleClassDTO,
  ResClassAnnualTranscriptDTO,
  ResClassTermTranscriptDTO,
} from '@/types/classTranscript'

const classTranscriptBasePath = '/api/v2/transcripts/classes'
const classBasePath = '/api/v2/classes'

export function fetchAccessibleClasses(
  token: string,
  academicYearId?: number,
): Promise<AccessibleClassDTO[]> {
  const query = academicYearId ? `?academicYearId=${academicYearId}` : ''
  return apiClient.get<AccessibleClassDTO[]>(
    `${classBasePath}/accessible-for-transcript${query}`,
    { token },
  )
}

export function fetchClassTermTranscript(
  token: string,
  classId: number,
  semesterId: number,
): Promise<ResClassTermTranscriptDTO> {
  return apiClient.get<ResClassTermTranscriptDTO>(
    `${classTranscriptBasePath}/${classId}/semesters/${semesterId}`,
    { token },
  )
}

export function fetchClassAnnualTranscript(
  token: string,
  classId: number,
  academicYearId: number,
): Promise<ResClassAnnualTranscriptDTO> {
  return apiClient.get<ResClassAnnualTranscriptDTO>(
    `${classTranscriptBasePath}/${classId}/academic-years/${academicYearId}`,
    { token },
  )
}

