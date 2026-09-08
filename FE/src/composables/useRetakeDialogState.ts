import { ref } from 'vue'

import type { RetakeRowItem } from '@/types/retake'

export function useRetakeDialogState() {
  const visible = ref(false)
  const mode = ref<'create' | 'score' | 'cancel'>('create')
  const selectedItem = ref<RetakeRowItem | null>(null)
  const saving = ref(false)
  const error = ref<string | string[]>('')

  function openCreate(): void {
    selectedItem.value = null
    mode.value = 'create'
    error.value = ''
    visible.value = true
  }

  function openScore(item: RetakeRowItem): void {
    selectedItem.value = item
    mode.value = 'score'
    error.value = ''
    visible.value = true
  }

  function openCancel(item: RetakeRowItem): void {
    selectedItem.value = item
    mode.value = 'cancel'
    error.value = ''
    visible.value = true
  }

  return { visible, mode, selectedItem, saving, error, openCreate, openScore, openCancel }
}
