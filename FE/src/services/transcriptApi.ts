import { apiClient } from '@/services/apiClient'
import type {
  ResStudentAnnualTranscriptDTO,
  ResStudentTermTranscriptDTO,
  ResTranscriptCalculationStatusDTO,
} from '@/types/transcript'

const basePath = '/api/v2/transcripts/students'

export function fetchMyTermTranscript(
  token: string,
  semesterId: number,
): Promise<ResStudentTermTranscriptDTO> {
  return apiClient.get<ResStudentTermTranscriptDTO>(
    `${basePath}/me/semesters/${semesterId}`,
    { token },
  )
}

export function fetchMyAnnualTranscript(
  token: string,
  academicYearId: number,
): Promise<ResStudentAnnualTranscriptDTO> {
  return apiClient.get<ResStudentAnnualTranscriptDTO>(
    `${basePath}/me/academic-years/${academicYearId}`,
    { token },
  )
}

export function fetchMyTermStatus(
  token: string,
  semesterId: number,
): Promise<ResTranscriptCalculationStatusDTO> {
  return apiClient.get<ResTranscriptCalculationStatusDTO>(
    `${basePath}/me/semesters/${semesterId}/status`,
    { token },
  )
}

export function fetchMyAnnualStatus(
  token: string,
  academicYearId: number,
): Promise<ResTranscriptCalculationStatusDTO> {
  return apiClient.get<ResTranscriptCalculationStatusDTO>(
    `${basePath}/me/academic-years/${academicYearId}/status`,
    { token },
  )
}

export function fetchStudentTermTranscript(
  token: string,
  studentId: number,
  semesterId: number,
): Promise<ResStudentTermTranscriptDTO> {
  return apiClient.get<ResStudentTermTranscriptDTO>(
    `${basePath}/${studentId}/semesters/${semesterId}`,
    { token },
  )
}

export function fetchStudentAnnualTranscript(
  token: string,
  studentId: number,
  academicYearId: number,
): Promise<ResStudentAnnualTranscriptDTO> {
  return apiClient.get<ResStudentAnnualTranscriptDTO>(
    `${basePath}/${studentId}/academic-years/${academicYearId}`,
    { token },
  )
}
