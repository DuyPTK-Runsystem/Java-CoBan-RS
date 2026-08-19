<script setup lang="ts">
import { reactive } from 'vue'
import Button from 'primevue/button'
import DatePicker from 'primevue/datepicker'
import InputText from 'primevue/inputtext'

import type { StudentSearchValues } from '@/types/student'

withDefaults(defineProps<{ loading?: boolean }>(), { loading: false })
const emit = defineEmits<{ search: [values: StudentSearchValues] }>()

const values = reactive<StudentSearchValues>({
  studentCode: '',
  studentName: '',
  dateOfBirth: null,
})

function submit(): void {
  emit('search', { ...values })
}
</script>

<template>
  <form class="search-grid" @submit.prevent="submit">
    <div class="field-group">
      <label for="search-student-code">Student code</label>
      <InputText id="search-student-code" v-model="values.studentCode" maxlength="10" placeholder="Example: STU1234567" />
    </div>
    <div class="field-group">
      <label for="search-student-name">Student name</label>
      <InputText id="search-student-name" v-model="values.studentName" maxlength="35" placeholder="Example: John Doe" />
    </div>
    <div class="field-group">
      <label for="search-date-of-birth">Birthday</label>
      <DatePicker id="search-date-of-birth" v-model="values.dateOfBirth" date-format="dd-mm-yy" placeholder="dd-mm-yyyy" show-icon fluid />
    </div>
    <Button class="search-action" type="submit" label="Search" icon="pi pi-search" :loading="loading" />
  </form>
</template>
