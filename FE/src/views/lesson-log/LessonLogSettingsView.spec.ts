import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import { lessonLogPolicyFixture } from '@/fixtures/lessonLogFixture'
import LessonLogSettingsView from './LessonLogSettingsView.vue'

const mocks = vi.hoisted(() => ({
  getLessonLogPolicy: vi.fn(),
  updateLessonLogPolicy: vi.fn(),
}))

vi.mock('@/services/lessonLogApi', () => ({
  getLessonLogPolicy: mocks.getLessonLogPolicy,
  updateLessonLogPolicy: mocks.updateLessonLogPolicy,
}))

function mountView() {
  return mount(LessonLogSettingsView, {
    global: {
      stubs: {
        Button: {
          props: ['label', 'loading', 'disabled'],
          template: '<button :disabled="disabled"><slot />{{ label }}</button>',
        },
        Select: {
          props: ['modelValue', 'options'],
          template: '<select :value="modelValue"><option v-for="opt in options" :key="opt.value" :value="opt.value">{{ opt.label }}</option></select>',
        },
        InputNumber: {
          props: ['modelValue'],
          template: '<input type="number" :value="modelValue" />',
        },
        InputText: {
          props: ['modelValue', 'id'],
          emits: ['update:modelValue'],
          template: '<input :id="id" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
        },
        LessonLogSubtabs: { props: ['showPolicy'], template: '<div data-testid="lesson-log-subtabs" />' },
        FormAlert: { template: '<div data-testid="form-alert"><slot /></div>' },
        PageState: { props: ['state'], template: '<div data-testid="page-state">{{ state }}</div>' },
      },
    },
  })
}

describe('LessonLogSettingsView.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    saveAuthSession({
      accessToken: 'test-token',
      user: { id: 1, username: 'admin', roles: ['ADMIN'] },
    })
    mocks.getLessonLogPolicy.mockResolvedValue(lessonLogPolicyFixture)
  })

  afterEach(() => {
    clearAuthSession()
  })

  it('renders standard page heading, caption, and refresh button', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('h1').text()).toBe('Chính sách sổ đầu bài')
    expect(wrapper.find('.section-caption').text()).toContain('Cấu hình thời hạn ghi, sửa')
    expect(wrapper.text()).toContain('Làm mới')
  })

  it('renders policy settings surface and form fields when loaded', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('.lesson-log-settings-surface').exists()).toBe(true)
    expect(wrapper.find('#policy-deadline-mode').exists()).toBe(true)
    expect(wrapper.find('#policy-effective-date').exists()).toBe(true)
    expect(wrapper.find('#policy-timezone').exists()).toBe(true)
    expect(wrapper.find('#policy-reason').exists()).toBe(true)
  })

  it('requires reason before allowing submission', async () => {
    const wrapper = mountView()
    await flushPromises()

    const submitBtn = wrapper.findAll('button').find((btn) => btn.text().includes('Tạo phiên bản chính sách mới'))
    expect(submitBtn).toBeDefined()
    expect(submitBtn?.attributes('disabled')).toBeDefined()
  })
})
