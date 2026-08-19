<script setup lang="ts">
withDefaults(defineProps<{
  label?: string
  loading?: boolean
  disabled?: boolean
}>(), {
  label: '',
  loading: false,
  disabled: false,
})

defineEmits<{ click: [] }>()
</script>

<template>
  <button :disabled="loading || disabled" @click="$emit('click')">
    {{ label }}
  </button>
</template>
