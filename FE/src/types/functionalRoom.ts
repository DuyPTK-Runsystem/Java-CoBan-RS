export type RoomStatus = 'ACTIVE' | 'INACTIVE'

export interface FunctionalRoom {
  id: number
  code: string
  name: string
  status: RoomStatus
  version: number
  createdAt: string
  updatedAt: string
}

export interface CreateFunctionalRoomPayload {
  code: string
  name: string
}

export interface UpdateFunctionalRoomPayload {
  code: string
  name: string
  status: RoomStatus
  expectedVersion: number
}

export interface SubjectFunctionalRooms {
  subjectId: number
  rooms: FunctionalRoom[]
}

export interface UpdateSubjectFunctionalRoomsPayload {
  functionalRoomIds: number[]
  expectedVersion?: number | null
}

