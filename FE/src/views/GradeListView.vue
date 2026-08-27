<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
import Button from 'primevue/button'
import ConfirmDialog from 'primevue/confirmdialog'
import { useRouter } from 'vue-router'

import GradeDialog from '@/components/GradeDialog.vue'
import GradeTable from '@/components/GradeTable.vue'
import FormAlert from '@/components/FormAlert.vue'
import PageState from '@/components/PageState.vue'
import { clearAuthSession, getAuthSession } from '@/services/authSession'
import { createGrade, deleteGrade, fetchGrades, updateGrade } from '@/services/academicApi'
import { isApiError } from '@/types/api'
import type { GradeLevel, GradeLevelFormValues, GradeLevelRequest } from '@/types/academic'
import type { LoadingState } from '@/types/ui'

const router = useRouter()
const confirm = useConfirm()
const grades = ref<GradeLevel[]>([])
const loadingState = ref<LoadingState>('loading')
const forbidden = ref(false)
const errorMessage = ref('')
const statusMessage = ref('')
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const selectedGrade = ref<GradeLevel | null>(null)
const saving = ref(false)
const dialogErrorMessage = ref('')

const pageState = computed<LoadingState>(() => loadingState.value === 'success' && grades.value.length === 0 ? 'empty' : loadingState.value)

function token(): string | null {
  const session = getAuthSession()
  if (session) return session.accessToken
  clearAuthSession()
  void router.replace({ name: 'login' })
  return null
}

function messageFor(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

async function load(): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  loadingState.value = 'loading'
  forbidden.value = false
  errorMessage.value = ''
  try {
    grades.value = await fetchGrades(accessToken)
    loadingState.value = 'success'
  } catch (error) {
    if (isApiError(error, 401)) return
    forbidden.value = isApiError(error, 403)
    errorMessage.value = messageFor(error, 'Không thể tải danh sách khối.')
    loadingState.value = 'error'
  }
}

function openCreate(): void {
  selectedGrade.value = null
  dialogMode.value = 'create'
  dialogErrorMessage.value = ''
  dialogVisible.value = true
}

function openEdit(grade: GradeLevel): void {
  selectedGrade.value = grade
  dialogMode.value = 'edit'
  dialogErrorMessage.value = ''
  dialogVisible.value = true
}

function closeDialog(): void {
  dialogVisible.value = false
  dialogErrorMessage.value = ''
}

function requestFrom(values: GradeLevelFormValues, active = values.active): GradeLevelRequest | null {
  if (values.gradeLevel === null || values.displayOrder === null) return null
  return {
    code: values.code.trim(),
    name: values.name.trim(),
    gradeLevel: values.gradeLevel,
    displayOrder: values.displayOrder,
    nextGradeId: values.nextGradeId,
    active,
    description: values.description.trim() || null,
  }
}

async function save(values: GradeLevelFormValues): Promise<void> {
  const accessToken = token()
  const request = requestFrom(values)
  if (!accessToken || !request) {
    dialogErrorMessage.value = 'Cấp và dữ liệu khối là bắt buộc.'
    return
  }
  saving.value = true
  dialogErrorMessage.value = ''
  try {
    if (dialogMode.value === 'edit' && selectedGrade.value) {
      await updateGrade(accessToken, selectedGrade.value.id, request)
      statusMessage.value = 'Đã cập nhật khối.'
    } else {
      await createGrade(accessToken, request)
      statusMessage.value = 'Đã tạo khối.'
    }
    await load()
    closeDialog()
  } catch (error) {
    if (isApiError(error, 401)) return
    dialogErrorMessage.value = messageFor(error, 'Không thể lưu khối.')
  } finally {
    saving.value = false
  }
}

function confirmToggle(grade: GradeLevel): void {
  const action = grade.active ? 'ngừng dùng' : 'kích hoạt'
  confirm.require({
    message: `Bạn có chắc muốn ${action} ${grade.name}? Lịch sử học tập sẽ được giữ nguyên.`,
    header: grade.active ? 'Ngừng dùng khối' : 'Kích hoạt khối',
    icon: grade.active ? 'pi pi-ban' : 'pi pi-check',
    acceptLabel: grade.active ? 'Ngừng dùng' : 'Kích hoạt',
    rejectLabel: 'Hủy',
    accept: () => { void toggleActive(grade) },
  })
}

async function toggleActive(grade: GradeLevel): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  errorMessage.value = ''
  try {
    await updateGrade(accessToken, grade.id, {
      code: grade.code,
      name: grade.name,
      gradeLevel: grade.gradeLevel,
      displayOrder: grade.displayOrder,
      nextGradeId: grade.nextGradeId,
      active: !grade.active,
      description: grade.description,
    })
    statusMessage.value = grade.active ? 'Đã ngừng dùng khối.' : 'Đã kích hoạt khối.'
    await load()
  } catch (error) {
    if (isApiError(error, 401)) return
    errorMessage.value = messageFor(error, 'Không thể thay đổi trạng thái khối.')
  }
}

function confirmDelete(grade: GradeLevel): void {
  confirm.require({
    message: `Bạn có chắc muốn xóa ${grade.name}? Chỉ khối chưa được tham chiếu mới có thể xóa.`,
    header: 'Xóa khối',
    icon: 'pi pi-trash',
    acceptLabel: 'Xóa khối',
    rejectLabel: 'Hủy',
    accept: () => { void remove(grade) },
  })
}

async function remove(grade: GradeLevel): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  errorMessage.value = ''
  try {
    await deleteGrade(accessToken, grade.id)
    statusMessage.value = 'Đã xóa khối.'
    await load()
  } catch (error) {
    if (isApiError(error, 401)) return
    errorMessage.value = messageFor(error, 'Không thể xóa khối.')
  }
}

onMounted(() => { void load() })
</script>

<template>
  <ConfirmDialog />
  <div class="page-heading">
    <div>
      <p class="eyebrow">Academic structure</p>
      <h1>Khối</h1>
      <p>Quản lý metadata khối và chuỗi khối kế tiếp.</p>
    </div>
    <div class="page-heading-actions">
      <Button label="Danh sách lớp" icon="pi pi-building" severity="secondary" outlined @click="router.push({ name: 'v2-academic-classes' })" />
      <Button label="Tạo khối" icon="pi pi-plus" @click="openCreate" />
    </div>
  </div>
  <FormAlert v-if="statusMessage" tone="success" :message="statusMessage" />
  <FormAlert v-if="errorMessage && !forbidden" tone="error" :message="errorMessage" />
  <section class="content-surface">
    <PageState
      :state="pageState"
      :forbidden="forbidden"
      forbidden-message="Bạn không có quyền xem danh sách khối."
      :error-message="errorMessage"
      empty-heading="Chưa có khối"
      empty-message="Hãy tạo metadata khối đầu tiên để bắt đầu quản lý lớp."
      @retry="load"
    >
      <GradeTable :grades="grades" @edit="openEdit" @toggle-active="confirmToggle" @delete="confirmDelete" />
    </PageState>
  </section>
  <GradeDialog v-model:visible="dialogVisible" :mode="dialogMode" :initial-value="selectedGrade" :grades="grades" :saving="saving" :error-message="dialogErrorMessage" @save="save" @cancel="closeDialog" />
</template>
