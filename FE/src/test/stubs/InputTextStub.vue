<script setup lang="ts">
defineProps<{ modelValue?: string | number | boolean | null }>()
const emit = defineEmits<{ 'update:modelValue': [value: string | boolean] }>()

function updateValue(event: Event): void {
  const target = event.target as HTMLInputElement
  emit('update:modelValue', target.type === 'checkbox' ? target.checked : target.value)
}
</script>

<template>
  <input :value="typeof modelValue === 'boolean' ? undefined : (modelValue ?? '')" :checked="typeof modelValue === 'boolean' ? modelValue : undefined" @input="updateValue" @change="updateValue">
</template>
