import type { NotificationResponse, PagedResponseNotificationResponse } from './generated/data-contracts'
import { api, apiHttpClient } from './client'
import { ApiClientError } from './errors'

export type NotificationListItem = Required<Pick<
  NotificationResponse,
  'id' | 'recordId' | 'message' | 'notificationType' | 'read' | 'createdAt'
>>

export type UnreadNotification = Omit<NotificationListItem, 'read'> & { read: false }

export type NotificationListQuery = {
  page?: number
  size?: number
  sort?: string[]
}

export type NotificationListResult = {
  content: NotificationListItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

function invalidNotificationResponse(message: string): never {
  throw new ApiClientError({
    code: 'INVALID_NOTIFICATION_RESPONSE',
    message,
    status: 0,
  })
}

function normalizeNotification(notification: NotificationResponse): NotificationListItem {
  if (
    !notification.id ||
    !notification.recordId ||
    !notification.message?.trim() ||
    !notification.notificationType ||
    !notification.createdAt ||
    typeof notification.read !== 'boolean'
  ) {
    return invalidNotificationResponse('Sunucu geçerli bildirim bilgisi döndürmedi.')
  }

  return {
    id: notification.id,
    recordId: notification.recordId,
    message: notification.message.trim(),
    notificationType: notification.notificationType,
    read: notification.read,
    createdAt: notification.createdAt,
  }
}

function requirePageNumber(value: number | undefined, field: string, minimum = 0) {
  if (!Number.isSafeInteger(value) || value! < minimum) {
    return invalidNotificationResponse(`Sunucu geçerli ${field} bilgisi döndürmedi.`)
  }
  return value!
}

export async function listNotifications({
  page = 0,
  size = 20,
  sort = ['createdAt,desc'],
}: NotificationListQuery = {}): Promise<NotificationListResult> {
  // Spring Pageable query parametreleri OpenAPI'de tek `pageable` nesnesi
  // olarak üretildiği için mevcut liste adapter'larıyla aynı düzleştirme yapılır.
  const response = await apiHttpClient.request<PagedResponseNotificationResponse>({
    path: '/api/notifications',
    method: 'GET',
    query: { page, size, sort },
    secure: true,
  })

  return {
    content: (response.content ?? []).map(normalizeNotification),
    page: requirePageNumber(response.page, 'sayfa'),
    size: requirePageNumber(response.size, 'sayfa boyutu', 1),
    totalElements: requirePageNumber(response.totalElements, 'toplam bildirim sayısı'),
    totalPages: requirePageNumber(response.totalPages, 'toplam sayfa sayısı'),
  }
}

export async function listUnreadNotifications() {
  const notifications = await api.notifications.getUnread()
  const normalized = notifications.map(normalizeNotification)
  if (normalized.some((notification) => notification.read)) {
    return invalidNotificationResponse('Sunucu okunmamış bildirim listesinde okunmuş bildirim döndürdü.')
  }
  return normalized.map((notification): UnreadNotification => ({
    ...notification,
    read: false,
  }))
}

export async function getUnreadNotificationCount() {
  const count = await api.notifications.countUnread()
  if (!Number.isSafeInteger(count) || count < 0) {
    return invalidNotificationResponse('Sunucu geçerli okunmamış bildirim sayısı döndürmedi.')
  }
  return count
}

export async function markNotificationAsRead(notificationId: string) {
  await api.notifications.markAsRead({ id: notificationId })
}
