import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import PlacementSessionSetup from './PlacementSessionSetup.vue'

const academicYears = [
  { id: 1, code: '2026-2027', startDate: '2026-09-01', endDate: '2027-05-31', status: 'ACTIVE' as const, notes: null },
]
const grades = [
  { id: 8, code: 'GRADE_8', name: 'Khối 8', gradeLevel: 8 as const, displayOrder: 1, nextGradeId: 9, active: true, description: null },
]
const classes = [
  { id: 81, academicYearId: 1, gradeLevelId: 8, classCode: '8A1', className: 'Lớp 8A1', capacity: 35, status: 'ACTIVE' as const },
  { id: 82, academicYearId: 1, gradeLevelId: 8, classCode: '8A2', className: 'Lớp 8A2', capacity: 30, status: 'ACTIVE' as const },
]
const unassignedStudents = [
  { studentId: 101, studentCode: 'HS101', studentName: 'Nguyễn Văn A' },
  { studentId: 102, studentCode: 'HS102', studentName: 'Trần Thị B' },
]

describe('PlacementSessionSetup', () => {
  it('renders academic scope, classes, and candidate lists', () => {
    const wrapper = mount(PlacementSessionSetup, {
      props: {
        academicYears,
        grades,
        classes,
        unassignedStudents,
      },
      global: {
        stubs: {
          Select: {
            props: ['modelValue', 'options'],
            emits: ['update:modelValue'],
            template: '<select :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><option v-for="o in options" :key="o.id || o.value" :value="o.id || o.value">{{ o.name || o.code || o.label }}</option></select>',
          },
          Checkbox: {
            props: ['modelValue'],
            emits: ['update:modelValue'],
            template: '<input type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)" />',
          },
          Button: {
            props: ['label', 'disabled', 'loading'],
            emits: ['click'],
            template: '<button :disabled="disabled || loading" @click="$emit(\'click\')">{{ label }}</button>',
          },
          Tag: { props: ['value'], template: '<span>{{ value }}</span>' },
          InputText: {
            props: ['modelValue', 'placeholder'],
            emits: ['update:modelValue'],
            template: '<input :value="modelValue" :placeholder="placeholder" @input="$emit(\'update:modelValue\', $event.target.value)" />',
          },
        },
      },
    })

    expect(wrapper.text()).toContain('Tạo phiên xếp lớp tự động')
    expect(wrapper.text()).toContain('8A1')
    expect(wrapper.text()).toContain('8A2')
    expect(wrapper.text()).toContain('HS101')
    expect(wrapper.text()).toContain('Học sinh nhập học mới hoặc học lại cần có căn cứ và thông tin phê duyệt trước khi xếp lớp.')
    expect(wrapper.text()).toContain('✓ Đủ điều kiện lên lớp theo kết quả năm học trước. Không cần bổ sung hồ sơ.')
    expect(wrapper.text()).not.toContain('V3 · PLACEMENT')
    expect(wrapper.text()).not.toContain('CONTINUING')
    expect(wrapper.text()).not.toContain('NEW_ADMISSION')
    expect(wrapper.text()).not.toContain('REPEAT')
    expect(wrapper.text()).not.toContain('profile')
    // Verify old fixed columns are removed
    expect(wrapper.text()).not.toContain('Căn cứ xét xếp lớp')
    expect(wrapper.text()).not.toContain('Mã/phê duyệt tham chiếu')
  })

  it('renders supplementary inputs with specific labels and placeholders for NEW_ADMISSION and REPEAT', async () => {
    interface SetupVm {
      candidateConfigs: Record<number, {
        selected: boolean
        sourceType: 'CONTINUING' | 'NEW_ADMISSION' | 'REPEAT'
        eligibilityEvidence: string
        approvalReference: string
      }>
    }

    const wrapper = mount(PlacementSessionSetup, {
      props: {
        academicYears,
        grades,
        classes,
        unassignedStudents,
      },
      global: {
        stubs: {
          Select: {
            props: ['modelValue', 'options'],
            emits: ['update:modelValue'],
            template: '<select :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><option v-for="o in options" :key="o.id || o.value" :value="o.id || o.value">{{ o.name || o.code || o.label }}</option></select>',
          },
          Checkbox: {
            props: ['modelValue'],
            emits: ['update:modelValue'],
            template: '<input type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)" />',
          },
          Button: true,
          Tag: true,
          InputText: {
            props: ['modelValue', 'placeholder', 'id', 'size'],
            emits: ['update:modelValue'],
            template: '<input :id="id" :value="modelValue" :placeholder="placeholder" @input="$emit(\'update:modelValue\', $event.target.value)" />',
          },
        },
      },
    })

    const vm = wrapper.vm as unknown as SetupVm

    // 1. Switch student 101 to NEW_ADMISSION
    vm.candidateConfigs[101].sourceType = 'NEW_ADMISSION'
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Căn cứ nhập học *')
    expect(wrapper.text()).toContain('Mã hồ sơ / quyết định tiếp nhận *')
    const newAdmissionInputEvidence = wrapper.find(`input[id="evidence-101"]`)
    expect(newAdmissionInputEvidence.attributes('placeholder')).toBe('VD: Hồ sơ chuyển trường từ THCS ABC, Trúng tuyển đầu cấp...')
    const newAdmissionInputRef = wrapper.find(`input[id="ref-101"]`)
    expect(newAdmissionInputRef.attributes('placeholder')).toBe('VD: QĐ-124/THCS-2026, HS-TS-2026-0045...')

    // 2. Switch student 102 to REPEAT
    vm.candidateConfigs[102].sourceType = 'REPEAT'
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Căn cứ học lại *')
    expect(wrapper.text()).toContain('Mã quyết định / biên bản *')
    const repeatInputEvidence = wrapper.find(`input[id="evidence-102"]`)
    expect(repeatInputEvidence.attributes('placeholder')).toBe('VD: Học bạ năm trước chưa đủ điều kiện lên lớp...')
    const repeatInputRef = wrapper.find(`input[id="ref-102"]`)
    expect(repeatInputRef.attributes('placeholder')).toBe('VD: BB-HDXL-09/2026, QĐ-08/LƯU-BAN...')
  })

  it('validates candidate requiring eligibility evidence and approval reference for NEW_ADMISSION', async () => {
    const wrapper = mount(PlacementSessionSetup, {
      props: {
        academicYears,
        grades,
        classes,
        unassignedStudents,
        initialAcademicYearId: 1,
        initialGradeId: 8,
      },
      global: {
        stubs: {
          Select: true,
          Checkbox: true,
          DataTable: true,
          Column: true,
          Button: {
            props: ['label'],
            emits: ['click'],
            template: '<button @click="$emit(\'click\')">{{ label }}</button>',
          },
          Tag: true,
          InputText: true,
        },
      },
    })

    interface SetupVm {
      candidateConfigs: Record<number, {
        selected: boolean
        sourceType: 'CONTINUING' | 'NEW_ADMISSION' | 'REPEAT'
        eligibilityEvidence: string
        approvalReference: string
      }>
      classProfiles: Record<number, {
        selected: boolean
        profile: 'ADVANCED' | 'SUPPORT' | 'REGULAR'
      }>
    }

    const vm = wrapper.vm as unknown as SetupVm
    // Set student 101 as NEW_ADMISSION without evidence
    vm.candidateConfigs[101].sourceType = 'NEW_ADMISSION'
    vm.candidateConfigs[101].eligibilityEvidence = ''
    vm.candidateConfigs[101].approvalReference = ''

    // Find submit button
    const submitBtn = wrapper.findAll('button').find((b) => b.text().includes('Tạo phiên nháp'))
    await submitBtn?.trigger('click')

    expect(wrapper.emitted('submit')).toBeUndefined()
    expect(wrapper.text()).toContain('bắt buộc phải có căn cứ xét xếp lớp và mã/phê duyệt tham chiếu')
  })

  it('validates candidate requiring eligibility evidence and approval reference for REPEAT', async () => {
    const wrapper = mount(PlacementSessionSetup, {
      props: {
        academicYears,
        grades,
        classes,
        unassignedStudents,
        initialAcademicYearId: 1,
        initialGradeId: 8,
      },
      global: {
        stubs: {
          Select: true,
          Checkbox: true,
          DataTable: true,
          Column: true,
          Button: {
            props: ['label'],
            emits: ['click'],
            template: '<button @click="$emit(\'click\')">{{ label }}</button>',
          },
          Tag: true,
          InputText: true,
        },
      },
    })

    interface SetupVm {
      candidateConfigs: Record<number, {
        selected: boolean
        sourceType: 'CONTINUING' | 'NEW_ADMISSION' | 'REPEAT'
        eligibilityEvidence: string
        approvalReference: string
      }>
    }

    const vm = wrapper.vm as unknown as SetupVm
    // Set student 101 as CONTINUING, student 102 as REPEAT without evidence
    vm.candidateConfigs[101].sourceType = 'CONTINUING'
    vm.candidateConfigs[102].sourceType = 'REPEAT'
    vm.candidateConfigs[102].eligibilityEvidence = '   '
    vm.candidateConfigs[102].approvalReference = ''

    const submitBtn = wrapper.findAll('button').find((b) => b.text().includes('Tạo phiên nháp'))
    await submitBtn?.trigger('click')

    expect(wrapper.emitted('submit')).toBeUndefined()
    expect(wrapper.text()).toContain('bắt buộc phải có căn cứ xét xếp lớp và mã/phê duyệt tham chiếu')
  })

  it('emits submit with valid CreatePlacementSessionRequest payload', async () => {
    const wrapper = mount(PlacementSessionSetup, {
      props: {
        academicYears,
        grades,
        classes,
        unassignedStudents,
        initialAcademicYearId: 1,
        initialGradeId: 8,
      },
      global: {
        stubs: {
          Select: true,
          Checkbox: true,
          DataTable: true,
          Column: true,
          Button: {
            props: ['label'],
            emits: ['click'],
            template: '<button @click="$emit(\'click\')">{{ label }}</button>',
          },
          Tag: true,
          InputText: true,
        },
      },
    })

    const vm = wrapper.vm as unknown as SetupVm
    // Set 8A1 as ADVANCED, 8A2 as REGULAR
    vm.classProfiles[81].profile = 'ADVANCED'
    vm.classProfiles[82].profile = 'REGULAR'

    // Set 101 as CONTINUING, 102 as NEW_ADMISSION with valid evidence
    vm.candidateConfigs[101].sourceType = 'CONTINUING'
    vm.candidateConfigs[102].sourceType = 'NEW_ADMISSION'
    vm.candidateConfigs[102].eligibilityEvidence = 'Chuyển trường'
    vm.candidateConfigs[102].approvalReference = 'QD-99'

    const submitBtn = wrapper.findAll('button').find((b) => b.text().includes('Tạo phiên nháp'))
    await submitBtn?.trigger('click')

    expect(wrapper.emitted('submit')).toBeDefined()
    const emittedPayload = wrapper.emitted('submit')?.[0]?.[0] as Record<string, unknown>
    expect(emittedPayload).toMatchObject({
      academicYearId: 1,
      targetGradeId: 8,
      ruleVersion: '074-v1',
      targetClasses: [
        { classId: 81, profile: 'ADVANCED', capacity: null },
        { classId: 82, profile: 'REGULAR', capacity: null },
      ],
      candidates: [
        {
          studentId: 101,
          targetGradeId: 8,
          sourceType: 'CONTINUING',
          score: null,
          scoreSourceReference: null,
          genderSnapshot: null,
          eligibilityEvidence: null,
          approvalReference: null,
        },
        {
          studentId: 102,
          targetGradeId: 8,
          sourceType: 'NEW_ADMISSION',
          score: null,
          scoreSourceReference: null,
          genderSnapshot: null,
          eligibilityEvidence: 'Chuyển trường',
          approvalReference: 'QD-99',
        },
      ],
    })
  })
})
