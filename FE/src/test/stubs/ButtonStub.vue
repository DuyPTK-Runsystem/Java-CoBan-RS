<script setup lang="ts">
withDefaults(defineProps<{
  label?: string
  loading?: boolean
}>(), {
  label: '',
  loading: false,
})

defineEmits<{ click: [] }>()
</script>

<template>
  <button :disabled="loading" @click="$emit('click')">
    {{ label }}
  </button>
</template>
