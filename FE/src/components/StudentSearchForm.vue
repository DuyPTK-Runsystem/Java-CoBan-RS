<script setup lang="ts">
import { reactive } from 'vue'
import Button from 'primevue/button'
import DatePicker from 'primevue/datepicker'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'

import type { StudentAcademicStatus, StudentSearchValues } from '@/types/student'

withDefaults(defineProps<{ loading?: boolean }>(), { loading: false })
const emit = defineEmits<{ search: [values: StudentSearchValues] }>()

const statusOptions = [
  { label: 'Tất cả trạng thái', value: '' },
  { label: 'Đang học (ACTIVE)', value: 'ACTIVE' },
  { label: 'Ngừng học (INACTIVE)', value: 'INACTIVE' },
  { label: 'Tốt nghiệp (GRADUATED)', value: 'GRADUATED' },
]

const values = reactive<{
  studentCode: string
  studentName: string
  dateOfBirth: Date | null
  status: StudentAcademicStatus | ''
  classId: number | null
}>({
  studentCode: '',
  studentName: '',
  dateOfBirth: null,
  status: '',
  classId: null,
})

function submit(): void {
  const result: StudentSearchValues = {
    studentCode: values.studentCode,
    studentName: values.studentName,
    dateOfBirth: values.dateOfBirth,
    classId: values.classId,
  }
  if (values.status) {
    result.status = values.status
  }
  emit('search', result)
}

function reset(): void {
  values.studentCode = ''
  values.studentName = ''
  values.dateOfBirth = null
  values.status = ''
  values.classId = null
  submit()
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
    <div class="field-group">
      <label for="search-status">Trạng thái</label>
      <Select
        id="search-status"
        v-model="values.status"
        :options="statusOptions"
        option-label="label"
        option-value="value"
        placeholder="Chọn trạng thái"
        fluid
      />
    </div>
    <div class="field-group">
      <label for="search-class-id">Lớp hiện tại</label>
      <InputNumber
        id="search-class-id"
        v-model="values.classId"
        :min="1"
        :use-grouping="false"
        placeholder="Nhập mã lớp"
        fluid
      />
    </div>
    <div class="search-actions">
      <Button class="search-action" type="submit" label="Search" icon="pi pi-search" :loading="loading" />
      <Button type="button" label="Reset" icon="pi pi-filter-slash" severity="secondary" outlined :disabled="loading" @click="reset" />
    </div>
  </form>
</template>

<style scoped>
.search-actions {
  display: flex;
  align-items: flex-end;
  gap: 0.5rem;
}
</style>
