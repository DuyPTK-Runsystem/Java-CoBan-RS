import { ref, type ComputedRef, type Ref } from 'vue'

import { retryAllFailedCalculationTasks, retryCalculationTask } from '@/services/calculationTaskApi'
import { extractApiErrorMessage, isApiError } from '@/types/api'
import type { ResCalculationTaskDTO } from '@/types/calculationTask'

interface CalculationRetryOptions {
  token: ComputedRef<string>
  conflictMessage: Ref<string | null>
  errorMessage: Ref<string | null>
  successMessage: Ref<string | null>
  reloadTasks: () => Promise<void>
  reloadTranscriptStatus: () => Promise<void>
  reloadAuditLogs: () => Promise<void>
}

export function useCalculationRetry(options: CalculationRetryOptions) {
  const selectedDetailTask = ref<ResCalculationTaskDTO | null>(null)
  const isDetailModalVisible = ref(false)
  const isRetryModalVisible = ref(false)
  const retryMode = ref<'single' | 'bulk'>('single')
  const targetRetryTask = ref<ResCalculationTaskDTO | null>(null)
  const retryingTaskId = ref<number | null>(null)
  const retrying = ref(false)

  function openDetail(task: ResCalculationTaskDTO): void {
    selectedDetailTask.value = task
    isDetailModalVisible.value = true
  }

  function openSingleRetry(task: ResCalculationTaskDTO): void {
    targetRetryTask.value = task
    retryMode.value = 'single'
    isRetryModalVisible.value = true
  }

  function openBulkRetry(): void {
    targetRetryTask.value = null
    retryMode.value = 'bulk'
    isRetryModalVisible.value = true
  }

  async function confirmRetry(): Promise<void> {
    if (!options.token.value) return
    retrying.value = true
    options.conflictMessage.value = null
    options.errorMessage.value = null
    options.successMessage.value = null
    try {
      if (retryMode.value === 'single' && targetRetryTask.value) {
        retryingTaskId.value = targetRetryTask.value.taskId
        await retryCalculationTask(options.token.value, targetRetryTask.value.taskId)
        options.successMessage.value = `Đã yêu cầu retry task #CT-${targetRetryTask.value.taskId}. Trạng thái đã chuyển về PENDING.`
      } else {
        const retried = await retryAllFailedCalculationTasks(options.token.value)
        options.successMessage.value = `Đã yêu cầu retry toàn bộ ${retried.length} task FAILED. Các task đã chuyển về PENDING.`
      }
      isRetryModalVisible.value = false
      isDetailModalVisible.value = false
      await options.reloadTasks()
      await options.reloadTranscriptStatus()
      await options.reloadAuditLogs()
    } catch (error) {
      if (isApiError(error, 409)) {
        options.conflictMessage.value = 'Task đã đổi trạng thái hoặc không còn FAILED. Dữ liệu mới nhất đã được tải; hãy kiểm tra trước khi retry lại.'
        isRetryModalVisible.value = false
        await options.reloadTasks()
        await options.reloadTranscriptStatus()
      } else {
        options.errorMessage.value = extractApiErrorMessage(error, 'Thao tác retry thất bại. Vui lòng thử lại.')
      }
    } finally {
      retrying.value = false
      retryingTaskId.value = null
    }
  }

  return { selectedDetailTask, isDetailModalVisible, isRetryModalVisible, retryMode, targetRetryTask, retryingTaskId, retrying, openDetail, openSingleRetry, openBulkRetry, confirmRetry }
}
