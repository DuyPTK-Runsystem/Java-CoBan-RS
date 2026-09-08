import { ref, type Ref } from 'vue'

import { fetchAcademicYears, fetchSemesters } from '@/services/academicApi'
import { isApiError } from '@/types/api'
import type { AcademicYear, Semester } from '@/types/academic'

type TranscriptQuery = Record<string, unknown>

export function useTranscriptContext(
  token: Readonly<Ref<string>>,
  routeQuery: Readonly<Ref<TranscriptQuery>>,
) {
  const academicYears = ref<AcademicYear[]>([])
  const semesters = ref<Semester[]>([])
  const selectedAcademicYearId = ref<number | null>(null)
  const selectedSemesterId = ref<number | null>(null)
  const contextLoading = ref(false)
  const errorMessage = ref('')

  async function loadContext(): Promise<void> {
    if (!token.value) return
    contextLoading.value = true
    errorMessage.value = ''
    try {
      const years = await fetchAcademicYears(token.value)
      academicYears.value = years
      if (years.length > 0) {
        const queryYearId = routeQuery.value.academicYearId
          ? Number(routeQuery.value.academicYearId)
          : null
        const matchedYear = queryYearId ? years.find((year) => year.id === queryYearId) : null
        const activeYear = matchedYear ?? years.find((year) => year.status === 'ACTIVE') ?? years[0]
        selectedAcademicYearId.value = activeYear.id
      }
    } catch (error: unknown) {
      errorMessage.value = isApiError(error)
        ? error.message
        : 'Không thể tải danh sách năm học.'
    } finally {
      contextLoading.value = false
    }
  }

  async function loadSemesters(academicYearId: number): Promise<void> {
    if (!token.value) return
    try {
      const loadedSemesters = await fetchSemesters(token.value, academicYearId)
      semesters.value = loadedSemesters
      if (loadedSemesters.length === 0) {
        selectedSemesterId.value = null
        return
      }

      const querySemesterId = routeQuery.value.semesterId
        ? Number(routeQuery.value.semesterId)
        : null
      const matchedSemester = querySemesterId
        ? loadedSemesters.find((semester) => semester.id === querySemesterId)
        : null
      const activeSemester = matchedSemester
        ?? loadedSemesters.find((semester) => semester.status === 'ACTIVE')
        ?? loadedSemesters[0]
      selectedSemesterId.value = activeSemester.id
    } catch (error: unknown) {
      if (isApiError(error)) errorMessage.value = error.message
    }
  }

  return {
    academicYears,
    semesters,
    selectedAcademicYearId,
    selectedSemesterId,
    contextLoading,
    errorMessage,
    loadContext,
    loadSemesters,
  }
}
