import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import { ApiError } from '@/types/api'
import type { Student, StudentPage, StudentQuery } from '@/types/student'
import StudentListView from './StudentListView.vue'

const mocks = vi.hoisted(() => ({
  fetchStudents: vi.fn(),
  deleteStudent: vi.fn(),
  transitionStudentStatus: vi.fn(),
  confirmRequire: vi.fn(),
  push: vi.fn(),
  replace: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mocks.push,
    replace: mocks.replace,
  }),
}))

vi.mock('primevue/useconfirm', () => ({
  useConfirm: () => ({ require: mocks.confirmRequire }),
}))

vi.mock('@/services/studentApi', () => ({
  fetchStudents: mocks.fetchStudents,
  deleteStudent: mocks.deleteStudent,
  transitionStudentStatus: mocks.transitionStudentStatus,
}))

const sampleStudents: Student[] = [
  {
    studentId: 101,
    studentCode: 'STU0000001',
    studentName: 'Nguyễn Văn An',
    dateOfBirth: '2010-05-15',
    address: 'Hà Nội',
    status: 'ACTIVE',
    currentClassCode: '6A1',
    currentClassId: 1,
    account: null,
  },
  {
    studentId: 102,
    studentCode: 'STU0000002',
    studentName: 'Trần Thị Bình',
    dateOfBirth: '2010-08-20',
    address: 'Hà Nội',
    status: 'ACTIVE',
    currentClassCode: '6A2',
    currentClassId: 2,
    account: null,
  },
]

const sampleStudentPage: StudentPage = {
  content: sampleStudents,
  page: 0,
  size: 10,
  totalElements: 2,
  totalPages: 1,
}

const buttonStub = {
  props: ['label', 'loading', 'disabled'],
  emits: ['click'],
  template: '<button :disabled="disabled || loading" @click="$emit(\'click\')">{{ label }}<slot /></button>',
}

const dialogStub = {
  props: ['visible', 'header'],
  emits: ['update:visible'],
  template: '<div v-if="visible" class="dialog-stub"><h3>{{ header }}</h3><slot /></div>',
}

const confirmDialogStub = {
  template: '<div class="confirm-dialog-stub" />',
}

const studentSearchFormStub = {
  props: ['loading'],
  emits: ['search'],
  template: '<div class="student-search-form-stub"><button data-testid="trigger-search" @click="$emit(\'search\', { studentCode: \'STU0000001\', studentName: \'\', dateOfBirth: null })">Search</button></div>',
}

const studentTableStub = {
  props: ['students', 'loading', 'totalRecords', 'totalPages', 'page', 'rowsPerPage', 'sortField', 'sortOrder', 'canManageStudents'],
  emits: ['pageChange', 'sortChange', 'viewDetail', 'edit', 'delete'],
  template: `
    <div class="student-table-stub">
      <div v-for="student in students" :key="student.studentId" class="student-row">
        <span class="student-name">{{ student.studentName }}</span>
        <button :data-testid="'view-btn-' + student.studentId" @click="$emit('viewDetail', student)">Detail</button>
        <button v-if="canManageStudents" :data-testid="'edit-btn-' + student.studentId" @click="$emit('edit', student)">Edit</button>
        <button v-if="canManageStudents" :data-testid="'delete-btn-' + student.studentId" @click="$emit('delete', student)">Delete</button>
      </div>
      <button data-testid="trigger-page" @click="$emit('pageChange', 1, 10)">Page</button>
      <button data-testid="trigger-sort" @click="$emit('sortChange', 'studentName', 1)">Sort</button>
    </div>
  `,
}

function mountView() {
  return mount(StudentListView, {
    global: {
      stubs: {
        Button: buttonStub,
        Dialog: dialogStub,
        ConfirmDialog: confirmDialogStub,
        StudentSearchForm: studentSearchFormStub,
        StudentTable: studentTableStub,
      },
    },
  })
}

describe('StudentListView.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    clearAuthSession()
    saveAuthSession({
      accessToken: 'test-token',
      user: { id: 1, username: 'admin', roles: ['ADMIN'] },
    })
    mocks.fetchStudents.mockImplementation((_token: string, q: StudentQuery) =>
      Promise.resolve({ ...sampleStudentPage, page: q?.page ?? 0 }),
    )
  })

  afterEach(() => {
    clearAuthSession()
  })

  it('fetches and renders students on mount', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(mocks.fetchStudents).toHaveBeenCalledWith(
      'test-token',
      expect.objectContaining({
        page: 0,
        pageSize: 10,
        sortField: 'studentCode',
      }),
    )
    expect(wrapper.text()).toContain('Nguyễn Văn An')
    expect(wrapper.text()).toContain('Trần Thị Bình')
  })

  it('navigates to create student page when clicking Add button', async () => {
    const wrapper = mountView()
    await flushPromises()

    const addBtn = wrapper.findAll('button').find((b) => b.text().includes('Thêm học sinh'))
    expect(addBtn).toBeDefined()
    await addBtn!.trigger('click')

    expect(mocks.push).toHaveBeenCalledWith('/v2/students/new')
  })

  it('hides mutation actions for a TEACHER while retaining read access', async () => {
    clearAuthSession()
    saveAuthSession({ accessToken: 'teacher-token', user: { id: 3, username: 'teacher01', roles: ['TEACHER'] } })
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).not.toContain('Thêm học sinh')
    expect(wrapper.find('[data-testid="edit-btn-101"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="delete-btn-101"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="view-btn-101"]').exists()).toBe(true)
  })

  it('navigates to student detail on viewDetail event', async () => {
    const wrapper = mountView()
    await flushPromises()

    const detailBtn = wrapper.find('[data-testid="view-btn-101"]')
    await detailBtn.trigger('click')

    expect(mocks.push).toHaveBeenCalledWith('/v2/students/101')
  })

  it('navigates to student edit on edit event', async () => {
    const wrapper = mountView()
    await flushPromises()

    const editBtn = wrapper.find('[data-testid="edit-btn-101"]')
    await editBtn.trigger('click')

    expect(mocks.push).toHaveBeenCalledWith('/v2/students/101/edit')
  })

  it('refetches when search form triggers search event', async () => {
    const wrapper = mountView()
    await flushPromises()

    const searchBtn = wrapper.find('[data-testid="trigger-search"]')
    await searchBtn.trigger('click')
    await flushPromises()

    expect(mocks.fetchStudents).toHaveBeenCalledWith(
      'test-token',
      expect.objectContaining({
        page: 0,
        search: expect.objectContaining({ studentCode: 'STU0000001' }),
      }),
    )
  })

  it('refetches when pagination changes', async () => {
    const wrapper = mountView()
    await flushPromises()

    const pageBtn = wrapper.find('[data-testid="trigger-page"]')
    await pageBtn.trigger('click')
    await flushPromises()

    expect(mocks.fetchStudents).toHaveBeenCalledWith(
      'test-token',
      expect.objectContaining({ page: 1, pageSize: 10 }),
    )
  })

  it('refetches when sort field changes', async () => {
    const wrapper = mountView()
    await flushPromises()

    const sortBtn = wrapper.find('[data-testid="trigger-sort"]')
    await sortBtn.trigger('click')
    await flushPromises()

    expect(mocks.fetchStudents).toHaveBeenCalledWith(
      'test-token',
      expect.objectContaining({ sortField: 'studentName', sortOrder: 1 }),
    )
  })

  it('handles delete confirmation and triggers deleteStudent API', async () => {
    mocks.deleteStudent.mockResolvedValueOnce(undefined)
    mocks.confirmRequire.mockImplementationOnce((options: { accept: () => void }) => {
      options.accept()
    })

    const wrapper = mountView()
    await flushPromises()

    const deleteBtn = wrapper.find('[data-testid="delete-btn-101"]')
    await deleteBtn.trigger('click')
    await flushPromises()

    expect(mocks.confirmRequire).toHaveBeenCalled()
    expect(mocks.deleteStudent).toHaveBeenCalledWith('test-token', 101)
  })

  it('opens safe lifecycle dialog R5 when delete fails due to linked academic data', async () => {
    mocks.deleteStudent.mockRejectedValueOnce(new ApiError(409, 'Student has academic dependencies'))
    mocks.confirmRequire.mockImplementationOnce((options: { accept: () => void }) => {
      options.accept()
    })

    const wrapper = mountView()
    await flushPromises()

    const deleteBtn = wrapper.find('[data-testid="delete-btn-101"]')
    await deleteBtn.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Chính sách bảo toàn dữ liệu học vụ (R5)')
    expect(wrapper.text()).toContain('Không thể xóa cứng hồ sơ học sinh')
  })

  it('transitions student status to INACTIVE from safe lifecycle dialog', async () => {
    mocks.deleteStudent.mockRejectedValueOnce(new ApiError(409, 'Student has academic dependencies'))
    mocks.transitionStudentStatus.mockResolvedValueOnce({
      ...sampleStudents[0],
      status: 'INACTIVE',
    })
    mocks.confirmRequire.mockImplementationOnce((options: { accept: () => void }) => {
      options.accept()
    })

    const wrapper = mountView()
    await flushPromises()

    // Trigger delete failure to open dialog
    const deleteBtn = wrapper.find('[data-testid="delete-btn-101"]')
    await deleteBtn.trigger('click')
    await flushPromises()

    // Click "Chuyển sang ngừng học"
    const inactiveBtn = wrapper.findAll('button').find((b) => b.text().includes('Chuyển sang ngừng học'))
    expect(inactiveBtn).toBeDefined()
    await inactiveBtn!.trigger('click')
    await flushPromises()

    expect(mocks.transitionStudentStatus).toHaveBeenCalledWith(
      'test-token',
      101,
      'INACTIVE',
    )
  })
})
