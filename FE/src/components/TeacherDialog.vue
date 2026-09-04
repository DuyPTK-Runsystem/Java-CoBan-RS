<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import FormAlert from '@/components/FormAlert.vue'
import type { Teacher, TeacherFormValues, TeacherStatus } from '@/types/teacher'
const props = withDefaults(defineProps<{ visible?: boolean; mode?: 'create' | 'edit'; initialValue?: Teacher | null; saving?: boolean; errorMessage?: string }>(), { visible: false, mode: 'create', initialValue: null, saving: false, errorMessage: '' })
const emit = defineEmits<{ 'update:visible': [value: boolean]; save: [values: TeacherFormValues]; cancel: [] }>()
const values = reactive<TeacherFormValues>({ userId: null, teacherCode: '', teacherName: '', dateOfBirth: '', gender: '', phone: '', email: '', department: '', joinDate: '', status: 'ACTIVE' })
const errors = reactive<Record<string, string>>({})
const statusOptions: Array<{ label: string; value: TeacherStatus }> = [{ label: 'Đang công tác', value: 'ACTIVE' }, { label: 'Nghỉ phép', value: 'ON_LEAVE' }, { label: 'Ngừng công tác', value: 'INACTIVE' }]
const heading = computed(() => props.mode === 'edit' ? 'Cập nhật hồ sơ giáo viên' : 'Thêm giáo viên')
watch(() => [props.visible, props.initialValue, props.mode], () => { const v = props.initialValue; Object.assign(values, { userId: v?.userId ?? null, teacherCode: v?.teacherCode ?? '', teacherName: v?.teacherName ?? '', dateOfBirth: v?.dateOfBirth ?? '', gender: v?.gender ?? '', phone: v?.phone ?? '', email: v?.email ?? '', department: v?.department ?? '', joinDate: v?.joinDate ?? '', status: v?.status ?? 'ACTIVE' }); Object.keys(errors).forEach((key) => delete errors[key]) }, { immediate: true, deep: true })
function validate(): boolean { Object.keys(errors).forEach((key) => delete errors[key]); if (!values.teacherCode.trim()) errors.teacherCode = 'Mã giáo viên là bắt buộc.'; else if (values.teacherCode.trim().length > 50) errors.teacherCode = 'Mã giáo viên tối đa 50 ký tự.'; if (!values.teacherName.trim()) errors.teacherName = 'Họ tên là bắt buộc.'; else if (values.teacherName.trim().length > 150) errors.teacherName = 'Họ tên tối đa 150 ký tự.'; if (values.gender.length > 20) errors.gender = 'Giới tính tối đa 20 ký tự.'; if (values.phone.length > 30) errors.phone = 'Số điện thoại tối đa 30 ký tự.'; if (values.email && (!/^\S+@\S+\.\S+$/.test(values.email) || values.email.length > 150)) errors.email = 'Email không hợp lệ.'; if (values.department.length > 100) errors.department = 'Tổ chuyên môn tối đa 100 ký tự.'; if (values.userId !== null && (!Number.isInteger(values.userId) || values.userId < 1)) errors.userId = 'User ID phải là số nguyên dương.'; return Object.keys(errors).length === 0 }
function save(): void { if (validate()) emit('save', { ...values, teacherCode: values.teacherCode.trim(), teacherName: values.teacherName.trim(), gender: values.gender.trim(), phone: values.phone.trim(), email: values.email.trim(), department: values.department.trim() }) }
function close(): void { emit('update:visible', false); emit('cancel') }
</script>
<template>
  <Dialog :visible="props.visible" modal :header="heading" :style="{ width: 'min(100% - 2rem, 720px)' }" :closable="!props.saving" @update:visible="emit('update:visible', $event)">
    <FormAlert v-if="props.errorMessage" tone="error" :message="props.errorMessage" /><form class="form-stack" novalidate @submit.prevent="save">
      <div class="catalog-form-grid">
        <div class="field-group"><label for="teacher-code">Mã giáo viên *</label><InputText id="teacher-code" v-model="values.teacherCode" maxlength="50" :invalid="Boolean(errors.teacherCode)" fluid /><small v-if="errors.teacherCode" class="field-error">{{ errors.teacherCode }}</small></div>
        <div class="field-group"><label for="teacher-name">Họ và tên *</label><InputText id="teacher-name" v-model="values.teacherName" maxlength="150" :invalid="Boolean(errors.teacherName)" fluid /><small v-if="errors.teacherName" class="field-error">{{ errors.teacherName }}</small></div>
        <div class="field-group"><label for="teacher-gender">Giới tính</label><InputText id="teacher-gender" v-model="values.gender" maxlength="20" :invalid="Boolean(errors.gender)" fluid /></div>
        <div class="field-group"><label for="teacher-dob">Ngày sinh</label><InputText id="teacher-dob" v-model="values.dateOfBirth" type="date" fluid /></div>
        <div class="field-group"><label for="teacher-phone">Số điện thoại</label><InputText id="teacher-phone" v-model="values.phone" maxlength="30" :invalid="Boolean(errors.phone)" fluid /></div>
        <div class="field-group"><label for="teacher-email">Email</label><InputText id="teacher-email" v-model="values.email" type="email" maxlength="150" :invalid="Boolean(errors.email)" fluid /><small v-if="errors.email" class="field-error">{{ errors.email }}</small></div>
        <div class="field-group"><label for="teacher-department">Tổ chuyên môn</label><InputText id="teacher-department" v-model="values.department" maxlength="100" fluid /></div>
        <div class="field-group"><label for="teacher-join-date">Ngày vào trường</label><InputText id="teacher-join-date" v-model="values.joinDate" type="date" fluid /></div>
        <div class="field-group"><label for="teacher-status">Trạng thái *</label><Select id="teacher-status" v-model="values.status" :options="statusOptions" option-label="label" option-value="value" fluid /></div>
        <div class="field-group"><label for="teacher-user-id">User ID liên kết</label><InputNumber id="teacher-user-id" v-model="values.userId" :min="1" :use-grouping="false" :invalid="Boolean(errors.userId)" fluid /><small v-if="errors.userId" class="field-error">{{ errors.userId }}</small><small class="field-hint">Tùy chọn, để trống nếu chưa liên kết.</small></div>
      </div><div class="form-actions"><Button type="button" label="Hủy" icon="pi pi-times" severity="secondary" outlined :disabled="props.saving" @click="close" /><Button type="submit" :label="props.mode === 'edit' ? 'Lưu thay đổi' : 'Tạo giáo viên'" icon="pi pi-check" :loading="props.saving" /></div>
    </form>
  </Dialog>
</template>
