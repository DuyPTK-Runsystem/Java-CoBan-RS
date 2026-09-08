import { nextTick, ref } from 'vue'

import type { ScoreChangeRequestFormContext } from '@/components/score-change/ScoreChangeRequestForm.vue'
import type { AssessmentColumn, ScoreGridColumn, ScoreStatus, StudentScore, StudentScoreGridRow } from '@/types/scorebook'

export function useScorebookDialogs() {
  const columnDialogVisible = ref(false)
  const columnDialogMode = ref<'create' | 'edit'>('create')
  const selectedColumn = ref<AssessmentColumn | null>(null)
  const scoreDialogVisible = ref(false)
  const scoreChangeRequestDialogVisible = ref(false)
  const bulkDialogVisible = ref(false)
  const selectedStudent = ref<StudentScoreGridRow | null>(null)
  const selectedScore = ref<StudentScore | null>(null)
  const selectedGridColumn = ref<ScoreGridColumn | null>(null)
  const scoreChangeRequestContext = ref<ScoreChangeRequestFormContext | null>(null)
  const dialogError = ref('')

  function reset(): void {
    columnDialogVisible.value = false
    scoreDialogVisible.value = false
    scoreChangeRequestDialogVisible.value = false
    bulkDialogVisible.value = false
    selectedColumn.value = null
    selectedStudent.value = null
    selectedScore.value = null
    selectedGridColumn.value = null
    scoreChangeRequestContext.value = null
    dialogError.value = ''
  }

  function openColumn(mode: 'create' | 'edit', column: AssessmentColumn | null = null): void {
    dialogError.value = ''
    columnDialogMode.value = mode
    selectedColumn.value = column
    columnDialogVisible.value = true
  }

  function openScore(student: StudentScoreGridRow, column: ScoreGridColumn, score: StudentScore | null): void {
    dialogError.value = ''
    selectedStudent.value = student
    selectedGridColumn.value = column
    selectedScore.value = score
    scoreDialogVisible.value = true
  }

  function openBulk(column: ScoreGridColumn): void {
    dialogError.value = ''
    selectedGridColumn.value = column
    bulkDialogVisible.value = true
  }

  async function openChangeRequest(context: { studentName: string; score: StudentScore | null; proposedStatus: ScoreStatus; proposedValue: number | null; reason: string }): Promise<void> {
    if (!selectedStudent.value || !selectedGridColumn.value) return
    scoreDialogVisible.value = false
    await nextTick()
    scoreChangeRequestContext.value = {
      studentCode: selectedStudent.value.studentCode,
      studentName: selectedStudent.value.studentName || context.studentName,
      columnId: selectedGridColumn.value.columnId,
      columnName: selectedGridColumn.value.columnName,
      currentStatus: context.score?.scoreStatus,
      currentValue: context.score?.scoreValue ?? null,
      proposedStatus: context.proposedStatus,
      proposedValue: context.proposedValue,
      reason: context.reason,
    }
    scoreChangeRequestDialogVisible.value = true
  }

  return { columnDialogVisible, columnDialogMode, selectedColumn, scoreDialogVisible, scoreChangeRequestDialogVisible, bulkDialogVisible, selectedStudent, selectedScore, selectedGridColumn, scoreChangeRequestContext, dialogError, reset, openColumn, openScore, openBulk, openChangeRequest }
}
