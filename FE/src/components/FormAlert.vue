<script setup lang="ts">
import { computed } from 'vue'

import type { ValidationError } from '@/types/api'

const props = withDefaults(defineProps<{
  tone?: 'error' | 'warning' | 'info' | 'success'
  message?: string
  messages?: string[]
  validationErrors?: ValidationError[]
}>(), {
  tone: 'error',
  message: '',
  messages: () => [],
  validationErrors: () => [],
})

const role = computed(() => props.tone === 'error' || props.tone === 'warning' ? 'alert' : 'status')
const validationMessages = computed(() => props.validationErrors.flatMap((error) => error.messages.map((message) => ({ field: error.field, message }))))
</script>

<template>
  <div :class="['form-alert', `form-alert-${props.tone}`]" :role="role" aria-live="polite">
    <p v-if="props.message" class="form-alert-message">{{ props.message }}</p>
    <ul v-if="props.messages.length || validationMessages.length" class="form-alert-list">
      <li v-for="(item, index) in props.messages" :key="`message-${index}`">{{ item }}</li>
      <li v-for="item in validationMessages" :key="`${item.field}-${item.message}`"><strong>{{ item.field }}:</strong> {{ item.message }}</li>
    </ul>
  </div>
</template>
