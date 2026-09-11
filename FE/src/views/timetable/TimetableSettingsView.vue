<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import DatePicker from 'primevue/datepicker'
import Dialog from 'primevue/dialog'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'

import FormAlert from '@/components/common/FormAlert.vue'
import PageState from '@/components/common/PageState.vue'
import { useAuthSession } from '@/composables/useAuthSession'
import { fetchSemesters } from '@/services/academicApi'
import { fetchTeachers } from '@/services/teacherApi'
import {
  activateTeacherLoadPolicy,
  createTeacherLoadEligibility,
  createTeacherLoadPolicy,
  deleteTeacherLoadEligibility,
  getActiveTeacherLoadPolicy,
  listTeacherLoadEligibilities,
  listTeacherLoadPolicies,
} from '@/services/teacherLoadApi'
import { initTimetableCalendar } from '@/services/timetableApi'
import { extractApiErrorMessage } from '@/types/api'
import type { Semester } from '@/types/academic'
import type { Teacher } from '@/types/teacher'
import type { TeacherLoadEligibility, TeacherLoadPolicy } from '@/types/timetable'
import type { LoadingState } from '@/types/ui'

const router = useRouter()
const { requireAccessToken } = useAuthSession()

const activeTab = ref<'POLICY' | 'ELIGIBILITY' | 'CALENDAR'>('POLICY')
const loadingState = ref<LoadingState>('loading')
const generalError = ref('')
const successMessage = ref('')

const activePolicy = ref<TeacherLoadPolicy | null>(null)
const policies = ref<TeacherLoadPolicy[]>([])
const eligibilities = ref<TeacherLoadEligibility[]>([])
const teachers = ref<Teacher[]>([])
const semesters = ref<Semester[]>([])

// Create policy dialog
const isPolicyDialogVisible = ref(false)
const policyForm = ref({
  policyName: '',
  sourceDocument: '',
  effectiveFrom: new Date(),
  standardPeriodsHighSchool: 19,
  homeroomReduction: 4,
  nursingChildReduction: 3,
})
const policySaving = ref(false)

// Create eligibility dialog
const isEligibilityDialogVisible = ref(false)
const eligibilityForm = ref({
  teacherId: null as number | null,
  conditionType: 'NURSING_CHILD_UNDER_12M',
  validFrom: new Date(),
  validTo: new Date(),
  evidenceInfo: '',
})
const eligibilitySaving = ref(false)

// Calendar init
const selectedSemesterId = ref<number | null>(null)
const calendarInitializing = ref(false)

async function loadData() {
  const token = requireAccessToken()
  if (!token) return
  loadingState.value = 'loading'
  generalError.value = ''
  try {
    const [actPol, polList, eligList, teacherList, semList] = await Promise.all([
      getActiveTeacherLoadPolicy(token).catch(() => null),
      listTeacherLoadPolicies(token),
      listTeacherLoadEligibilities(undefined, token),
      fetchTeachers(token),
      fetchSemesters(token),
    ])
    activePolicy.value = actPol
    policies.value = polList
    eligibilities.value = eligList
    teachers.value = teacherList
    semesters.value = semList
    if (semList.length > 0) selectedSemesterId.value = semList[0].id
    loadingState.value = 'idle'
  } catch (err) {
    loadingState.value = 'error'
    generalError.value = extractApiErrorMessage(err, 'Không thể tải cấu hình thời khóa biểu')
  }
}

async function handleActivatePolicy(policyId: number) {
  const token = requireAccessToken()
  if (!token) return
  try {
    await activateTeacherLoadPolicy(policyId, token)
    successMessage.value = 'Đã kích hoạt chính sách định mức thành công'
    await loadData()
  } catch (err) {
    generalError.value = extractApiErrorMessage(err, 'Không thể kích hoạt chính sách')
  }
}

function formatDateStr(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

async function handleSavePolicy() {
  const token = requireAccessToken()
  if (!token) return
  policySaving.value = true
  try {
    await createTeacherLoadPolicy(
      {
        policyName: policyForm.value.policyName,
        sourceDocument: policyForm.value.sourceDocument,
        effectiveFrom: formatDateStr(policyForm.value.effectiveFrom),
        standardPeriodsHighSchool: policyForm.value.standardPeriodsHighSchool,
        homeroomReduction: policyForm.value.homeroomReduction,
        nursingChildReduction: policyForm.value.nursingChildReduction,
      },
      token,
    )
    isPolicyDialogVisible.value = false
    successMessage.value = 'Đã tạo chính sách mới'
    await loadData()
  } catch (err) {
    generalError.value = extractApiErrorMessage(err, 'Không thể tạo chính sách')
  } finally {
    policySaving.value = false
  }
}

async function handleSaveEligibility() {
  if (!eligibilityForm.value.teacherId) return
  const token = requireAccessToken()
  if (!token) return
  eligibilitySaving.value = true
  try {
    await createTeacherLoadEligibility(
      {
        teacherId: eligibilityForm.value.teacherId,
        conditionType: eligibilityForm.value.conditionType,
        validFrom: formatDateStr(eligibilityForm.value.validFrom),
        validTo: formatDateStr(eligibilityForm.value.validTo),
        evidenceInfo: eligibilityForm.value.evidenceInfo,
      },
      token,
    )
    isEligibilityDialogVisible.value = false
    successMessage.value = 'Đã thêm điều kiện miễn giảm cho giáo viên'
    await loadData()
  } catch (err) {
    generalError.value = extractApiErrorMessage(err, 'Không thể thêm điều kiện miễn giảm')
  } finally {
    eligibilitySaving.value = false
  }
}

async function handleDeleteEligibility(id: number) {
  const token = requireAccessToken()
  if (!token) return
  try {
    await deleteTeacherLoadEligibility(id, token)
    successMessage.value = 'Đã xóa điều kiện miễn giảm'
    await loadData()
  } catch (err) {
    generalError.value = extractApiErrorMessage(err, 'Không thể xóa điều kiện')
  }
}

async function handleInitCalendar() {
  if (!selectedSemesterId.value) return
  const token = requireAccessToken()
  if (!token) return
  calendarInitializing.value = true
  try {
    await initTimetableCalendar(selectedSemesterId.value, token)
    successMessage.value = 'Đã khởi tạo khung giờ chuẩn 2 buổi × 4 tiết cho học kỳ'
  } catch (err) {
    generalError.value = extractApiErrorMessage(err, 'Không thể khởi tạo khung giờ')
  } finally {
    calendarInitializing.value = false
  }
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <div class="p-6 max-w-7xl mx-auto flex flex-col gap-6">
    <div class="flex items-center gap-3">
      <Button icon="pi pi-arrow-left" severity="secondary" rounded text @click="router.push('/v2/timetables')" />
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Cấu hình Thời khóa biểu & Định mức</h1>
        <p class="text-sm text-gray-500">Quản lý chính sách định mức tiết dạy, diện miễn giảm và khung giờ học</p>
      </div>
    </div>

    <FormAlert v-if="generalError" :message="generalError" type="error" />
    <FormAlert v-if="successMessage" :message="successMessage" type="success" />

    <!-- Tabs -->
    <div class="flex gap-2 border-b border-gray-200 pb-2">
      <button
        class="px-4 py-2 text-sm font-semibold rounded-lg transition"
        :class="[activeTab === 'POLICY' ? 'bg-blue-600 text-white' : 'text-gray-600 hover:bg-gray-100']"
        @click="activeTab = 'POLICY'"
      >
        Chính sách định mức
      </button>
      <button
        class="px-4 py-2 text-sm font-semibold rounded-lg transition"
        :class="[activeTab === 'ELIGIBILITY' ? 'bg-blue-600 text-white' : 'text-gray-600 hover:bg-gray-100']"
        @click="activeTab = 'ELIGIBILITY'"
      >
        Xác nhận miễn giảm
      </button>
      <button
        class="px-4 py-2 text-sm font-semibold rounded-lg transition"
        :class="[activeTab === 'CALENDAR' ? 'bg-blue-600 text-white' : 'text-gray-600 hover:bg-gray-100']"
        @click="activeTab = 'CALENDAR'"
      >
        Khung giờ học kỳ
      </button>
    </div>

    <PageState
      v-if="loadingState === 'loading' || loadingState === 'error'"
      :state="loadingState"
      :message="generalError"
      @retry="loadData"
    />

    <div v-else>
      <!-- TAB 1: POLICIES -->
      <div v-if="activeTab === 'POLICY'" class="flex flex-col gap-6">
        <div class="flex justify-between items-center">
          <h3 class="text-lg font-bold text-gray-900">Danh sách chính sách định mức</h3>
          <Button label="Thêm chính sách mới" icon="pi pi-plus" @click="isPolicyDialogVisible = true" />
        </div>

        <DataTable :value="policies" responsive-layout="scroll">
          <Column field="policyName" header="Tên chính sách" />
          <Column field="sourceDocument" header="Văn bản căn cứ" />
          <Column header="Áp dụng từ" style="width: 140px">
            <template #body="{ data }">{{ data.effectiveFrom }}</template>
          </Column>
          <Column field="standardPeriodsHighSchool" header="Tiết chuẩn" style="width: 100px" class="text-center" />
          <Column header="Chủ nhiệm" style="width: 110px" class="text-center">
            <template #body="{ data }">-{{ data.homeroomReduction }} tiết</template>
          </Column>
          <Column header="Nuôi con nhỏ" style="width: 120px" class="text-center">
            <template #body="{ data }">-{{ data.nursingChildReduction }} tiết</template>
          </Column>
          <Column header="Trạng thái" style="width: 140px">
            <template #body="{ data }">
              <Tag :value="data.active ? 'Đang kích hoạt' : 'Không kích hoạt'" :severity="data.active ? 'success' : 'secondary'" />
            </template>
          </Column>
          <Column header="Thao tác" style="width: 140px" class="text-right">
            <template #body="{ data }">
              <Button
                v-if="!data.active"
                label="Kích hoạt"
                size="small"
                severity="primary"
                text
                @click="handleActivatePolicy(data.id)"
              />
            </template>
          </Column>
        </DataTable>
      </div>

      <!-- TAB 2: ELIGIBILITIES -->
      <div v-if="activeTab === 'ELIGIBILITY'" class="flex flex-col gap-6">
        <div class="flex justify-between items-center">
          <div>
            <h3 class="text-lg font-bold text-gray-900">Danh sách giáo viên được miễn giảm tiết</h3>
            <p class="text-xs text-gray-500">Các điều kiện được giảm trừ tiết dạy ngoài kiêm nhiệm chủ nhiệm (như nuôi con &lt; 12 tháng)</p>
          </div>
          <Button label="Thêm diện miễn giảm" icon="pi pi-plus" @click="isEligibilityDialogVisible = true" />
        </div>

        <DataTable :value="eligibilities" responsive-layout="scroll">
          <Column field="teacherName" header="Giáo viên" />
          <Column header="Diện miễn giảm">
            <template #body="{ data }">
              {{ data.conditionType === 'NURSING_CHILD_UNDER_12M' ? 'Nuôi con nhỏ dưới 12 tháng' : data.conditionType }}
            </template>
          </Column>
          <Column header="Khoảng hiệu lực" style="width: 220px">
            <template #body="{ data }">{{ data.validFrom }} → {{ data.validTo }}</template>
          </Column>
          <Column field="evidenceInfo" header="Minh chứng / Hồ sơ" />
          <Column header="Thao tác" style="width: 100px" class="text-right">
            <template #body="{ data }">
              <Button
                icon="pi pi-trash"
                severity="danger"
                text
                rounded
                @click="handleDeleteEligibility(data.id)"
              />
            </template>
          </Column>
        </DataTable>
      </div>

      <!-- TAB 3: CALENDAR -->
      <div v-if="activeTab === 'CALENDAR'" class="bg-white p-6 rounded-xl border border-gray-200 flex flex-col gap-4 max-w-lg">
        <h3 class="text-lg font-bold text-gray-900">Khởi tạo khung giờ chuẩn</h3>
        <p class="text-xs text-gray-600 leading-relaxed">
          Cấu hình tự động khởi tạo 48 tiết học chuẩn (Thứ 2 đến Thứ 7, mỗi ngày 2 buổi Sáng & Chiều, mỗi buổi 4 tiết) theo quy định của nhà trường.
        </p>

        <div class="flex flex-col gap-1">
          <label class="font-medium text-sm">Chọn học kỳ áp dụng</label>
          <Select
            v-model="selectedSemesterId"
            :options="semesters"
            option-label="name"
            option-value="id"
            placeholder="Chọn học kỳ"
          />
        </div>

        <Button
          label="Khởi tạo khung giờ 2 buổi × 4 tiết"
          icon="pi pi-calendar-plus"
          :loading="calendarInitializing"
          @click="handleInitCalendar"
        />
      </div>
    </div>

    <!-- Create Policy Dialog -->
    <Dialog v-model:visible="isPolicyDialogVisible" header="Thêm chính sách định mức mới" modal :style="{ width: '480px' }">
      <div class="flex flex-col gap-4">
        <div class="flex flex-col gap-1">
          <label class="font-medium text-sm">Tên chính sách</label>
          <InputText v-model="policyForm.policyName" placeholder="Ví dụ: Quy định định mức tiết dạy 2026" />
        </div>
        <div class="flex flex-col gap-1">
          <label class="font-medium text-sm">Văn bản căn cứ</label>
          <InputText v-model="policyForm.sourceDocument" placeholder="Ví dụ: Thông tư 28/2009/TT-BGDĐT" />
        </div>
        <div class="flex flex-col gap-1">
          <label class="font-medium text-sm">Ngày bắt đầu áp dụng</label>
          <DatePicker v-model="policyForm.effectiveFrom" date-format="yy-mm-dd" show-icon />
        </div>
        <div class="grid grid-cols-3 gap-2">
          <div class="flex flex-col gap-1">
            <label class="font-medium text-xs">Tiết chuẩn THPT</label>
            <InputNumber v-model="policyForm.standardPeriodsHighSchool" :min="1" />
          </div>
          <div class="flex flex-col gap-1">
            <label class="font-medium text-xs">Giảm chủ nhiệm</label>
            <InputNumber v-model="policyForm.homeroomReduction" :min="0" />
          </div>
          <div class="flex flex-col gap-1">
            <label class="font-medium text-xs">Giảm nuôi con nhỏ</label>
            <InputNumber v-model="policyForm.nursingChildReduction" :min="0" />
          </div>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-2 mt-4">
          <Button label="Hủy" severity="secondary" text @click="isPolicyDialogVisible = false" />
          <Button label="Tạo chính sách" :loading="policySaving" @click="handleSavePolicy" />
        </div>
      </template>
    </Dialog>

    <!-- Create Eligibility Dialog -->
    <Dialog v-model:visible="isEligibilityDialogVisible" header="Thêm diện miễn giảm giáo viên" modal :style="{ width: '480px' }">
      <div class="flex flex-col gap-4">
        <div class="flex flex-col gap-1">
          <label class="font-medium text-sm">Giáo viên</label>
          <Select
            v-model="eligibilityForm.teacherId"
            :options="teachers"
            option-label="teacherName"
            option-value="id"
            placeholder="Chọn giáo viên"
          />
        </div>
        <div class="flex flex-col gap-1">
          <label class="font-medium text-sm">Điều kiện miễn giảm</label>
          <Select
            v-model="eligibilityForm.conditionType"
            :options="[{ label: 'Nuôi con nhỏ dưới 12 tháng (giảm 3 tiết)', value: 'NURSING_CHILD_UNDER_12M' }]"
            option-label="label"
            option-value="value"
          />
        </div>
        <div class="grid grid-cols-2 gap-2">
          <div class="flex flex-col gap-1">
            <label class="font-medium text-xs">Từ ngày</label>
            <DatePicker v-model="eligibilityForm.validFrom" date-format="yy-mm-dd" show-icon />
          </div>
          <div class="flex flex-col gap-1">
            <label class="font-medium text-xs">Đến ngày</label>
            <DatePicker v-model="eligibilityForm.validTo" date-format="yy-mm-dd" show-icon />
          </div>
        </div>
        <div class="flex flex-col gap-1">
          <label class="font-medium text-sm">Minh chứng / Ghi chú</label>
          <InputText v-model="eligibilityForm.evidenceInfo" placeholder="Ví dụ: Giấy khai sinh số 123..." />
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-2 mt-4">
          <Button label="Hủy" severity="secondary" text @click="isEligibilityDialogVisible = false" />
          <Button label="Lưu" :loading="eligibilitySaving" @click="handleSaveEligibility" />
        </div>
      </template>
    </Dialog>
  </div>
</template>

