import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import ButtonStub from '@/test/stubs/ButtonStub.vue'
import NotificationInboxView from '@/views/notification/NotificationInboxView.vue'
import type { NotificationPage } from '@/types/notification'
import {
  cancelNotification,
  createNotificationDraft,
  fetchManagedNotifications,
  fetchNotification,
  fetchNotificationInbox,
  markNotificationRead,
  publishNotification,
} from './notificationApi'

const fetchMock = vi.fn()
const routerPush = vi.fn()
const authSessionMock = {
  requireAccessToken: vi.fn(() => 'test-token'),
  roles: { value: [] },
}

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush, back: vi.fn() }),
  useRoute: () => ({ params: { notificationId: '1' } }),
}))

vi.mock('@/composables/useAuthSession', () => ({
  useAuthSession: () => authSessionMock,
}))

describe('notificationApi', () => {
  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('calls fetchNotificationInbox with correct query parameters', async () => {
    fetchMock.mockImplementation(() =>
      Promise.resolve(new Response(JSON.stringify({ data: { meta: {}, result: [] } }), { status: 200 })),
    )
    vi.stubGlobal('fetch', fetchMock)

    await fetchNotificationInbox('test-token', { page: 0, pageSize: 10, unreadOnly: true })

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toContain('/api/v3/notifications/inbox?page=0&size=10&unreadOnly=true')
    expect(options.headers.Authorization).toBe('Bearer test-token')
  })

  it('preserves canonical ResultPaginationDTO metadata from the v3 response', async () => {
    const page: NotificationPage = {
      meta: {
        page: 0,
        pageSize: 20,
        totalPages: 2,
        totalItems: 21,
      },
      result: [],
    }
    fetchMock.mockImplementation(() =>
      Promise.resolve(new Response(JSON.stringify({ data: page }), { status: 200 })),
    )
    vi.stubGlobal('fetch', fetchMock)

    const result = await fetchNotificationInbox('test-token')

    expect(result.meta).toEqual({
      page: 0,
      pageSize: 20,
      totalPages: 2,
      totalItems: 21,
    })
    expect(result.meta).not.toHaveProperty('pages')
    expect(result.meta).not.toHaveProperty('total')
  })

  it('calls fetchManagedNotifications with correct query parameters', async () => {
    fetchMock.mockImplementation(() =>
      Promise.resolve(new Response(JSON.stringify({ data: { meta: {}, result: [] } }), { status: 200 })),
    )
    vi.stubGlobal('fetch', fetchMock)

    await fetchManagedNotifications('test-token', { page: 1, pageSize: 20 })

    const [url] = fetchMock.mock.calls[0]
    expect(url).toContain('/api/v3/notifications/manage?page=1&size=20')
  })

  it('returns the canonical ResultPaginationDTO metadata without legacy aliases', async () => {
    const meta = { page: 0, pageSize: 20, totalPages: 3, totalItems: 41 }
    fetchMock.mockImplementation(() =>
      Promise.resolve(new Response(JSON.stringify({ data: { meta, result: [] } }), { status: 200 })),
    )
    vi.stubGlobal('fetch', fetchMock)

    const response = await fetchNotificationInbox('test-token', { page: 0, pageSize: 20 })

    expect(response.meta).toEqual(meta)
    expect(response.meta).not.toHaveProperty('pages')
    expect(response.meta).not.toHaveProperty('total')
  })

  it('keeps notification pagination zero-based and drives the inbox next-page control', async () => {
    fetchMock
      .mockImplementationOnce(() => Promise.resolve(new Response(JSON.stringify({
        data: {
          meta: { page: 0, pageSize: 20, totalPages: 2, totalItems: 21 },
          result: [],
        },
      }), { status: 200 })))
      .mockImplementationOnce(() => Promise.resolve(new Response(JSON.stringify({
        data: {
          meta: { page: 1, pageSize: 20, totalPages: 2, totalItems: 21 },
          result: [],
        },
      }), { status: 200 })))
    vi.stubGlobal('fetch', fetchMock)

    const wrapper = mount(NotificationInboxView, {
      global: {
        stubs: {
          Button: ButtonStub,
          NotificationList: true,
        },
      },
    })
    await flushPromises()

    expect(fetchMock.mock.calls[0]?.[0]).toContain('page=0&size=20')
    expect(wrapper.text()).toContain('Trang 1 / 2')

    const nextButton = wrapper.findAllComponents(ButtonStub).find((button) => button.text().includes('Trang sau'))
    expect(nextButton).toBeDefined()
    await nextButton!.trigger('click')
    await flushPromises()

    expect(fetchMock.mock.calls[1]?.[0]).toContain('page=1&size=20')
    expect(wrapper.text()).toContain('Trang 2 / 2')
  })

  it('calls fetchNotification with correct path', async () => {
    fetchMock.mockImplementation(() =>
      Promise.resolve(new Response(JSON.stringify({ data: { id: 42 } }), { status: 200 })),
    )
    vi.stubGlobal('fetch', fetchMock)

    await fetchNotification('test-token', 42)

    const [url] = fetchMock.mock.calls[0]
    expect(url).toContain('/api/v3/notifications/42')
  })

  it('serializes caller-provided school scope and idempotency key unchanged', async () => {
    fetchMock.mockImplementation(() =>
      Promise.resolve(new Response(JSON.stringify({ data: { id: 1, title: 'Draft' } }), { status: 201 })),
    )
    vi.stubGlobal('fetch', fetchMock)

    const payload = {
      title: 'Họp phụ huynh',
      body: 'Nội dung thông báo',
      audienceType: 'CLASS' as const,
      targetReference: '101',
      schoolScope: 'caller-provided-scope',
      idempotencyKey: 'notification-intent-001',
    }
    await createNotificationDraft('test-token', payload)

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toContain('/api/v3/notifications')
    expect(options.method).toBe('POST')
    expect(JSON.parse(options.body)).toEqual(payload)
  })

  it('calls publishNotification with expectedVersion', async () => {
    fetchMock.mockImplementation(() =>
      Promise.resolve(new Response(JSON.stringify({ data: { id: 1, status: 'PUBLISHED' } }), { status: 200 })),
    )
    vi.stubGlobal('fetch', fetchMock)

    await publishNotification('test-token', 1, { expectedVersion: 0 })

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toContain('/api/v3/notifications/1/publish')
    expect(options.method).toBe('POST')
    expect(JSON.parse(options.body)).toEqual({ expectedVersion: 0 })
  })

  it('calls cancelNotification endpoint', async () => {
    fetchMock.mockImplementation(() =>
      Promise.resolve(new Response(JSON.stringify({ data: { id: 1, status: 'CANCELLED' } }), { status: 200 })),
    )
    vi.stubGlobal('fetch', fetchMock)

    await cancelNotification('test-token', 1)

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toContain('/api/v3/notifications/1/cancel')
    expect(options.method).toBe('POST')
  })

  it('calls markNotificationRead endpoint', async () => {
    fetchMock.mockImplementation(() =>
      Promise.resolve(new Response(JSON.stringify({ data: { id: 9, readAt: '2026-09-15T12:00:00' } }), { status: 200 })),
    )
    vi.stubGlobal('fetch', fetchMock)

    await markNotificationRead('test-token', 1)

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toContain('/api/v3/notifications/1/read')
    expect(options.method).toBe('POST')
  })
})
