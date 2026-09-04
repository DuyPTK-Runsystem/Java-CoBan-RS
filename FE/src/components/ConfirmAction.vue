<script setup lang="ts">
import Button from 'primevue/button'
import ConfirmDialog from 'primevue/confirmdialog'
import { useConfirm } from 'primevue/useconfirm'

const props = withDefaults(defineProps<{
  message: string
  label?: string
  icon?: string
  header?: string
  severity?: 'primary' | 'secondary' | 'success' | 'info' | 'warn' | 'danger' | 'contrast'
  disabled?: boolean
  loading?: boolean
  acceptLabel?: string
  rejectLabel?: string
}>(), {
  label: 'Confirm',
  icon: 'pi pi-check',
  header: 'Confirm action',
  severity: 'primary',
  disabled: false,
  loading: false,
  acceptLabel: 'Confirm',
  rejectLabel: 'Cancel',
})

const emit = defineEmits<{ confirm: []; cancel: [] }>()
const confirm = useConfirm()

function open(): void {
  confirm.require({
    message: props.message,
    header: props.header,
    icon: props.icon,
    acceptLabel: props.acceptLabel,
    rejectLabel: props.rejectLabel,
    accept: () => emit('confirm'),
    reject: () => emit('cancel'),
  })
}
</script>

<template>
  <ConfirmDialog />
  <Button :label="props.label" :icon="props.icon" :severity="props.severity" :disabled="props.disabled" :loading="props.loading" @click="open" />
</template>
