import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import { ApiError } from '@/types/api'
import type { Student } from '@/types/student'
import StudentFormView from './StudentFormView.vue'

const mocks = vi.hoisted(() => ({
  createStudent: vi.fn(),
  createStudentV3: vi.fn(),
  updateStudent: vi.fn(),
  getStudent: vi.fn(),
  generateStudentCode: vi.fn(),
  push: vi.fn(),
  replace: vi.fn(),
  params: {} as Record<string, string>,
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mocks.push,
    replace: mocks.replace,
  }),
  useRoute: () => ({
    params: mocks.params,
  }),
}))

vi.mock('@/services/studentApi', () => ({
  createStudent: mocks.createStudent,
  createStudentV3: mocks.createStudentV3,
  updateStudent: mocks.updateStudent,
  getStudent: mocks.getStudent,
  generateStudentCode: mocks.generateStudentCode,
}))

const sampleStudent: Student = {
  studentId: 101,
  studentCode: 'STU0000001',
  studentName: 'Nguyễn Văn An',
  dateOfBirth: '2010-05-15',
  address: 'Hà Nội',
  status: 'ACTIVE',
  currentClassCode: null,
  currentClassId: null,
  account: null,
}

const studentFormStub = {
  props: ['mode', 'initialValue', 'saving', 'generating', 'errorMessage', 'canProvisionAccount'],
  emits: ['save', 'generateCode', 'back'],
  template: `
    <div class="student-form-stub">
      <span class="mode-text">{{ mode }}</span>
      <span class="can-provision">{{ canProvisionAccount }}</span>
      <button data-testid="btn-save-v1" @click="$emit('save', { studentCode: 'STU0000001', studentName: 'Nguyễn Văn An', dateOfBirth: new Date('2010-05-15'), address: 'Hà Nội', provisionAccount: false })">Save V2</button>
      <button data-testid="btn-save-v3" @click="$emit('save', { studentCode: 'STU0000001', studentName: 'Nguyễn Văn An', dateOfBirth: new Date('2010-05-15'), address: 'Hà Nội', provisionAccount: true, username: 'nguyenvanan01' })">Save V3</button>
      <button data-testid="btn-gen-code" @click="$emit('generateCode')">Gen Code</button>
      <button data-testid="btn-back" @click="$emit('back')">Back</button>
    </div>
  `,
}

function mountView() {
  return mount(StudentFormView, {
    global: {
      stubs: {
        StudentForm: studentFormStub,
        Dialog: {
          props: ['visible', 'header'],
          template: '<section v-if="visible"><h2>{{ header }}</h2><slot /><slot name="footer" /></section>',
        },
        Button: { template: '<button><slot /></button>' },
      },
    },
  })
}

describe('StudentFormView.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    clearAuthSession()
    mocks.params = {}
    saveAuthSession({
      accessToken: 'test-token',
      user: { id: 1, username: 'admin', roles: ['ADMIN'] },
    })
    mocks.createStudent.mockResolvedValue(sampleStudent)
    mocks.createStudentV3.mockResolvedValue({
      studentId: 101,
      studentCode: 'STU0000001',
      studentName: 'Nguyễn Văn An',
      account: { userId: 10, username: 'nguyenvanan01', role: 'STUDENT' },
    })
    mocks.updateStudent.mockResolvedValue(sampleStudent)
    mocks.getStudent.mockResolvedValue(sampleStudent)
    mocks.generateStudentCode.mockResolvedValue({ studentCode: 'STU9999999' })
  })

  afterEach(() => {
    clearAuthSession()
  })

  it('calls createStudent and redirects to /v2/students when saving without account provisioning', async () => {
    const wrapper = mountView()
    await flushPromises()

    const saveV1Btn = wrapper.find('[data-testid="btn-save-v1"]')
    await saveV1Btn.trigger('click')
    await flushPromises()

    expect(mocks.createStudent).toHaveBeenCalledWith(
      'test-token',
      expect.objectContaining({
        studentCode: 'STU0000001',
        studentName: 'Nguyễn Văn An',
        dateOfBirth: '2010-05-15',
        address: 'Hà Nội',
      }),
    )
    expect(mocks.push).toHaveBeenCalledWith('/v2/students')
  })

  it('shows the returned username after creating a student account', async () => {
    const wrapper = mountView()
    await flushPromises()

    const saveV3Btn = wrapper.find('[data-testid="btn-save-v3"]')
    await saveV3Btn.trigger('click')
    await flushPromises()

    expect(mocks.createStudentV3).toHaveBeenCalledWith(
      'test-token',
      expect.objectContaining({
        studentCode: 'STU0000001',
        studentName: 'Nguyễn Văn An',
        username: 'nguyenvanan01',
        password: null,
      }),
    )
    expect(wrapper.text()).toContain('Đã cấp tài khoản học sinh')
    expect(wrapper.text()).toContain('Username: nguyenvanan01')
    expect(mocks.push).not.toHaveBeenCalledWith('/v2/students')
  })

  it('loads student and calls updateStudent when in edit mode', async () => {
    mocks.params = { studentId: '101' }
    const wrapper = mountView()
    await flushPromises()

    expect(mocks.getStudent).toHaveBeenCalledWith('test-token', 101)

    const saveBtn = wrapper.find('[data-testid="btn-save-v1"]')
    await saveBtn.trigger('click')
    await flushPromises()

    expect(mocks.updateStudent).toHaveBeenCalledWith(
      'test-token',
      101,
      expect.objectContaining({
        studentName: 'Nguyễn Văn An',
        dateOfBirth: '2010-05-15',
        address: 'Hà Nội',
      }),
    )
    expect(mocks.push).toHaveBeenCalledWith('/v2/students')
  })

  it('normalizes nullable v2 profile fields before passing them to the edit form', async () => {
    mocks.params = { studentId: '101' }
    mocks.getStudent.mockResolvedValueOnce({
      ...sampleStudent,
      dateOfBirth: null,
      address: null,
    })
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.findComponent(studentFormStub).props('initialValue')).toMatchObject({
      dateOfBirth: null,
      address: '',
    })
  })

  it('handles 409 conflict error properly when saving', async () => {
    mocks.createStudentV3.mockRejectedValueOnce(
      new ApiError(409, 'Mã sinh viên hoặc username đã tồn tại'),
    )

    const wrapper = mountView()
    await flushPromises()

    const saveV3Btn = wrapper.find('[data-testid="btn-save-v3"]')
    await saveV3Btn.trigger('click')
    await flushPromises()

    expect(wrapper.findComponent(studentFormStub).props('errorMessage')).toContain(
      'Mã sinh viên hoặc username đã tồn tại',
    )
  })

  it('navigates back to /v2/students on back event', async () => {
    const wrapper = mountView()
    await flushPromises()

    const backBtn = wrapper.find('[data-testid="btn-back"]')
    await backBtn.trigger('click')

    expect(mocks.push).toHaveBeenCalledWith('/v2/students')
  })
})
