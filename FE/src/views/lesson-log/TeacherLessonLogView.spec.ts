import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import { scheduleFixture } from '@/fixtures/lessonLogFixture'
import TeacherLessonLogView from './TeacherLessonLogView.vue'

const mocks = vi.hoisted(() => ({
  getMyLessonSchedule: vi.fn(),
  createLessonLog: vi.fn(),
  updateLessonLog: vi.fn(),
  submitLessonLog: vi.fn(),
}))

vi.mock('@/services/lessonLogApi', () => ({
  getMyLessonSchedule: mocks.getMyLessonSchedule,
  createLessonLog: mocks.createLessonLog,
  updateLessonLog: mocks.updateLessonLog,
  submitLessonLog: mocks.submitLessonLog,
}))

function mountView() {
  return mount(TeacherLessonLogView, {
    global: {
      stubs: {
        Button: {
          props: ['label', 'loading', 'disabled'],
          template: '<button :disabled="disabled"><slot />{{ label }}</button>',
        },
        DatePicker: {
          props: ['modelValue'],
          template: '<input type="text" :value="modelValue" />',
        },
        LessonLogStatusBadge: {
          props: ['status'],
          template: '<span data-testid="status-badge">{{ status }}</span>',
        },
        LessonLogEntryDialog: { template: '<div data-testid="entry-dialog" />' },
        LessonLogDetailDialog: { template: '<div data-testid="detail-dialog" />' },
        LessonLogAuditDrawer: { template: '<div data-testid="audit-drawer" />' },
        FormAlert: { template: '<div data-testid="form-alert"><slot /></div>' },
        EmptyState: {
          props: ['heading', 'message'],
          template: '<div data-testid="empty-state"><h3>{{ heading }}</h3><p>{{ message }}</p></div>',
        },
        PageState: { props: ['state'], template: '<div data-testid="page-state">{{ state }}</div>' },
      },
    },
  })
}

describe('TeacherLessonLogView.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    saveAuthSession({
      accessToken: 'test-token',
      user: { id: 7, username: 'teacher', roles: ['TEACHER'] },
    })
    mocks.getMyLessonSchedule.mockResolvedValue({ items: scheduleFixture })
  })

  afterEach(() => {
    clearAuthSession()
  })

  it('renders standard page heading, caption, and refresh button', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('h1').text()).toBe('Sổ đầu bài của tôi')
    expect(wrapper.find('.section-caption').text()).toContain('Xem lịch phân công và ghi sổ đầu bài')
    expect(wrapper.text()).toContain('Làm mới')
  })

  it('renders date picker in context panel', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('.lesson-log-context-panel').exists()).toBe(true)
    expect(wrapper.find('#teacher-date-picker').exists()).toBe(true)
  })

  it('renders schedule cards with subject, class, period, and action buttons', async () => {
    const wrapper = mountView()
    await flushPromises()

    const cards = wrapper.findAll('.teacher-schedule-card')
    expect(cards.length).toBeGreaterThan(0)
    expect(cards[0].text()).toContain('Toán')
    expect(cards[0].text()).toContain('10A1')
    expect(cards[0].text()).toContain('Tiết 1')
  })

  it('renders EmptyState when there are no scheduled lessons', async () => {
    mocks.getMyLessonSchedule.mockResolvedValueOnce({ items: [] })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="empty-state"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Không có tiết dạy')
  })
})
