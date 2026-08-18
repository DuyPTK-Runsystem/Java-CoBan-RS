<script setup lang="ts">
defineProps<{ modelValue?: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

function updateValue(event: Event): void {
  emit('update:modelValue', (event.target as HTMLInputElement).value)
}
</script>

<template>
  <input :value="modelValue ?? ''" @input="updateValue">
</template>
