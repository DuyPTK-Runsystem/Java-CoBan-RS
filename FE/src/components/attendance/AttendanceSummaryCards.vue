<script setup lang="ts">
export interface AttendanceSummaryMetric {
  key: string
  label: string
  value: string | number
  tone?: 'default' | 'success' | 'warning' | 'danger'
}

const props = withDefaults(defineProps<{ metrics?: AttendanceSummaryMetric[] }>(), { metrics: () => [] })
</script>

<template>
  <div class="attendance-summary-grid">
    <div v-for="metric in props.metrics" :key="metric.key" :class="['summary-metric', `summary-metric-${metric.tone ?? 'default'}`]">
      <span>{{ metric.label }}</span>
      <strong>{{ metric.value }}</strong>
    </div>
  </div>
</template>
