import { apiClient } from '@/services/apiClient'
import { apiBaseUrl } from '@/services/apiConfig'
import type {
  NotificationInboxQuery,
  NotificationAudienceLookupQuery,
  NotificationClassAudience,
  NotificationIndividualAudience,
  NotificationItem,
  NotificationManageQuery,
  NotificationPage,
  NotificationReceipt,
  ReqCreateNotificationDTO,
  ReqPublishNotificationDTO,
  ResultPaginationDTO,
} from '@/types/notification'

export const NOTIFICATION_READ_EVENT = 'notification:read'
export const NOTIFICATION_INBOX_CHANGED_EVENT = 'notification:inbox-changed'

const EVENT_STREAM_PATH = '/api/v3/notifications/events'
const MAX_RECONNECT_DELAY_MS = 30000

export function startNotificationEventStream(token: string, onInboxChanged: () => void): () => void {
  let stopped = false
  let activeController: AbortController | undefined
  let reconnectDelayMs = 1000
  let reconnectTimer: number | undefined
  let resumeReconnect: (() => void) | undefined

  const waitBeforeReconnect = () => new Promise<void>((resolve) => {
    if (stopped) {
      resolve()
      return
    }
    resumeReconnect = resolve
    const scheduleTimer = typeof window !== 'undefined' ? window.setTimeout.bind(window) : setTimeout
    reconnectTimer = scheduleTimer(() => {
      reconnectTimer = undefined
      resumeReconnect = undefined
      resolve()
    }, reconnectDelayMs) as unknown as number
  })

  const readEvents = async (): Promise<void> => {
    while (!stopped) {
      activeController = new AbortController()
      let connectedAt = 0
      try {
        const response = await fetch(`${apiBaseUrl.replace(/\/$/, '')}${EVENT_STREAM_PATH}`, {
          method: 'GET',
          headers: {
            Accept: 'text/event-stream',
            Authorization: `Bearer ${token}`,
          },
          signal: activeController.signal,
        })
        if (stopped || activeController.signal.aborted) return
        if (response.status === 401 || response.status === 403) return
        if (!response.ok || !response.body) throw new Error('Notification event stream unavailable')

        // A publish can occur between the initial REST snapshot and SSE subscription.
        // Refresh on every connection because the server does not replay missed events.
        onInboxChanged()
        connectedAt = Date.now()
        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        try {
          while (!stopped) {
            const { done, value } = await reader.read()
            if (done || stopped) break
            buffer += decoder.decode(value, { stream: true })
            const frames = buffer.split(/\r?\n\r?\n/)
            buffer = frames.pop() ?? ''
            frames.forEach((frame) => {
              if (frame.split(/\r?\n/).some((line) => /^event:\s*inbox-changed$/.test(line))) {
                onInboxChanged()
              }
            })
          }
        } finally {
          reader.releaseLock?.()
        }
      } catch {
        if (stopped || activeController?.signal.aborted) return
      }
      if (stopped) return
      if (connectedAt > 0 && Date.now() - connectedAt >= MAX_RECONNECT_DELAY_MS) reconnectDelayMs = 1000
      await waitBeforeReconnect()
      if (stopped) return
      reconnectDelayMs = Math.min(reconnectDelayMs * 2, MAX_RECONNECT_DELAY_MS)
    }
  }

  void readEvents().catch(() => {})
  return () => {
    if (stopped) return
    stopped = true
    activeController?.abort()
    activeController = undefined
    if (reconnectTimer !== undefined) {
      const cancelTimer = typeof window !== 'undefined' ? window.clearTimeout.bind(window) : clearTimeout
      cancelTimer(reconnectTimer)
      reconnectTimer = undefined
    }
    resumeReconnect?.()
    resumeReconnect = undefined
  }
}

function notificationPage(response: NotificationPage): NotificationPage {
  return {
    result: response.result,
    meta: {
      page: response.meta.page,
      pageSize: response.meta.pageSize,
      totalPages: response.meta.totalPages,
      totalItems: response.meta.totalItems,
    },
  }
}

function paginationParams(page: number | undefined, pageSize: number | undefined): URLSearchParams {
  const params = new URLSearchParams()
  if (page !== undefined) params.set('page', String(page))
  // Spring Pageable names the request-size parameter `size`; the response contract calls it `pageSize`.
  if (pageSize !== undefined) params.set('size', String(pageSize))
  return params
}

function audienceLookupParams(query: NotificationAudienceLookupQuery = {}): URLSearchParams {
  const params = paginationParams(query.page, query.pageSize)
  const search = query.q?.trim()
  if (search) params.set('q', search)
  if (query.roleCode) params.set('roleCode', query.roleCode)
  if (query.studentClassId != null) params.set('studentClassId', String(query.studentClassId))
  if (query.teacherClassId != null) params.set('teacherClassId', String(query.teacherClassId))
  return params
}

function audiencePage<T>(response: ResultPaginationDTO<T>): ResultPaginationDTO<T> {
  return {
    result: response.result,
    meta: {
      page: response.meta.page,
      pageSize: response.meta.pageSize,
      totalPages: response.meta.totalPages,
      totalItems: response.meta.totalItems,
    },
  }
}

export function fetchNotificationClassAudiences(
  token: string,
  query: NotificationAudienceLookupQuery = {},
): Promise<ResultPaginationDTO<NotificationClassAudience>> {
  return apiClient.get<ResultPaginationDTO<NotificationClassAudience>>(
    '/api/v3/notifications/audiences/classes',
    { token, query: audienceLookupParams(query) },
  ).then(audiencePage)
}

export function fetchNotificationIndividualAudiences(
  token: string,
  query: NotificationAudienceLookupQuery = {},
): Promise<ResultPaginationDTO<NotificationIndividualAudience>> {
  return apiClient.get<ResultPaginationDTO<NotificationIndividualAudience>>(
    '/api/v3/notifications/audiences/individuals',
    { token, query: audienceLookupParams(query) },
  ).then(audiencePage)
}

export function fetchNotificationInbox(
  token: string,
  query: NotificationInboxQuery = {},
): Promise<NotificationPage> {
  const params = paginationParams(query.page, query.pageSize)
  if (query.unreadOnly !== undefined) params.set('unreadOnly', String(query.unreadOnly))

  return apiClient.get<NotificationPage>('/api/v3/notifications/inbox', {
    token,
    query: params,
  }).then(notificationPage)
}

export function fetchUnreadNotificationCount(token: string): Promise<number> {
  const params = paginationParams(0, 1)
  params.set('unreadOnly', 'true')

  return apiClient.get<NotificationPage>('/api/v3/notifications/inbox', {
    token,
    query: params,
    cache: 'no-store',
  }).then(notificationPage).then((response) => Math.max(response.meta.totalItems, 0))
}

export function fetchManagedNotifications(
  token: string,
  query: NotificationManageQuery = {},
): Promise<NotificationPage> {
  const params = paginationParams(query.page, query.pageSize)

  return apiClient.get<NotificationPage>('/api/v3/notifications/manage', {
    token,
    query: params,
  }).then(notificationPage)
}

export function fetchNotification(
  token: string,
  notificationId: number,
): Promise<NotificationItem> {
  return apiClient.get<NotificationItem>(`/api/v3/notifications/${notificationId}`, {
    token,
  })
}

export function createNotificationDraft(
  token: string,
  request: ReqCreateNotificationDTO,
): Promise<NotificationItem> {
  return apiClient.post<NotificationItem>('/api/v3/notifications', request, {
    token,
  })
}

export function publishNotification(
  token: string,
  notificationId: number,
  request?: ReqPublishNotificationDTO,
): Promise<NotificationItem> {
  return apiClient.post<NotificationItem>(
    `/api/v3/notifications/${notificationId}/publish`,
    request ?? {},
    { token },
  )
}

export function cancelNotification(
  token: string,
  notificationId: number,
): Promise<NotificationItem> {
  return apiClient.post<NotificationItem>(
    `/api/v3/notifications/${notificationId}/cancel`,
    {},
    { token },
  )
}

export function markNotificationRead(
  token: string,
  notificationId: number,
): Promise<NotificationReceipt> {
  return apiClient.post<NotificationReceipt>(
    `/api/v3/notifications/${notificationId}/read`,
    {},
    { token },
  ).then((receipt) => {
    if (typeof window !== 'undefined') window.dispatchEvent(new Event(NOTIFICATION_READ_EVENT))
    return receipt
  })
}
