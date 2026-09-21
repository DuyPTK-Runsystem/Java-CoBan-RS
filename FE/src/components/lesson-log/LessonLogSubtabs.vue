<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const props = withDefaults(defineProps<{ showPolicy?: boolean }>(), {
  showPolicy: false,
})

const route = useRoute()
const isPolicyRoute = computed(() => route.path === '/v2/lesson-logs/settings')
</script>

<template>
  <nav v-if="props.showPolicy" class="lesson-log-subtabs" aria-label="Chức năng sổ đầu bài">
    <RouterLink
      to="/v2/lesson-logs"
      class="lesson-log-subtab"
      :class="{ 'is-active': !isPolicyRoute }"
    >
      <i class="pi pi-book" aria-hidden="true" />
      Sổ đầu bài
    </RouterLink>
    <RouterLink
      to="/v2/lesson-logs/settings"
      class="lesson-log-subtab"
      :class="{ 'is-active': isPolicyRoute }"
    >
      <i class="pi pi-sliders-h" aria-hidden="true" />
      Chính sách sổ đầu bài
    </RouterLink>
  </nav>
</template>

<style scoped>
.lesson-log-subtabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 4px;
  border-radius: 10px;
  background: #f1f2f4;
}

.lesson-log-subtab {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 14px;
  border-radius: 8px;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
}

.lesson-log-subtab:hover {
  background: #fff;
  color: #1f2937;
}

.lesson-log-subtab.is-active {
  background: #fff;
  color: #111827;
  box-shadow: 0 1px 2px rgb(15 23 42 / 10%);
}
</style>
