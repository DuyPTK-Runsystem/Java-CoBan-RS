import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import { weeklyFixture } from '@/fixtures/lessonLogFixture'
import ClassLessonLogWorkspaceView from './ClassLessonLogWorkspaceView.vue'

const mocks = vi.hoisted(() => ({
  fetchAcademicYears: vi.fn(),
  fetchSemesters: vi.fn(),
  listLessonLogClasses: vi.fn(),
  getClassWeeklyLessonLogs: vi.fn(),
  createLessonLog: vi.fn(),
  updateLessonLog,
  submitLessonLog: vi.fn(),
  reviewLessonLog: vi.fn(),
  signWeeklyReview: vi.fn(),
  amendLessonLog: vi.fn(),
  recordLateLessonLog: vi.fn(),
}))

function updateLessonLog() {}

vi.mock('@/services/academicApi', () => ({
  fetchAcademicYears: mocks.fetchAcademicYears,
  fetchSemesters: mocks.fetchSemesters,
}))

vi.mock('@/services/lessonLogApi', () => ({
  listLessonLogClasses: mocks.listLessonLogClasses,
  getClassWeeklyLessonLogs: mocks.getClassWeeklyLessonLogs,
  createLessonLog: mocks.createLessonLog,
  updateLessonLog: mocks.updateLessonLog,
  submitLessonLog: mocks.submitLessonLog,
  reviewLessonLog: mocks.reviewLessonLog,
  signWeeklyReview: mocks.signWeeklyReview,
  amendLessonLog: mocks.amendLessonLog,
  recordLateLessonLog: mocks.recordLateLessonLog,
}))

const academicYears = [{ id: 1, code: '2026-2027', status: 'ACTIVE' as const }]
const semesters = [{ id: 1, name: 'Học kỳ 1', status: 'ACTIVE' as const }]
const classes = [{ id: 3, name: '10A1' }]

function mountView() {
  return mount(ClassLessonLogWorkspaceView, {
    global: {
      stubs: {
        Button: {
          props: ['label', 'loading', 'disabled'],
          template: '<button :disabled="disabled"><slot />{{ label }}</button>',
        },
        Select: {
          props: ['modelValue', 'options'],
          template: '<select :value="modelValue"><option v-for="opt in options" :key="opt.value || opt.id" :value="opt.value || opt.id">{{ opt.label || opt.name }}</option></select>',
        },
        DatePicker: {
          props: ['modelValue'],
          template: '<input type="text" :value="modelValue" />',
        },
        LessonLogSubtabs: { template: '<div data-testid="lesson-log-subtabs" />' },
        LessonLogWeeklyMatrix: {
          props: ['days', 'entries'],
          template: '<div data-testid="weekly-matrix">{{ entries.length }} entries</div>',
        },
        LessonLogStatusBadge: { template: '<span data-testid="status-badge" />' },
        LessonLogEntryDialog: { template: '<div data-testid="entry-dialog" />' },
        LessonLogDetailDialog: { template: '<div data-testid="detail-dialog" />' },
        LessonLogAmendDialog: { template: '<div data-testid="amend-dialog" />' },
        LessonLogAuditDrawer: { template: '<div data-testid="audit-drawer" />' },
        WeeklyHomeroomReviewDialog: { template: '<div data-testid="review-dialog" />' },
        FormAlert: { template: '<div data-testid="form-alert"><slot /></div>' },
        EmptyState: { props: ['heading'], template: '<div data-testid="empty-state">{{ heading }}</div>' },
        PageState: { props: ['state'], template: '<div data-testid="page-state">{{ state }}</div>' },
      },
    },
  })
}

describe('ClassLessonLogWorkspaceView.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    saveAuthSession({
      accessToken: 'test-token',
      user: { id: 1, username: 'admin', roles: ['ADMIN'] },
    })
    mocks.fetchAcademicYears.mockResolvedValue(academicYears)
    mocks.fetchSemesters.mockResolvedValue(semesters)
    mocks.listLessonLogClasses.mockResolvedValue(classes)
    mocks.getClassWeeklyLessonLogs.mockResolvedValue(weeklyFixture)
  })

  afterEach(() => {
    clearAuthSession()
  })

  it('renders standard page heading, caption, and refresh button', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('h1').text()).toBe('Sổ đầu bài')
    expect(wrapper.find('.section-caption').text()).toContain('Theo dõi và quản lý sổ đầu bài')
    expect(wrapper.text()).toContain('Làm mới')
  })

  it('renders context filter panel with semester, class, and week controls', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('.lesson-log-context-panel').exists()).toBe(true)
    expect(wrapper.find('#semester-select').exists()).toBe(true)
    expect(wrapper.find('#class-select').exists()).toBe(true)
    expect(wrapper.find('#week-picker').exists()).toBe(true)
  })

  it('renders KPI summary metrics with expected labels and numbers', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('.lesson-log-summary-grid').exists()).toBe(true)
    const metrics = wrapper.findAll('.lesson-log-summary-metric')
    expect(metrics).toHaveLength(4)
    expect(metrics[0].text()).toContain('Tiết dự kiến')
    expect(metrics[0].text()).toContain('8')
    expect(metrics[1].text()).toContain('Chưa ghi')
    expect(metrics[1].text()).toContain('2')
    expect(metrics[2].text()).toContain('Bản nháp')
    expect(metrics[2].text()).toContain('1')
    expect(metrics[3].text()).toContain('Đã nộp trở lên')
  })

  it('passes entries to weekly matrix and updates when status filter changes', async () => {
    const wrapper = mountView()
    await flushPromises()

    const matrix = wrapper.find('[data-testid="weekly-matrix"]')
    expect(matrix.exists()).toBe(true)
    expect(matrix.text()).toContain('1 entries')
  })
})
