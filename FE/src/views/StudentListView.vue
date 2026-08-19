<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
import ConfirmDialog from 'primevue/confirmdialog'
import Button from 'primevue/button'
import { useRouter } from 'vue-router'
import AuthenticatedLayout from '@/components/AuthenticatedLayout.vue'
import StudentSearchForm from '@/components/StudentSearchForm.vue'
import StudentTable from '@/components/StudentTable.vue'
import { clearAuthSession, getAuthSession } from '@/services/authSession'
import { deleteStudent, downloadStudentsCsv, fetchStudents } from '@/services/studentApi'
import { isApiError, logout as logoutApi } from '@/services/userApi'
import type { Student, StudentQuery, StudentSearchValues } from '@/types/student'

const router = useRouter(); const confirm = useConfirm(); const loading = ref(false); const downloading = ref(false); const students = ref<Student[]>([]); const totalRecords = ref(0); const totalPages = ref(0); const errorMessage = ref(''); const statusMessage = ref('')
const query = ref<StudentQuery>({ page: 0, pageSize: 10, sortField: 'studentCode', sortOrder: 1, search: { studentCode: '', studentName: '', dateOfBirth: null } })
function token(): string | null { const session = getAuthSession(); if (session) return session.accessToken; clearAuthSession(); void router.replace('/login'); return null }
async function handleUnauthorized(error: unknown): Promise<boolean> { if (!isApiError(error, 401)) return false; clearAuthSession(); await router.replace('/login'); return true }
async function load(): Promise<void> { const accessToken = token(); if (!accessToken) return; loading.value = true; errorMessage.value = ''; try { const response = await fetchStudents(accessToken, query.value); students.value = response.content; totalRecords.value = response.totalElements; totalPages.value = response.totalPages; query.value.page = response.page } catch (error) { if (!await handleUnauthorized(error)) errorMessage.value = error instanceof Error ? error.message : 'Unable to load students.' } finally { loading.value = false } }
function search(values: StudentSearchValues): void { query.value = { ...query.value, page: 0, search: values }; void load() }
function page(value: number, pageSize: number): void { query.value = { ...query.value, page: value, pageSize }; void load() }
function sort(field: keyof Student, order: 1 | -1): void { query.value = { ...query.value, page: 0, sortField: field, sortOrder: order }; void load() }
function confirmDelete(student: Student): void { confirm.require({ message: `Delete ${student.studentName}?`, header: 'Confirm deletion', icon: 'pi pi-exclamation-triangle', accept: () => { void remove(student) } }) }
async function remove(student: Student): Promise<void> { const accessToken = token(); if (!accessToken) return; try { await deleteStudent(accessToken, student.studentId); if (students.value.length === 1 && query.value.page > 0) query.value.page -= 1; await load(); statusMessage.value = 'Student deleted.' } catch (error) { errorMessage.value = error instanceof Error ? error.message : 'Unable to delete student.' } }
async function download(): Promise<void> { const accessToken = token(); if (!accessToken) return; downloading.value = true; errorMessage.value = ''; try { const csv = await downloadStudentsCsv(accessToken); const downloadUrl = URL.createObjectURL(csv); const link = document.createElement('a'); link.href = downloadUrl; link.download = 'students.csv'; document.body.append(link); link.click(); link.remove(); URL.revokeObjectURL(downloadUrl) } catch (error) { if (!await handleUnauthorized(error)) errorMessage.value = error instanceof Error ? error.message : 'Unable to download students.' } finally { downloading.value = false } }
function logout(): void { const session = getAuthSession(); if (!session) { clearAuthSession(); void router.replace('/login'); return }; void logoutApi(session.accessToken).catch(() => undefined).finally(() => { clearAuthSession(); return router.replace('/login') }) }
onMounted(() => { void load() })
</script>
<template><AuthenticatedLayout :user-name="getAuthSession()?.user.username ?? ''" @logout="logout"><ConfirmDialog /><div class="page-heading"><div><p class="eyebrow">Student workspace</p><h1>Students</h1><p>Search, review and maintain student records.</p></div><div class="page-heading-actions"><Button label="Download all students" icon="pi pi-download" :loading="downloading" :disabled="downloading" @click="download" /><Button label="Add student" icon="pi pi-plus" @click="router.push('/students/new')" /></div></div><div v-if="statusMessage" class="form-alert form-alert-info" role="status">{{ statusMessage }}</div><div v-if="errorMessage" class="form-alert form-alert-error" role="alert">{{ errorMessage }}</div><section class="content-surface"><StudentSearchForm :loading="loading" @search="search" /></section><section class="content-surface"><StudentTable :students="students" :loading="loading" :total-records="totalRecords" :total-pages="totalPages" :page="query.page" :rows-per-page="query.pageSize" :sort-field="query.sortField" :sort-order="query.sortOrder" @page-change="page" @sort-change="sort" @edit="router.push(`/students/${$event.studentId}/edit`)" @delete="confirmDelete" /></section></AuthenticatedLayout></template>
