<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'

import StudentForm from '@/components/StudentForm.vue'
import { clearAuthSession, getAuthSession } from '@/services/authSession'
import {
  createStudent,
  createStudentV3,
  generateStudentCode,
  getStudent,
  updateStudent,
} from '@/services/studentApi'
import { studentUiMessage } from '@/utils/studentUiMessage'
import { isApiError } from '@/types/api'
import type { Student, StudentFormValues, StudentV2Payload, StudentV3CreateRequest } from '@/types/student'

const route = useRoute()
const router = useRouter()

const saving = ref(false)
const generating = ref(false)
const errorMessage = ref('')

const isEdit = computed(() => Boolean(route.params.studentId))
const studentId = computed(() => {
  const id = Number(route.params.studentId)
  return Number.isFinite(id) && id > 0 ? id : null
})

const initialValue = ref<Partial<StudentFormValues>>({
  studentCode: '',
  studentName: '',
  dateOfBirth: null,
  address: '',
  status: 'ACTIVE',
  provisionAccount: true,
  username: '',
  password: '',
})

const session = computed(() => getAuthSession())
const userRoles = computed(() => session.value?.user.roles ?? [])
const canProvisionAccount = computed(() => {
  return userRoles.value.includes('ADMIN') || userRoles.value.includes('ACADEMIC_OFFICE')
})
const createdAccountUsername = ref<string | null>(null)

function token(): string | null {
  const sess = getAuthSession()
  if (sess) return sess.accessToken
  clearAuthSession()
  void router.replace('/login')
  return null
}

function formatDate(value: Date | null): string | null {
  if (!value) return null
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

async function load(): Promise<void> {
  const accessToken = token()
  const id = studentId.value
  if (!accessToken || !id) return

  try {
    const student: Student = await getStudent(accessToken, id)
    initialValue.value = {
      studentId: student.studentId,
      studentCode: student.studentCode,
      studentName: student.studentName,
      dateOfBirth: student.dateOfBirth ? new Date(`${student.dateOfBirth}T00:00:00`) : null,
      address: student.address ?? '',
      status: student.status ?? 'ACTIVE',
    }
  } catch (error) {
    if (isApiError(error, 401)) return
    errorMessage.value = studentUiMessage(error, 'Không thể tải thông tin học sinh.')
  }
}

async function generate(): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  generating.value = true
  errorMessage.value = ''
  try {
    const res = await generateStudentCode(accessToken)
    initialValue.value = {
      ...initialValue.value,
      studentCode: res.studentCode,
    }
  } catch (error) {
    if (isApiError(error, 401)) return
    errorMessage.value = studentUiMessage(error, 'Không thể tự động sinh mã học sinh.')
  } finally {
    generating.value = false
  }
}

async function save(values: StudentFormValues): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  saving.value = true
  errorMessage.value = ''

  try {
    if (isEdit.value && studentId.value) {
      const updatePayload: StudentV2Payload = {
        studentCode: values.studentCode.trim(),
        studentName: values.studentName.trim(),
        dateOfBirth: formatDate(values.dateOfBirth),
        address: values.address.trim() || null,
      }
      await updateStudent(accessToken, studentId.value, updatePayload)
    } else {
      if (values.provisionAccount) {
        const v3Payload: StudentV3CreateRequest = {
          studentCode: values.studentCode.trim(),
          studentName: values.studentName.trim(),
          dateOfBirth: formatDate(values.dateOfBirth),
          address: values.address.trim() || null,
          username: values.username?.trim() || null,
          password: values.password || null,
        }
        const created = await createStudentV3(accessToken, v3Payload)
        createdAccountUsername.value = created.account.username
      } else {
        const v2Payload: StudentV2Payload = {
          studentCode: values.studentCode.trim(),
          studentName: values.studentName.trim(),
          dateOfBirth: formatDate(values.dateOfBirth),
          address: values.address.trim() || null,
        }
        await createStudent(accessToken, v2Payload)
      }
    }
    if (!createdAccountUsername.value) {
      await router.push('/v2/students')
    }
  } catch (error: unknown) {
    if (isApiError(error, 401)) return
    if (isApiError(error, 409)) {
      errorMessage.value = error.message || 'Mã học sinh hoặc tên đăng nhập đã tồn tại trên hệ thống.'
    } else {
      errorMessage.value = studentUiMessage(error, 'Không thể lưu hồ sơ học sinh.')
    }
  } finally {
    saving.value = false
  }
}

function handleBack(): void {
  void router.push('/v2/students')
}

function finishAccountProvisioning(): void {
  createdAccountUsername.value = null
  void router.push('/v2/students')
}

onMounted(() => {
  if (isEdit.value) {
    void load()
  }
})
</script>

<template>
  <div class="student-form-view">
    <div class="page-heading">
      <div>
        <h1>{{ isEdit ? 'Chỉnh sửa hồ sơ học sinh' : 'Thêm mới học sinh' }}</h1>
      </div>
    </div>

    <section class="content-surface form-surface">
      <StudentForm
        :mode="isEdit ? 'edit' : 'add'"
        :initial-value="initialValue"
        :saving="saving"
        :generating="generating"
        :error-message="errorMessage"
        :can-provision-account="canProvisionAccount"
        @save="save"
        @generate-code="generate"
        @back="handleBack"
      />
    </section>

    <Dialog
      :visible="Boolean(createdAccountUsername)"
      modal
      header="Đã cấp tài khoản học sinh"
      :style="{ width: 'min(100% - 2rem, 28rem)' }"
      :closable="false"
    >
      <p>Tài khoản đăng nhập của học sinh đã được tạo thành công.</p>
      <p><strong>Username: {{ createdAccountUsername }}</strong></p>
      <template #footer>
        <Button label="Hoàn tất" icon="pi pi-check" @click="finishAccountProvisioning" />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.student-form-view {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.form-surface {
  max-width: 48rem;
  padding: 1.5rem;
}
</style>
