import { apiClient } from '@/services/apiClient'
import type {
  SubjectFunctionalRooms,
  UpdateSubjectFunctionalRoomsPayload,
} from '@/types/functionalRoom'

const subjectsPath = '/api/v2/subjects'

export function getRoomsForSubject(
  subjectId: number,
  token?: string,
): Promise<SubjectFunctionalRooms> {
  return apiClient.get<SubjectFunctionalRooms>(`${subjectsPath}/${subjectId}/functional-rooms`, {
    token,
  })
}

export function updateRoomsForSubject(
  subjectId: number,
  payload: UpdateSubjectFunctionalRoomsPayload,
  token?: string,
): Promise<SubjectFunctionalRooms> {
  return apiClient.put<SubjectFunctionalRooms>(
    `${subjectsPath}/${subjectId}/functional-rooms`,
    payload,
    { token },
  )
}

