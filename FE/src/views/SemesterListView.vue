<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
import Button from 'primevue/button'
import ConfirmDialog from 'primevue/confirmdialog'
import Dialog from 'primevue/dialog'
import Textarea from 'primevue/textarea'
import { useRoute, useRouter } from 'vue-router'

import FormAlert from '@/components/FormAlert.vue'
import PageState from '@/components/PageState.vue'
import SemesterDialog from '@/components/SemesterDialog.vue'
import SemesterStatusDialog from '@/components/SemesterStatusDialog.vue'
import SemesterTable from '@/components/SemesterTable.vue'
import { clearAuthSession, getAuthSession } from '@/services/authSession'
import { activateSemester, createSemester, dispatchSemesterNotifications, fetchAcademicYears, fetchSemesters, fetchSemesterNotifications, getSemesterCompletenessReport, lockSemester, reopenSemester, retryFailedSemesterNotifications, updateSemester } from '@/services/academicApi'
import { isApiError } from '@/types/api'
import type { AcademicYear, Semester, SemesterCompletenessReport, SemesterFormValues, SemesterNotification } from '@/types/academic'
import type { LoadingState } from '@/types/ui'
import { formatAcademicDate } from '@/utils/academicDate'

const route = useRoute()
const router = useRouter()
const confirm = useConfirm()
const academicYear = ref<AcademicYear | null>(null)
const semesters = ref<Semester[]>([])
const loadingState = ref<LoadingState>('loading')
const loading = computed(() => loadingState.value === 'loading')
const forbidden = ref(false)
const errorMessage = ref('')
const statusMessage = ref('')
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const selectedSemester = ref<Semester | null>(null)
const saving = ref(false)
const dialogErrorMessage = ref('')
const statusDialogVisible = ref(false)
const report = ref<SemesterCompletenessReport | null>(null)
const reportLoading = ref(false)
const statusError = ref('')
const actionLoading = ref(false)
const reopenDialogVisible = ref(false)
const reopenReason = ref('')
const reopenError = ref('')
const notifications = ref<SemesterNotification[]>([])
const notificationsLoading = ref(false)
const notificationActionLoading = ref(false)
const notificationError = ref('')

const pageState = computed<LoadingState>(() => loadingState.value === 'success' && semesters.value.length === 0 ? 'empty' : loadingState.value)
const statusDialogError = computed(() => statusError.value)

function accessToken(): string | null {
  const session = getAuthSession()
  if (session) return session.accessToken
  clearAuthSession()
  void router.replace({ name: 'login' })
  return null
}

function messageFor(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

function academicYearId(): number | null {
  const id = Number(route.params.academicYearId)
  return Number.isInteger(id) && id > 0 ? id : null
}

async function load(): Promise<void> {
  const token = accessToken()
  const id = academicYearId()
  if (!token || !id) {
    loadingState.value = 'error'
    errorMessage.value = 'Mã năm học trên đường dẫn không hợp lệ.'
    return
  }
  loadingState.value = 'loading'
  forbidden.value = false
  errorMessage.value = ''
  try {
    const academicYears = await fetchAcademicYears(token)
    academicYear.value = academicYears.find((item) => item.id === id) ?? null
    if (!academicYear.value) {
      loadingState.value = 'error'
      errorMessage.value = 'Không tìm thấy năm học được yêu cầu.'
      return
    }
    semesters.value = await fetchSemesters(token, id)
    loadingState.value = 'success'
  } catch (error) {
    if (isApiError(error, 401)) return
    forbidden.value = isApiError(error, 403)
    errorMessage.value = messageFor(error, 'Không thể tải danh sách học kỳ.')
    loadingState.value = 'error'
  }
}

function openCreate(): void {
  selectedSemester.value = null
  dialogMode.value = 'create'
  dialogErrorMessage.value = ''
  dialogVisible.value = true
}

function openEdit(semester: Semester): void {
  selectedSemester.value = semester
  dialogMode.value = 'edit'
  dialogErrorMessage.value = ''
  dialogVisible.value = true
}

function closeDialog(): void {
  dialogVisible.value = false
  dialogErrorMessage.value = ''
}

async function saveSemester(values: SemesterFormValues): Promise<void> {
  const token = accessToken()
  const id = academicYearId()
  if (!token || !id) return
  if (values.displayOrder === null) {
    dialogErrorMessage.value = 'Thứ tự học kỳ là bắt buộc.'
    return
  }
  saving.value = true
  dialogErrorMessage.value = ''
  const metadata = {
    code: values.code.trim(),
    name: values.name.trim(),
    displayOrder: values.displayOrder,
    startDate: values.startDate,
    endDate: values.endDate,
    automaticLockAt: values.automaticLockAt.trim() || null,
  }
  try {
    if (dialogMode.value === 'edit' && selectedSemester.value) {
      await updateSemester(token, selectedSemester.value.id, metadata)
      statusMessage.value = 'Đã cập nhật học kỳ.'
    } else {
      await createSemester(token, { academicYearId: id, ...metadata })
      statusMessage.value = 'Đã tạo học kỳ.'
    }
    await load()
    closeDialog()
  } catch (error) {
    if (isApiError(error, 401)) return
    dialogErrorMessage.value = messageFor(error, 'Không thể lưu học kỳ.')
  } finally {
    saving.value = false
  }
}

function confirmActivate(semester: Semester): void {
  confirm.require({
    message: `Kích hoạt học kỳ ${semester.name}? Sau đó học kỳ có thể nhận dữ liệu nghiệp vụ.`,
    header: 'Kích hoạt học kỳ',
    icon: 'pi pi-play',
    acceptLabel: 'Kích hoạt',
    rejectLabel: 'Hủy',
    accept: () => { void activate(semester) },
  })
}

async function activate(semester: Semester): Promise<void> {
  const token = accessToken()
  if (!token) return
  actionLoading.value = true
  errorMessage.value = ''
  try {
    await activateSemester(token, semester.id)
    statusMessage.value = 'Đã kích hoạt học kỳ.'
    await load()
  } catch (error) {
    if (isApiError(error, 401)) return
    errorMessage.value = messageFor(error, 'Không thể kích hoạt học kỳ.')
  } finally {
    actionLoading.value = false
  }
}

function openStatus(semester: Semester): void {
  selectedSemester.value = semester
  report.value = null
  statusError.value = ''
  notifications.value = []
  notificationError.value = ''
  statusDialogVisible.value = true
  void Promise.all([loadReport(semester), loadNotifications(semester)])
}

async function loadReport(semester: Semester): Promise<void> {
  const token = accessToken()
  if (!token) return
  reportLoading.value = true
  try {
    const result = await getSemesterCompletenessReport(token, semester.id)
    if (selectedSemester.value?.id === semester.id) report.value = result
  } catch (error) {
    if (isApiError(error, 401)) return
    if (selectedSemester.value?.id === semester.id) statusError.value = messageFor(error, 'Không thể tải báo cáo hoàn thành dữ liệu.')
  } finally {
    reportLoading.value = false
  }
}

async function loadNotifications(semester: Semester): Promise<void> {
  const token = accessToken()
  if (!token) return
  notificationsLoading.value = true
  try {
    const result = await fetchSemesterNotifications(token, semester.id)
    if (selectedSemester.value?.id === semester.id) notifications.value = result
  } catch (error) {
    if (isApiError(error, 401)) return
    if (selectedSemester.value?.id === semester.id) notificationError.value = messageFor(error, 'Không thể tải lịch sử gửi email.')
  } finally {
    notificationsLoading.value = false
  }
}

function confirmDispatchNotifications(): void {
  const semester = selectedSemester.value
  if (!semester) return
  confirm.require({
    message: `Gửi email nhắc điểm cho học kỳ ${semester.name}?`,
    header: 'Gửi email nhắc điểm',
    icon: 'pi pi-send',
    acceptLabel: 'Gửi email',
    rejectLabel: 'Hủy',
    accept: () => { void dispatchNotifications(semester) },
  })
}

async function dispatchNotifications(semester: Semester): Promise<void> {
  const token = accessToken()
  if (!token) return
  notificationActionLoading.value = true
  notificationError.value = ''
  try {
    await dispatchSemesterNotifications(token, semester.id)
    await loadNotifications(semester)
    statusMessage.value = 'Đã hoàn tất yêu cầu gửi email.'
  } catch (error) {
    if (isApiError(error, 401)) return
    notificationError.value = messageFor(error, 'Không thể gửi email nhắc điểm.')
  } finally {
    notificationActionLoading.value = false
  }
}

function confirmRetryNotifications(): void {
  const semester = selectedSemester.value
  if (!semester) return
  confirm.require({
    message: `Thử gửi lại các email bị lỗi của học kỳ ${semester.name}?`,
    header: 'Thử gửi lại email',
    icon: 'pi pi-refresh',
    acceptLabel: 'Thử gửi lại',
    rejectLabel: 'Hủy',
    accept: () => { void retryNotifications(semester) },
  })
}

async function retryNotifications(semester: Semester): Promise<void> {
  const token = accessToken()
  if (!token) return
  notificationActionLoading.value = true
  notificationError.value = ''
  try {
    await retryFailedSemesterNotifications(token, semester.id)
    await loadNotifications(semester)
    statusMessage.value = 'Đã hoàn tất yêu cầu thử gửi lại email.'
  } catch (error) {
    if (isApiError(error, 401)) return
    notificationError.value = messageFor(error, 'Không thể thử gửi lại email.')
  } finally {
    notificationActionLoading.value = false
  }
}

function confirmLock(): void {
  const semester = selectedSemester.value
  if (!semester) return
  confirm.require({
    message: `Bạn có chắc muốn khóa học kỳ ${semester.name}? Dữ liệu chưa hoàn chỉnh chỉ là cảnh báo và không chặn thao tác.`,
    header: 'Khóa học kỳ',
    icon: 'pi pi-lock',
    acceptLabel: 'Khóa học kỳ',
    rejectLabel: 'Hủy',
    accept: () => { void lock(semester) },
  })
}

async function lock(semester: Semester): Promise<void> {
  const token = accessToken()
  if (!token) return
  actionLoading.value = true
  statusError.value = ''
  try {
    await lockSemester(token, semester.id)
    statusMessage.value = 'Đã khóa học kỳ.'
    statusDialogVisible.value = false
    await load()
  } catch (error) {
    if (isApiError(error, 401)) return
    statusError.value = messageFor(error, 'Không thể khóa học kỳ.')
  } finally {
    actionLoading.value = false
  }
}

function openReopen(): void {
  reopenReason.value = ''
  reopenError.value = ''
  reopenDialogVisible.value = true
}

async function submitReopen(): Promise<void> {
  const semester = selectedSemester.value
  const token = accessToken()
  if (!semester || !token) return
  const reason = reopenReason.value.trim()
  if (!reason) {
    reopenError.value = 'Lý do mở lại là bắt buộc.'
    return
  }
  actionLoading.value = true
  reopenError.value = ''
  try {
    await reopenSemester(token, semester.id, { reason })
    statusMessage.value = 'Đã mở lại học kỳ.'
    reopenDialogVisible.value = false
    statusDialogVisible.value = false
    await load()
  } catch (error) {
    if (isApiError(error, 401)) return
    reopenError.value = messageFor(error, 'Không thể mở lại học kỳ.')
  } finally {
    actionLoading.value = false
  }
}

watch(() => route.params.academicYearId, () => { void load() }, { immediate: true })
</script>

<template>
  <ConfirmDialog />
  <div class="page-heading">
    <div>
      <p class="eyebrow">Academic structure</p>
      <h1>Học kỳ</h1>
      <p v-if="academicYear">Năm học {{ academicYear.code }} · {{ formatAcademicDate(academicYear.startDate) }} → {{ formatAcademicDate(academicYear.endDate) }}</p>
      <p v-else>Đang tải thông tin năm học...</p>
    </div>
    <div class="page-heading-actions">
      <Button label="Quay lại năm học" icon="pi pi-arrow-left" severity="secondary" outlined @click="router.push({ name: 'v2-academic-years' })" />
      <Button label="Tạo học kỳ" icon="pi pi-plus" :disabled="!academicYear" @click="openCreate" />
    </div>
  </div>
  <FormAlert v-if="statusMessage" tone="success" :message="statusMessage" />
  <FormAlert v-if="errorMessage && !forbidden" tone="error" :message="errorMessage" />
  <section class="content-surface">
    <PageState
      :state="pageState"
      :forbidden="forbidden"
      forbidden-message="Bạn không có quyền xem danh sách học kỳ."
      :error-message="errorMessage"
      empty-heading="Chưa có học kỳ"
      empty-message="Năm học này chưa có học kỳ nào."
      @retry="load"
    >
      <SemesterTable :semesters="semesters" :loading="loading" @edit="openEdit" @activate="confirmActivate" @view-status="openStatus" @lock="openStatus" @reopen="openStatus" />
    </PageState>
  </section>
  <SemesterDialog v-model:visible="dialogVisible" :mode="dialogMode" :academic-year="academicYear" :initial-value="selectedSemester" :saving="saving" :error-message="dialogErrorMessage" @save="saveSemester" @cancel="closeDialog" />
  <SemesterStatusDialog v-model:visible="statusDialogVisible" :semester="selectedSemester" :report="report" :loading="reportLoading" :action-loading="actionLoading" :error-message="statusDialogError" :notifications="notifications" :notifications-loading="notificationsLoading" :notification-action-loading="notificationActionLoading" :notification-error-message="notificationError" @lock="confirmLock" @reopen="openReopen" @dispatch-notifications="confirmDispatchNotifications" @retry-notifications="confirmRetryNotifications" />
  <Dialog v-model:visible="reopenDialogVisible" modal header="Mở lại học kỳ" :style="{ width: 'min(100% - 2rem, 560px)' }" :closable="!actionLoading">
    <p class="dialog-caption">Hãy ghi rõ lý do mở lại học kỳ để lưu audit log.</p>
    <FormAlert v-if="reopenError" tone="error" :message="reopenError" />
    <div class="field-group">
      <label for="semester-reopen-reason">Lý do mở lại</label>
      <Textarea id="semester-reopen-reason" v-model="reopenReason" rows="4" maxlength="500" auto-resize :invalid="Boolean(reopenError)" />
    </div>
    <div class="form-actions">
      <Button type="button" label="Hủy" icon="pi pi-times" severity="secondary" outlined :disabled="actionLoading" @click="reopenDialogVisible = false" />
      <Button type="button" label="Mở lại học kỳ" icon="pi pi-lock-open" severity="info" :loading="actionLoading" @click="submitReopen" />
    </div>
  </Dialog>
</template>
