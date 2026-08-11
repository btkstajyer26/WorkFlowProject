import { http, HttpResponse } from 'msw'
import { apiBaseUrl } from '../../../api/config'
import { getAuthenticatedMockUser } from '../auth'
import { mockApiDb } from '../db'
import { apiErrorResponse, forbiddenResponse, unauthorizedResponse } from '../responses'

function unreadNotificationsFor(userId: string) {
  return mockApiDb.notifications
    .filter((notification) => notification.userId === userId && !notification.read)
    .sort((left, right) => right.createdAt.localeCompare(left.createdAt))
    .map(({ userId: _userId, ...notification }) => notification)
}

export const notificationHandlers = [
  http.get(`${apiBaseUrl}/api/notifications/unread/count`, ({ request }) => {
    const user = getAuthenticatedMockUser(request)
    if (!user) return unauthorizedResponse()
    return HttpResponse.json(unreadNotificationsFor(user.id).length)
  }),

  http.get(`${apiBaseUrl}/api/notifications/unread`, ({ request }) => {
    const user = getAuthenticatedMockUser(request)
    if (!user) return unauthorizedResponse()
    return HttpResponse.json(unreadNotificationsFor(user.id))
  }),

  http.put(`${apiBaseUrl}/api/notifications/:id/read`, ({ params, request }) => {
    const user = getAuthenticatedMockUser(request)
    if (!user) return unauthorizedResponse()

    const notification = mockApiDb.notifications.find((item) => item.id === params.id)
    if (!notification) {
      return apiErrorResponse(404, 'RESOURCE_NOT_FOUND', `Bildirim bulunamadı: ${params.id}`)
    }
    if (notification.userId !== user.id) {
      return forbiddenResponse('Bu bildirim üzerinde işlem yapma yetkiniz yok')
    }

    mockApiDb.notifications = mockApiDb.notifications.map((item) => (
      item.id === notification.id ? { ...item, read: true } : item
    ))
    return new HttpResponse(null, { status: 204 })
  }),
]
