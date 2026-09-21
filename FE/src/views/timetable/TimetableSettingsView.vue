<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
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
import { fetchTeachers } from '@/services/teacherApi'
import {
  activateTeacherLoadPolicy,
  createTeacherLoadEligibility,
  createTeacherLoadPolicy,
  revokeTeacherLoadEligibility,
  getActiveTeacherLoadPolicy,
  listTeacherLoadEligibilities,
  listTeacherLoadPolicies,
  updateTeacherLoadEligibility,
} from '@/services/teacherLoadApi'
import { extractApiErrorMessage } from '@/types/api'
import type { Teacher } from '@/types/teacher'
import type { TeacherLoadEligibility, TeacherLoadPolicy, TeacherLoadRule } from '@/types/timetable'
import type { LoadingState } from '@/types/ui'

const router = useRouter()
const { requireAccessToken } = useAuthSession()

const activeTab = ref<'POLICY' | 'ELIGIBILITY'>('POLICY')
const loadingState = ref<LoadingState>('loading')
const generalError = ref('')
const successMessage = ref('')

const activePolicy = ref<TeacherLoadPolicy | null>(null)
const policies = ref<TeacherLoadPolicy[]>([])
const eligibilities = ref<TeacherLoadEligibility[]>([])
const teachers = ref<Teacher[]>([])

// Create policy dialog
const isPolicyDialogVisible = ref(false)
const policyDialogMode = ref<'create' | 'clone'>('create')
const policyForm = ref({
  policyName: '',
  sourceDocument: '',
  effectiveFrom: new Date(),
  standardPeriodsHighSchool: 19,
  homeroomReduction: 4,
  nursingChildReduction: 3,
})
const ruleDrafts = ref<TeacherLoadRule[]>([])
const policySaving = ref(false)

// Create eligibility dialog
const isEligibilityDialogVisible = ref(false)
const editingEligibilityId = ref<number | null>(null)
const eligibilityForm = ref({
  teacherId: null as number | null,
  conditionType: 'NURSING_CHILD_UNDER_12M',
  validFrom: new Date(),
  validTo: new Date(),
  evidenceInfo: '',
})
const eligibilitySaving = ref(false)
const eligibilityRuleOptions = computed(() => {
  const rules = activePolicy.value?.rules.filter((rule) => rule.triggerType === 'ELIGIBILITY') ?? []
  return rules.length > 0
    ? rules.map((rule) => ({ label: `${rule.ruleName} (giảm ${rule.reductionPeriods} tiết)`, value: rule.ruleCode }))
    : [{ label: 'Nuôi con nhỏ dưới 12 tháng (giảm 3 tiết)', value: 'NURSING_CHILD_UNDER_12M' }]
})

function getEligibilityRuleLabel(ruleCode: string): string {
  const configuredRule = activePolicy.value?.rules.find((rule) => rule.ruleCode === ruleCode)
  if (configuredRule) {
    return `${configuredRule.ruleName} (giảm ${configuredRule.reductionPeriods} tiết)`
  }
  const labels: Record<string, string> = {
    NURSING_CHILD_UNDER_12M: 'Nuôi con nhỏ dưới 12 tháng',
    HOMEROOM: 'Giáo viên chủ nhiệm',
  }
  return labels[ruleCode] ?? `${ruleCode || 'Điều kiện miễn giảm khác'}`
}

function getSupplementalRules(policy: TeacherLoadPolicy): TeacherLoadRule[] {
  return policy.rules.filter((rule) => !['HOMEROOM', 'NURSING_CHILD_UNDER_12M'].includes(rule.ruleCode))
}

async function loadData(): Promise<boolean> {
  const token = requireAccessToken()
  if (!token) return false
  loadingState.value = 'loading'
  generalError.value = ''
  try {
    const [actPol, polList, eligList, teacherList] = await Promise.all([
      getActiveTeacherLoadPolicy(token).catch(() => null),
      listTeacherLoadPolicies(token),
      listTeacherLoadEligibilities(undefined, token),
      fetchTeachers(token),
    ])
    activePolicy.value = actPol
    policies.value = polList
    eligibilities.value = eligList
    teachers.value = teacherList
    loadingState.value = 'idle'
    return true
  } catch (err) {
    loadingState.value = 'error'
    generalError.value = extractApiErrorMessage(err, 'Không thể tải cấu hình thời khóa biểu')
    return false
  }
}

async function reloadAfterMutation(successText: string): Promise<void> {
  successMessage.value = successText
  const reloaded = await loadData()
  if (!reloaded) {
    generalError.value = 'Thao tác đã thành công nhưng không thể tải lại dữ liệu. Vui lòng thử tải lại.'
  }
}

async function handleActivatePolicy(policyId: number) {
  resetMessages()
  const token = requireAccessToken()
  if (!token) return
  try {
    const policy = policies.value.find((item) => item.id === policyId)
    if (!policy) return
    await activateTeacherLoadPolicy(policyId, policy.version, token)
    await reloadAfterMutation('Đã kích hoạt chính sách định mức thành công')
  } catch (err) {
    generalError.value = extractApiErrorMessage(err, 'Không thể kích hoạt chính sách')
  }
}

function openPolicyDialog(policy?: TeacherLoadPolicy) {
  policyDialogMode.value = policy ? 'clone' : 'create'
  policyForm.value = {
    policyName: policy ? `${policy.policyName} - bản mới` : '',
    sourceDocument: policy?.sourceDocument ?? '',
    effectiveFrom: policy ? new Date(`${policy.effectiveFrom}T00:00:00`) : new Date(),
    standardPeriodsHighSchool: policy?.standardPeriodsHighSchool ?? 19,
    homeroomReduction: policy?.homeroomReduction ?? 4,
    nursingChildReduction: policy?.nursingChildReduction ?? 3,
  }
  ruleDrafts.value = (policy?.rules ?? []).filter((rule) => !['HOMEROOM', 'NURSING_CHILD_UNDER_12M'].includes(rule.ruleCode))
  isPolicyDialogVisible.value = true
}

function addRuleDraft() {
  ruleDrafts.value.push({
    ruleCode: '',
    ruleName: '',
    triggerType: 'ELIGIBILITY',
    reductionPeriods: 0,
    source: policyForm.value.sourceDocument,
    active: true,
  })
}

function removeRuleDraft(index: number) {
  ruleDrafts.value.splice(index, 1)
}

function openEligibilityDialog(eligibility?: TeacherLoadEligibility) {
  editingEligibilityId.value = eligibility?.id ?? null
  eligibilityForm.value = {
    teacherId: eligibility?.teacherId ?? null,
    conditionType: eligibility?.ruleCode ?? 'NURSING_CHILD_UNDER_12M',
    validFrom: eligibility ? new Date(`${eligibility.validFrom}T00:00:00`) : new Date(),
    validTo: eligibility ? new Date(`${eligibility.validTo}T00:00:00`) : new Date(),
    evidenceInfo: eligibility?.evidenceReference ?? '',
  }
  isEligibilityDialogVisible.value = true
}

function formatDateStr(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

async function handleSavePolicy() {
  resetMessages()
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
        rules: [
          {
            ruleCode: 'HOMEROOM',
            ruleName: 'Giảm chủ nhiệm',
            triggerType: 'HOMEROOM',
            reductionPeriods: policyForm.value.homeroomReduction,
            source: policyForm.value.sourceDocument,
            active: true,
          },
          {
            ruleCode: 'NURSING_CHILD_UNDER_12M',
            ruleName: 'Nuôi con nhỏ dưới 12 tháng',
            triggerType: 'ELIGIBILITY',
            reductionPeriods: policyForm.value.nursingChildReduction,
            source: policyForm.value.sourceDocument,
            active: true,
          },
          ...ruleDrafts.value,
        ],
      },
      token,
    )
    isPolicyDialogVisible.value = false
    await reloadAfterMutation('Đã tạo chính sách mới')
  } catch (err) {
    generalError.value = extractApiErrorMessage(err, 'Không thể tạo chính sách')
  } finally {
    policySaving.value = false
  }
}

async function handleSaveEligibility() {
  resetMessages()
  if (!eligibilityForm.value.teacherId) {
    generalError.value = 'Vui lòng chọn giáo viên.'
    return
  }
  if (eligibilityForm.value.validTo < eligibilityForm.value.validFrom) {
    generalError.value = 'Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.'
    return
  }
  const token = requireAccessToken()
  if (!token) return
  eligibilitySaving.value = true
  try {
    const editingEligibility = eligibilities.value.find((item) => item.id === editingEligibilityId.value)
    if (editingEligibility) {
      await updateTeacherLoadEligibility(editingEligibility.id, {
        expectedVersion: editingEligibility.version,
        ruleCode: eligibilityForm.value.conditionType,
        validFrom: formatDateStr(eligibilityForm.value.validFrom),
        validTo: formatDateStr(eligibilityForm.value.validTo),
        evidenceReference: eligibilityForm.value.evidenceInfo,
      }, token)
    } else {
      await createTeacherLoadEligibility({
        teacherId: eligibilityForm.value.teacherId,
        ruleCode: eligibilityForm.value.conditionType,
        validFrom: formatDateStr(eligibilityForm.value.validFrom),
        validTo: formatDateStr(eligibilityForm.value.validTo),
        evidenceReference: eligibilityForm.value.evidenceInfo,
      }, token)
    }
    isEligibilityDialogVisible.value = false
    await reloadAfterMutation(editingEligibility ? 'Đã cập nhật điều kiện miễn giảm' : 'Đã thêm điều kiện miễn giảm cho giáo viên')
  } catch (err) {
    generalError.value = extractApiErrorMessage(err, 'Không thể thêm điều kiện miễn giảm')
  } finally {
    eligibilitySaving.value = false
  }
}

async function handleDeleteEligibility(id: number) {
  resetMessages()
  const token = requireAccessToken()
  if (!token) return
  try {
    const eligibility = eligibilities.value.find((item) => item.id === id)
    if (!eligibility) return
    await revokeTeacherLoadEligibility(id, eligibility.version, token)
    await reloadAfterMutation('Đã thu hồi điều kiện miễn giảm')
  } catch (err) {
    generalError.value = extractApiErrorMessage(err, 'Không thể xóa điều kiện')
  }
}

function resetMessages() {
  generalError.value = ''
  successMessage.value = ''
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <div class="timetable-page p-6 max-w-7xl mx-auto flex flex-col gap-6">
    <div class="flex items-center gap-3">
      <Button icon="pi pi-arrow-left" severity="secondary" rounded text @click="router.push('/v2/timetables')" />
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Cấu hình Thời khóa biểu & Định mức</h1>
      </div>
    </div>

    <FormAlert v-if="generalError" :message="generalError" tone="error" />
    <FormAlert v-if="successMessage" :message="successMessage" tone="success" />

    <!-- Tabs -->
    <div class="timetable-subtab-strip">
      <button
        class="timetable-subtab"
        :class="{ 'is-active': activeTab === 'POLICY' }"
        @click="activeTab = 'POLICY'"
      >
        Chính sách định mức
      </button>
      <button
        class="timetable-subtab"
        :class="{ 'is-active': activeTab === 'ELIGIBILITY' }"
        @click="activeTab = 'ELIGIBILITY'"
      >
        Xác nhận miễn giảm
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
          <Button label="Tạo phiên bản mới" icon="pi pi-plus" @click="openPolicyDialog(); resetMessages()" />
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
          <Column header="Miễn giảm bổ sung" style="min-width: 250px">
            <template #body="{ data }">
              <div v-if="getSupplementalRules(data).length" class="flex flex-col gap-1">
                <span v-for="rule in getSupplementalRules(data)" :key="rule.ruleCode">
                  {{ rule.ruleName || rule.ruleCode }}: -{{ rule.reductionPeriods }} tiết
                </span>
              </div>
              <span v-else class="text-gray-500">Không có</span>
            </template>
          </Column>
          <Column header="Trạng thái" style="width: 140px">
            <template #body="{ data }">
              <Tag :value="data.active ? 'Đang kích hoạt' : 'Không kích hoạt'" :severity="data.active ? 'success' : 'secondary'" />
            </template>
          </Column>
          <Column header="Thao tác" style="width: 140px" class="text-right">
            <template #body="{ data }">
              <Button v-if="data.active" label="Đang dùng" size="small" severity="success" text disabled />
              <Button
                v-if="!data.active"
                label="Kích hoạt phiên bản"
                size="small"
                severity="primary"
                text
                @click="handleActivatePolicy(data.id)"
              />
              <Button v-if="data.active" icon="pi pi-copy" label="Nhân bản" size="small" text @click="openPolicyDialog(data); resetMessages()" />
            </template>
          </Column>
        </DataTable>
      </div>

      <!-- TAB 2: ELIGIBILITIES -->
      <div v-if="activeTab === 'ELIGIBILITY'" class="flex flex-col gap-6">
        <div class="flex justify-between items-center">
          <div>
            <h3 class="text-lg font-bold text-gray-900">Danh sách giáo viên được miễn giảm tiết</h3>
          </div>
          <Button label="Thêm diện miễn giảm" icon="pi pi-plus" @click="openEligibilityDialog(); resetMessages()" />
        </div>

        <DataTable :value="eligibilities" responsive-layout="scroll">
          <Column field="teacherName" header="Giáo viên" />
          <Column header="Diện miễn giảm">
            <template #body="{ data }">
              {{ getEligibilityRuleLabel(data.ruleCode) }}
            </template>
          </Column>
          <Column header="Khoảng hiệu lực" style="width: 220px">
            <template #body="{ data }">{{ data.validFrom }} → {{ data.validTo }}</template>
          </Column>
          <Column field="evidenceReference" header="Minh chứng / Hồ sơ" />
          <Column header="Trạng thái" style="width: 130px">
            <template #body="{ data }">
              <Tag :value="data.status === 'REVOKED' ? 'Đã thu hồi' : 'Đang hiệu lực'" :severity="data.status === 'REVOKED' ? 'secondary' : 'success'" />
            </template>
          </Column>
          <Column header="Thao tác" style="width: 100px" class="text-right">
            <template #body="{ data }">
              <Button icon="pi pi-pencil" severity="secondary" text rounded :disabled="data.status === 'REVOKED'" @click="openEligibilityDialog(data); resetMessages()" />
              <Button
                v-if="data.status !== 'REVOKED'"
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
    </div>

    <!-- Create Policy Dialog -->
    <Dialog v-model:visible="isPolicyDialogVisible" :header="policyDialogMode === 'clone' ? 'Tạo phiên bản chính sách mới' : 'Thêm chính sách định mức mới'" modal class="timetable-dialog timetable-settings-dialog" :style="{ width: 'min(94vw, 680px)' }" :dismissable-mask="false">
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
        <div class="settings-form-grid grid grid-cols-1 sm:grid-cols-2 gap-2">
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
        <div class="settings-rule-editor flex flex-col gap-3">
          <div class="flex items-center justify-between gap-2">
            <div>
              <h4 class="font-semibold text-sm">Quy tắc miễn giảm bổ sung</h4>
              <p class="text-xs text-gray-500">Các mức giảm được cộng dồn và áp dụng theo điều kiện miễn giảm.</p>
            </div>
            <Button label="Thêm quy tắc" icon="pi pi-plus" size="small" severity="secondary" outlined @click="addRuleDraft" />
          </div>
          <div v-for="(rule, index) in ruleDrafts" :key="index" class="settings-rule-row grid grid-cols-1 sm:grid-cols-2 gap-2">
            <InputText v-model="rule.ruleCode" placeholder="Mã quy tắc, ví dụ SENIORITY" />
            <InputText v-model="rule.ruleName" placeholder="Tên diện miễn giảm" />
            <Select v-model="rule.triggerType" :options="[{ label: 'Theo điều kiện miễn giảm', value: 'ELIGIBILITY' }, { label: 'Theo giáo viên chủ nhiệm', value: 'HOMEROOM' }]" option-label="label" option-value="value" />
            <InputNumber v-model="rule.reductionPeriods" :min="0" placeholder="Số tiết giảm" />
            <InputText v-model="rule.source" class="sm:col-span-2" placeholder="Văn bản/căn cứ của quy tắc" />
            <Button label="Bỏ quy tắc" icon="pi pi-trash" severity="danger" text class="sm:col-span-2 justify-self-start" @click="removeRuleDraft(index)" />
          </div>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-2 mt-4">
          <Button label="Hủy" severity="secondary" text @click="isPolicyDialogVisible = false" />
          <Button :label="policyDialogMode === 'clone' ? 'Tạo phiên bản' : 'Tạo chính sách'" :loading="policySaving" @click="handleSavePolicy" />
        </div>
      </template>
    </Dialog>

    <!-- Create Eligibility Dialog -->
    <Dialog v-model:visible="isEligibilityDialogVisible" :header="editingEligibilityId ? 'Sửa diện miễn giảm giáo viên' : 'Thêm diện miễn giảm giáo viên'" modal class="timetable-dialog timetable-settings-dialog" :style="{ width: 'min(94vw, 620px)' }" :dismissable-mask="false">
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
            :options="eligibilityRuleOptions"
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
