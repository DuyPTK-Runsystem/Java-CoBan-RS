<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthenticatedLayout from '@/components/AuthenticatedLayout.vue'
import StudentForm from '@/components/StudentForm.vue'
import { clearAuthSession, getAuthSession } from '@/services/authSession'
import { createStudent, generateStudentCode, getStudent, updateStudent } from '@/services/studentApi'
import { logout as logoutApi } from '@/services/userApi'
import type { Student, StudentFormValues } from '@/types/student'
const route = useRoute(); const router = useRouter(); const saving = ref(false); const generating = ref(false); const errorMessage = ref('')
const isEdit = computed(() => Boolean(route.params.studentId)); const initialValue = ref<StudentFormValues>({ studentCode: '', studentName: '', dateOfBirth: null, address: '', averageScore: null })
function token(): string | null { const session = getAuthSession(); if (session) return session.accessToken; clearAuthSession(); void router.replace('/login'); return null }
function date(value: Date | null): string | null { return value ? `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}` : null }
async function load(): Promise<void> { const accessToken = token(); const id = Number(route.params.studentId); if (!accessToken || !id) return; try { const student: Student = await getStudent(accessToken, id); initialValue.value = { ...student, dateOfBirth: student.dateOfBirth ? new Date(`${student.dateOfBirth}T00:00:00`) : null } } catch (error) { errorMessage.value = error instanceof Error ? error.message : 'Unable to load student.' } }
async function generate(): Promise<void> { const accessToken = token(); if (!accessToken) return; generating.value = true; try { initialValue.value = { ...initialValue.value, studentCode: (await generateStudentCode(accessToken)).studentCode } } catch (error) { errorMessage.value = error instanceof Error ? error.message : 'Unable to generate code.' } finally { generating.value = false } }
async function save(values: StudentFormValues): Promise<void> { const accessToken = token(); if (!accessToken) return; saving.value = true; try { const body = { studentCode: values.studentCode, studentName: values.studentName.trim(), dateOfBirth: date(values.dateOfBirth), address: values.address, averageScore: values.averageScore }; if (isEdit.value) await updateStudent(accessToken, Number(route.params.studentId), body); else await createStudent(accessToken, body); await router.push('/students') } catch (error) { errorMessage.value = error instanceof Error ? error.message : 'Unable to save student.' } finally { saving.value = false } }
function logout(): void { const session = getAuthSession(); if (!session) { clearAuthSession(); void router.replace('/login'); return }; void logoutApi(session.accessToken).catch(() => undefined).finally(() => { clearAuthSession(); return router.replace('/login') }) }
onMounted(() => { if (isEdit.value) void load() })
</script>
<template><AuthenticatedLayout :user-name="getAuthSession()?.user.username ?? ''" @logout="logout"><div class="page-heading"><h1>{{ isEdit ? 'Update student' : 'Add student' }}</h1></div><section class="content-surface form-surface"><StudentForm :mode="isEdit ? 'edit' : 'add'" :initial-value="initialValue" :saving="saving" :generating="generating" :error-message="errorMessage" @save="save" @generate-code="generate" @back="router.push('/students')" /></section></AuthenticatedLayout></template>
