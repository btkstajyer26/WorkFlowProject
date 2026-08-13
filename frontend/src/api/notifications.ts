import type { NotificationResponse } from './generated/data-contracts'
import { api } from './client'
import { ApiClientError } from './errors'

export type UnreadNotification = Required<Pick<
  NotificationResponse,
  'id' | 'recordId' | 'message' | 'notificationType' | 'read' | 'createdAt'
>>

function invalidNotificationResponse(message: string): never {
  throw new ApiClientError({
    code: 'INVALID_NOTIFICATION_RESPONSE',
    message,
    status: 0,
  })
}

function normalizeUnreadNotification(notification: NotificationResponse): UnreadNotification {
  if (
    !notification.id ||
    !notification.recordId ||
    !notification.message?.trim() ||
    !notification.notificationType ||
    !notification.createdAt ||
    notification.read !== false
  ) {
    return invalidNotificationResponse('Sunucu geçerli okunmamış bildirim bilgisi döndürmedi.')
  }

  return {
    id: notification.id,
    recordId: notification.recordId,
    message: notification.message.trim(),
    notificationType: notification.notificationType,
    read: false,
    createdAt: notification.createdAt,
  }
}

export async function listUnreadNotifications() {
  const notifications = await api.notifications.getUnread()
  return notifications.map(normalizeUnreadNotification)
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
