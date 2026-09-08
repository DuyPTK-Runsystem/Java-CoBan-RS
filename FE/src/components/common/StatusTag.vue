<script setup lang="ts">
import Tag from 'primevue/tag'

import type { CalculationStatus } from '@/types/ui'

type TagSeverity = 'secondary' | 'success' | 'info' | 'warn' | 'danger' | 'contrast'

const props = withDefaults(defineProps<{
  status?: CalculationStatus
  label?: string
  severity?: TagSeverity
}>(), {
  status: undefined,
  label: '',
  severity: undefined,
})

const statusLabels: Record<CalculationStatus, string> = {
  IN_PROGRESS: 'In progress',
  FINISH: 'Finished',
}

const statusSeverities: Record<CalculationStatus, TagSeverity> = {
  IN_PROGRESS: 'warn',
  FINISH: 'success',
}

function displayLabel(): string {
  return props.label || (props.status ? statusLabels[props.status] : '')
}

function displaySeverity(): TagSeverity | undefined {
  return props.severity ?? (props.status ? statusSeverities[props.status] : undefined)
}
</script>

<template>
  <Tag :value="displayLabel()" :severity="displaySeverity()" />
</template>
