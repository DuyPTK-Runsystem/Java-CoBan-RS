import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import NotificationAudienceSelector from './NotificationAudienceSelector.vue'

const mocks = vi.hoisted(() => ({
  fetchClasses: vi.fn(),
  fetchIndividuals: vi.fn(),
  requireAccessToken: vi.fn(() => 'test-token'),
  routerReplace: vi.fn(),
}))

vi.mock('@/services/notificationApi', () => ({
  fetchNotificationClassAudiences: mocks.fetchClasses,
  fetchNotificationIndividualAudiences: mocks.fetchIndividuals,
}))

vi.mock('@/composables/useAuthSession', () => ({
  useAuthSession: () => ({ requireAccessToken: mocks.requireAccessToken }),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ replace: mocks.routerReplace }),
}))

const dropdownStub = {
  props: ['modelValue', 'options', 'loading'],
  template: '<select><option v-for="o in options" :key="o.value ?? o.classId" :value="o.value ?? o.classId">{{ o.label ?? o.className }}</option></select>',
}
const inputTextStub = {
  props: ['modelValue', 'placeholder', 'id'],
  template: '<input :id="id" :value="modelValue" :placeholder="placeholder" />',
}
const checkboxStub = {
  props: ['modelValue', 'binary', 'tabindex'],
  template: '<input type="checkbox" :checked="modelValue" :tabindex="tabindex" />',
}

function mountSelector(props: Record<string, unknown>) {
  return mount(NotificationAudienceSelector, {
    props,
    global: { stubs: { Checkbox: checkboxStub, Dropdown: dropdownStub, InputText: inputTextStub } },
  })
}

describe('NotificationAudienceSelector', () => {
  afterEach(() => {
    mocks.fetchClasses.mockReset()
    mocks.fetchIndividuals.mockReset()
    mocks.requireAccessToken.mockClear()
    mocks.routerReplace.mockClear()
  })

  it('renders the school audience notice without lookup', async () => {
    const wrapper = mountSelector({ audienceType: 'SCHOOL', targetReference: '' })
    await flushPromises()

    expect(wrapper.text()).toContain('Toàn trường')
    expect(mocks.fetchClasses).not.toHaveBeenCalled()
    expect(mocks.fetchIndividuals).not.toHaveBeenCalled()
  })

  it('renders a class picker and loads class lookup results', async () => {
    mocks.fetchClasses.mockResolvedValue({
      result: [{ classId: 101, classCode: '10A1', className: 'Lớp 10A1', academicYearId: 1, eligibleRecipientCount: 30 }],
      meta: { page: 0, pageSize: 20, totalPages: 1, totalItems: 1 },
    })

    const wrapper = mountSelector({ audienceType: 'CLASS', targetReference: '' })
    await flushPromises()

    expect(wrapper.text()).toContain('Lớp học nhận thông báo')
    expect(wrapper.find('#notification-class-picker').exists()).toBe(true)
    expect(wrapper.find('#notification-class-search').exists()).toBe(false)
    expect(wrapper.text()).toContain('10A1')
    expect(mocks.fetchClasses).toHaveBeenCalledWith('test-token', { q: '', page: 0, pageSize: 20 })
  })

  it('renders server-side filters and keeps canonical selection controls separate from filter metadata', async () => {
    mocks.fetchClasses.mockResolvedValue({ result: [], meta: { page: 0, pageSize: 20, totalPages: 1, totalItems: 0 } })
    mocks.fetchIndividuals.mockResolvedValue({ result: [], meta: { page: 0, pageSize: 20, totalPages: 1, totalItems: 0 } })

    const wrapper = mountSelector({ audienceType: 'INDIVIDUAL', recipientUserIds: [] })
    await flushPromises()

    expect(wrapper.find('#notification-user-search').exists()).toBe(true)
    expect(wrapper.find('#notification-role-filter').exists()).toBe(true)
    expect(wrapper.find('#notification-student-class-filter').exists()).toBe(true)
    expect(wrapper.find('#notification-teacher-class-filter').exists()).toBe(true)
    expect(wrapper.text()).toContain('Không tìm thấy người nhận phù hợp')
    expect(mocks.fetchIndividuals).toHaveBeenCalledWith('test-token', {
      q: '', roleCode: undefined, studentClassId: undefined, teacherClassId: undefined, page: 0, pageSize: 20,
    })
  })

  it('renders multi-select users, chips, and removes a selected user', async () => {
    mocks.fetchIndividuals.mockResolvedValue({
      result: [
        { userId: 2, displayName: 'Nguyễn Văn A', studentCode: 'HS001', username: 'nguyenvana' },
        { userId: 5, displayName: 'Trần Thị B', studentCode: 'HS002', username: 'tranthib' },
      ],
      meta: { page: 0, pageSize: 20, totalPages: 1, totalItems: 2 },
    })

    const wrapper = mountSelector({ audienceType: 'INDIVIDUAL', recipientUserIds: [] })
    await flushPromises()

    expect(wrapper.text()).toContain('Người nhận')
    expect(wrapper.text()).toContain('Chọn một hoặc nhiều người nhận')
    const results = wrapper.findAll('button.audience-result')
    await results[0]!.trigger('click')
    expect(wrapper.emitted('update:recipientUserIds')?.[0]).toEqual([[2]])
    expect(wrapper.text()).toContain('Nguyễn Văn A')

    const removeButton = wrapper.find('button[aria-label="Bỏ chọn Nguyễn Văn A"]')
    await removeButton.trigger('click')
    expect(wrapper.emitted('update:recipientUserIds')?.[1]).toEqual([[]])
  })

  it('renders one checkbox per result without an extra checkmark and emits toggled recipient IDs', async () => {
    mocks.fetchIndividuals.mockResolvedValue({
      result: [
        { userId: 2, displayName: 'Nguyễn Văn A', studentCode: 'HS001', username: 'nguyenvana' },
        { userId: 5, displayName: 'Trần Thị B', studentCode: 'HS002', username: 'tranthib' },
      ],
      meta: { page: 0, pageSize: 20, totalPages: 1, totalItems: 2 },
    })

    const wrapper = mountSelector({ audienceType: 'INDIVIDUAL', recipientUserIds: [] })
    await flushPromises()

    const results = wrapper.findAll('button.audience-result')
    expect(results).toHaveLength(2)
    expect(results[0]!.findAll('input[type="checkbox"]')).toHaveLength(1)
    expect(results[1]!.findAll('input[type="checkbox"]')).toHaveLength(1)
    expect(results[0]!.classes()).not.toContain('audience-result-selected')
    expect(results[0]!.attributes('aria-selected')).toBe('false')
    expect(wrapper.text()).not.toContain('✓')

    await results[0]!.trigger('click')
    expect(wrapper.emitted('update:recipientUserIds')?.[0]).toEqual([[2]])

    await wrapper.setProps({ recipientUserIds: [2] })
    await flushPromises()
    expect(results[0]!.find('input[type="checkbox"]').element).toHaveProperty('checked', true)
    expect(results[0]!.classes()).toContain('audience-result-selected')
    expect(results[0]!.attributes('aria-selected')).toBe('true')
    expect(results[1]!.find('input[type="checkbox"]').element).toHaveProperty('checked', false)

    await results[0]!.trigger('click')
    expect(wrapper.emitted('update:recipientUserIds')?.[1]).toEqual([[]])

    await wrapper.setProps({ recipientUserIds: [] })
    await results[1]!.trigger('click')
    expect(wrapper.emitted('update:recipientUserIds')?.[2]).toEqual([[5]])
  })
})
