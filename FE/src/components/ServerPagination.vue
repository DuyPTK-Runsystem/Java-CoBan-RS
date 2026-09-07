<script setup lang="ts">
import Paginator, { type PageState as PrimePageState } from 'primevue/paginator'

const props = withDefaults(defineProps<{
  page?: number
  pageSize: number
  totalRecords: number
  pageSizeOptions?: number[]
}>(), {
  page: 0,
  pageSizeOptions: () => [10, 20, 50],
})

const emit = defineEmits<{ pageChange: [page: number, pageSize: number] }>()

function handlePage(event: PrimePageState): void {
  emit('pageChange', event.page, event.rows)
}
</script>

<template>
  <Paginator
    :first="props.page * props.pageSize"
    :rows="props.pageSize"
    :total-records="props.totalRecords"
    :rows-per-page-options="props.pageSizeOptions"
    @page="handlePage"
  />
</template>
