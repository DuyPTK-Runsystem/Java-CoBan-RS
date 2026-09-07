<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import Button from 'primevue/button'
import ConfirmDialog from 'primevue/confirmdialog'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import { useConfirm } from 'primevue/useconfirm'
import { useRouter } from 'vue-router'
import TeacherTable from '@/components/TeacherTable.vue'
import TeacherDialog from '@/components/TeacherDialog.vue'
import TeacherDetailDialog from '@/components/TeacherDetailDialog.vue'
import FormAlert from '@/components/FormAlert.vue'
import PageState from '@/components/PageState.vue'
import { clearAuthSession, getAuthSession } from '@/services/authSession'
import { createTeacher, deleteTeacher, fetchTeachers, updateTeacher } from '@/services/teacherApi'
import { isApiError } from '@/types/api'
import type { Teacher, TeacherFormValues, TeacherStatus } from '@/types/teacher'
import type { LoadingState } from '@/types/ui'
const router = useRouter(); const confirm = useConfirm(); const teachers = ref<Teacher[]>([]); const state = ref<LoadingState>('loading'); const errorMessage = ref(''); const forbidden = ref(false); const statusMessage = ref(''); const search = ref(''); const status = ref<'ALL' | TeacherStatus>('ALL'); const dialogVisible = ref(false); const detailVisible = ref(false); const dialogMode = ref<'create' | 'edit'>('create'); const selected = ref<Teacher | null>(null); const saving = ref(false); const dialogError = ref('')
const statusOptions = [{ label: 'Tất cả trạng thái', value: 'ALL' }, { label: 'Đang công tác', value: 'ACTIVE' }, { label: 'Nghỉ phép', value: 'ON_LEAVE' }, { label: 'Ngừng công tác', value: 'INACTIVE' }] as const
const filteredTeachers = computed(() => { const q = search.value.trim().toLocaleLowerCase(); return teachers.value.filter((t) => (status.value === 'ALL' || t.status === status.value) && (!q || [t.teacherCode, t.teacherName, t.email, t.phone, t.department].some((v) => v?.toLocaleLowerCase().includes(q)))) })
const pageState = computed<LoadingState>(() => state.value === 'success' && filteredTeachers.value.length === 0 ? 'empty' : state.value)
function token(): string | null { const session = getAuthSession(); if (session) return session.accessToken; clearAuthSession(); void router.replace({ name: 'login' }); return null }
function message(error: unknown, fallback: string): string { return error instanceof Error && error.message ? error.message : fallback }
async function load(): Promise<void> { const t = token(); if (!t) return; state.value = 'loading'; errorMessage.value = ''; forbidden.value = false; try { teachers.value = await fetchTeachers(t, status.value === 'ALL' ? undefined : status.value); state.value = 'success' } catch (error) { if (isApiError(error, 401)) return; forbidden.value = isApiError(error, 403); errorMessage.value = message(error, 'Không thể tải danh sách giáo viên.'); state.value = 'error' } }
function openCreate(): void { selected.value = null; dialogMode.value = 'create'; dialogError.value = ''; dialogVisible.value = true }
function openEdit(teacher: Teacher): void { selected.value = teacher; dialogMode.value = 'edit'; dialogError.value = ''; dialogVisible.value = true }
function openDetail(teacher: Teacher): void { selected.value = teacher; detailVisible.value = true }
function remove(teacher: Teacher): void { confirm.require({ header: 'Xác nhận xóa giáo viên', message: `Xóa hồ sơ ${teacher.teacherCode} - ${teacher.teacherName}? Dữ liệu phân công/điểm phát sinh sẽ khiến backend từ chối thao tác.`, acceptLabel: 'Xóa', rejectLabel: 'Hủy', accept: () => void performDelete(teacher) }) }
async function performDelete(teacher: Teacher): Promise<void> { const t = token(); if (!t) return; try { await deleteTeacher(t, teacher.id); statusMessage.value = 'Đã xóa hồ sơ giáo viên.'; await load() } catch (error) { if (isApiError(error, 401)) return; errorMessage.value = message(error, 'Không thể xóa giáo viên.') } }
async function save(values: TeacherFormValues): Promise<void> { const t = token(); if (!t) return; saving.value = true; dialogError.value = ''; try { const request = { ...values, userId: values.userId || null, dateOfBirth: values.dateOfBirth || null, joinDate: values.joinDate || null, gender: values.gender || null, phone: values.phone || null, email: values.email || null, department: values.department || null }; if (dialogMode.value === 'edit' && selected.value) await updateTeacher(t, selected.value.id, request); else await createTeacher(t, request); statusMessage.value = dialogMode.value === 'edit' ? 'Đã cập nhật hồ sơ giáo viên.' : 'Đã tạo hồ sơ giáo viên.'; dialogVisible.value = false; await load() } catch (error) { if (isApiError(error, 401)) return; dialogError.value = message(error, 'Không thể lưu hồ sơ giáo viên.') } finally { saving.value = false } }
onMounted(() => { void load() })
</script>
<template><div class="page-content"><ConfirmDialog /><header class="page-heading"><div><p class="eyebrow">Quản lý nhân sự</p><h1>Hồ sơ giáo viên</h1><p>Quản lý thông tin hồ sơ và liên kết tài khoản giáo viên.</p></div><div class="page-heading-actions"><Button label="Thêm giáo viên" icon="pi pi-plus" @click="openCreate" /></div></header><FormAlert v-if="statusMessage" tone="success" :message="statusMessage" /><section class="content-surface"><div class="search-grid"><div class="field-group"><label for="teacher-status-filter">Trạng thái</label><Select id="teacher-status-filter" v-model="status" :options="statusOptions" option-label="label" option-value="value" fluid @update:model-value="load" /></div><div class="field-group" style="grid-column: span 2"><label for="teacher-search">Tìm kiếm</label><InputText id="teacher-search" v-model="search" placeholder="Mã GV, họ tên, email, SĐT, tổ chuyên môn..." fluid /></div></div></section><PageState :state="pageState" :error-message="errorMessage" :forbidden="forbidden" empty-heading="Chưa có giáo viên" empty-message="Không có hồ sơ phù hợp với bộ lọc hiện tại." @retry="load"><TeacherTable :teachers="filteredTeachers" @view="openDetail" @edit="openEdit" @delete="remove" /></PageState><TeacherDialog v-model:visible="dialogVisible" :mode="dialogMode" :initial-value="selected" :saving="saving" :error-message="dialogError" @save="save" /><TeacherDetailDialog v-model:visible="detailVisible" :teacher="selected" @schedule="(teacher) => { detailVisible = false; void router.push({ name: 'v2-teaching-assignments', query: { teacherId: String(teacher.id) } }) }" /></div></template>
