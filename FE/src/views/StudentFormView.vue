<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AuthenticatedLayout from '@/components/AuthenticatedLayout.vue'
import StudentForm from '@/components/StudentForm.vue'
import { clearAuthSession, getAuthSession } from '@/services/authSession'
import { logout as logoutApi } from '@/services/userApi'
import type { StudentFormValues } from '@/types/student'

const route = useRoute()
const router = useRouter()
const saving = ref(false)
const statusMessage = ref('')
const isEdit = computed(() => Boolean(route.params.studentId))

const initialValue: StudentFormValues = {
  studentId: isEdit.value ? Number(route.params.studentId) : undefined,
  studentCode: isEdit.value ? 'STU100001' : '',
  studentName: isEdit.value ? 'Nguyen An' : '',
  dateOfBirth: isEdit.value ? new Date('2002-04-18') : null,
  address: isEdit.value ? 'District 1' : '',
  averageScore: isEdit.value ? 8.4 : null,
}

function save(_values: StudentFormValues): void {
  statusMessage.value = 'Save API integration is ready for a later implementation plan.'
}

function logout(): void {
  const session = getAuthSession()
  if (!session) {
    clearAuthSession()
    void router.replace('/login')
    return
  }

  void logoutApi(session.accessToken)
    .catch(() => undefined)
    .finally(() => {
      clearAuthSession()
      return router.replace('/login')
    })
}
</script>

<template>
  <AuthenticatedLayout :user-name="getAuthSession()?.user.username ?? ''" @logout="logout">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Student workspace</p>
        <h1>{{ isEdit ? 'Update student' : 'Add student' }}</h1>
        <p>{{ isEdit ? 'Review the record and update its editable details.' : 'Create a student record for the workspace.' }}</p>
      </div>
    </div>
    <section class="content-surface form-surface" aria-labelledby="student-form-title">
      <div class="section-heading">
        <h2 id="student-form-title">{{ isEdit ? 'Student details' : 'New student details' }}</h2>
      </div>
      <div v-if="statusMessage" class="form-alert form-alert-info" role="status">{{ statusMessage }}</div>
      <StudentForm :mode="isEdit ? 'edit' : 'add'" :initial-value="initialValue" :saving="saving" @save="save" @back="router.push('/students')" />
    </section>
  </AuthenticatedLayout>
</template>
