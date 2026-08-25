import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import { apiBaseUrl } from '../api/config'
import { setApiAccessToken } from '../api/client'
import { apiMockServer } from '../mocks/api/server'
import { createAppQueryClient } from '../query/createQueryClient'
import { NotificationsPage } from './NotificationsPage'

const notifications = [
  {
    id: 'notification-unread',
    recordId: 'rec-001',
    message: 'İncelemeniz gereken yeni bir kayıt var.',
    notificationType: 'RECORD_SUBMITTED',
    read: false,
    createdAt: '2026-08-05T10:00:00.000Z',
  },
  {
    id: 'notification-read',
    recordId: 'rec-002',
    message: 'Kaydınız sonuçlandırıldı.',
    notificationType: 'RECORD_APPROVED',
    read: true,
    createdAt: '2026-08-04T10:00:00.000Z',
  },
]

function installNotificationHandlers() {
  const state = notifications.map((notification) => ({ ...notification }))
  apiMockServer.use(
    http.get(`${apiBaseUrl}/api/notifications`, () => HttpResponse.json({
      content: state,
      page: 0,
      size: 20,
      totalElements: state.length,
      totalPages: 1,
    })),
    http.get(`${apiBaseUrl}/api/notifications/unread/count`, () => HttpResponse.json(state.filter((item) => !item.read).length)),
    http.get(`${apiBaseUrl}/api/notifications/unread`, () => HttpResponse.json(state.filter((item) => !item.read))),
    http.put(`${apiBaseUrl}/api/notifications/:id/read`, ({ params }) => {
      const notification = state.find((item) => item.id === params.id)
      if (notification) notification.read = true
      return new HttpResponse(null, { status: 204 })
    }),
  )
}

function renderNotificationsPage() {
  setApiAccessToken('notification-test-token')
  const queryClient = createAppQueryClient()
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('NotificationsPage', () => {
  it('tek bildirimi backend üzerinden okundu yapar', async () => {
    installNotificationHandlers()
    const user = userEvent.setup()
    renderNotificationsPage()

    const markReadButton = await screen.findByRole('button', { name: 'Okundu yap' })
    await user.click(markReadButton)
    await waitFor(() => expect(markReadButton).not.toBeInTheDocument())
  })

  it('Tümü ve Okunmamış görünümleri arasında geçiş yapar', async () => {
    installNotificationHandlers()
    const user = userEvent.setup()
    renderNotificationsPage()

    expect(await screen.findByText('İncelemeniz gereken yeni bir kayıt var.')).toBeInTheDocument()
    expect(screen.getByText('Kaydınız sonuçlandırıldı.')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Okunmamış/ }))
    expect(screen.getByText('İncelemeniz gereken yeni bir kayıt var.')).toBeInTheDocument()
    expect(screen.queryByText('Kaydınız sonuçlandırıldı.')).not.toBeInTheDocument()
  })
})
