<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'

const props = withDefaults(defineProps<{ showPolicy?: boolean }>(), {
  showPolicy: false,
})

const route = useRoute()
const router = useRouter()
const isPolicyRoute = computed(() => route.path.startsWith('/v2/lesson-logs/settings'))
</script>

<template>
  <div v-if="props.showPolicy" class="tab-strip lesson-log-tab-strip" role="tablist" aria-label="Chức năng sổ đầu bài">
    <Button
      label="Sổ đầu bài theo lớp"
      icon="pi pi-book"
      :severity="!isPolicyRoute ? 'primary' : 'secondary'"
      :outlined="isPolicyRoute"
      @click="router.push('/v2/lesson-logs')"
    />
    <Button
      label="Chính sách sổ đầu bài"
      icon="pi pi-sliders-h"
      :severity="isPolicyRoute ? 'primary' : 'secondary'"
      :outlined="!isPolicyRoute"
      @click="router.push('/v2/lesson-logs/settings')"
    />
  </div>
</template>
