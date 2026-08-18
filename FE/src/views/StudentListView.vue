<script setup lang="ts">
import { computed, ref } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
import ConfirmDialog from 'primevue/confirmdialog'
import Button from 'primevue/button'
import { useRouter } from 'vue-router'

import AuthenticatedLayout from '@/components/AuthenticatedLayout.vue'
import StudentSearchForm from '@/components/StudentSearchForm.vue'
import StudentTable from '@/components/StudentTable.vue'
import type { Student, StudentSearchValues } from '@/types/student'

const router = useRouter()
const confirm = useConfirm()
const loading = ref(false)
const page = ref(0)
const sortField = ref<keyof Student>('studentCode')
const sortOrder = ref<1 | -1>(1)
const query = ref<StudentSearchValues>({ studentCode: '', studentName: '', dateOfBirth: null })
const statusMessage = ref('')

const students = ref<Student[]>([
  {
    studentId: 1,
    studentCode: 'STU100001',
    studentName: 'Nguyen An',
    dateOfBirth: '2002-04-18',
    address: 'District 1',
    averageScore: 8.4,
  },
  {
    studentId: 2,
    studentCode: 'STU100002',
    studentName: 'Tran Minh',
    dateOfBirth: '2001-11-09',
    address: 'District 3',
    averageScore: 7.8,
  },
])

const filteredStudents = computed(() => {
  const searchCode = query.value.studentCode.trim().toLowerCase()
  const searchName = query.value.studentName.trim().toLowerCase()
  return students.value.filter((student) =>
    (!searchCode || student.studentCode.toLowerCase().includes(searchCode))
    && (!searchName || student.studentName.toLowerCase().includes(searchName)),
  )
})

function search(values: StudentSearchValues): void {
  query.value = values
  page.value = 0
  statusMessage.value = 'Search state updated locally. Server-side filtering will be connected later.'
}

function confirmDelete(student: Student): void {
  confirm.require({
    message: `Delete ${student.studentName}?`,
    header: 'Confirm deletion',
    icon: 'pi pi-exclamation-triangle',
    rejectLabel: 'Cancel',
    acceptLabel: 'Delete',
    acceptClass: 'p-button-danger',
    accept: () => {
      students.value = students.value.filter((item) => item.studentId !== student.studentId)
      statusMessage.value = 'The demo row was removed. The delete API will be connected later.'
    },
  })
}

function logout(): void {
  router.push('/login')
}
</script>

<template>
  <AuthenticatedLayout @logout="logout">
    <ConfirmDialog />
    <div class="page-heading">
      <div>
        <p class="eyebrow">Student workspace</p>
        <h1>Students</h1>
        <p>Search, review and maintain student records.</p>
      </div>
      <Button label="Add student" icon="pi pi-plus" @click="router.push('/students/new')" />
    </div>
    <div v-if="statusMessage" class="form-alert form-alert-info" role="status">{{ statusMessage }}</div>
    <section class="content-surface" aria-labelledby="student-search-title">
      <div class="section-heading">
        <h2 id="student-search-title">Find a student</h2>
        <span class="section-caption">Page size: 10</span>
      </div>
      <StudentSearchForm :loading="loading" @search="search" />
    </section>
    <section class="content-surface" aria-labelledby="student-table-title">
      <div class="section-heading">
        <div>
          <h2 id="student-table-title">Student records</h2>
          <span class="section-caption">{{ filteredStudents.length }} demo records</span>
        </div>
      </div>
      <StudentTable
        :students="filteredStudents"
        :loading="loading"
        :total-records="filteredStudents.length"
        :page="page"
        :sort-field="sortField"
        :sort-order="sortOrder"
        @page-change="page = $event"
        @sort-change="(field, order) => { sortField = field; sortOrder = order }"
        @edit="router.push(`/students/${$event.studentId}/edit`)"
        @delete="confirmDelete"
      />
    </section>
  </AuthenticatedLayout>
</template>
