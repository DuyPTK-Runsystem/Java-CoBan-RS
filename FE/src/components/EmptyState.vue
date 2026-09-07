<script setup lang="ts">
import Button from 'primevue/button'

withDefaults(defineProps<{
  icon?: string
  heading?: string
  message?: string
  actionLabel?: string
  actionIcon?: string
}>(), {
  icon: 'pi pi-inbox',
  heading: 'Nothing to show',
  message: 'There are no records available yet.',
  actionLabel: '',
  actionIcon: 'pi pi-refresh',
})

const emit = defineEmits<{ action: [] }>()
</script>

<template>
  <div class="empty-state" role="status">
    <i :class="icon" aria-hidden="true" />
    <strong>{{ heading }}</strong>
    <p>{{ message }}</p>
    <Button v-if="actionLabel" :label="actionLabel" :icon="actionIcon" severity="secondary" @click="emit('action')" />
  </div>
</template>
