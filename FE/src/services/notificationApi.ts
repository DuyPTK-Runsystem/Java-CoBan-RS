import { apiClient } from '@/services/apiClient'
import type {
  NotificationInboxQuery,
  NotificationItem,
  NotificationManageQuery,
  NotificationPage,
  NotificationReceipt,
  ReqCreateNotificationDTO,
  ReqPublishNotificationDTO,
} from '@/types/notification'

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
  )
}
