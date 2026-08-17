import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { QueryClientProvider } from '@tanstack/react-query'
import { NotificationsPage } from './NotificationsPage'
import type { NotificationItem } from '../types/notification'
import { createAppQueryClient } from '../query/createQueryClient'

const notifications: NotificationItem[] = [
  {
    id: 'notification-unread',
    userId: 'user-demo-001',
    recordId: 'rec-001',
    message: 'İncelemeniz gereken yeni bir kayıt var.',
    isRead: false,
    createdAt: '2026-08-05T10:00:00.000Z',
  },
  {
    id: 'notification-read',
    userId: 'user-demo-001',
    recordId: 'rec-002',
    message: 'Kaydınız sonuçlandırıldı.',
    isRead: true,
    createdAt: '2026-08-04T10:00:00.000Z',
  },
]

function renderNotificationsPage(
  onMarkRead: (notificationId: string) => void,
) {
  const queryClient = createAppQueryClient()
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <NotificationsPage notifications={notifications} onMarkRead={onMarkRead} />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('NotificationsPage', () => {
  it('tek bildirimi okundu yapma aksiyonunu iletir', async () => {
    const user = userEvent.setup()
    const onMarkRead = vi.fn()

    renderNotificationsPage(onMarkRead)

    await user.click(screen.getByRole('button', { name: 'Okundu yap' }))
    expect(onMarkRead).toHaveBeenCalledWith('notification-unread')
    expect(screen.queryByRole('button', { name: 'Tümünü okundu yap' })).not.toBeInTheDocument()
  })

  it('Tümü ve Okunmamış görünümleri arasında geçiş yapar', async () => {
    const user = userEvent.setup()
    renderNotificationsPage(() => undefined)

    expect(screen.getByText('İncelemeniz gereken yeni bir kayıt var.')).toBeInTheDocument()
    expect(screen.getByText('Kaydınız sonuçlandırıldı.')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Okunmamış/ }))
    expect(screen.getByText('İncelemeniz gereken yeni bir kayıt var.')).toBeInTheDocument()
    expect(screen.queryByText('Kaydınız sonuçlandırıldı.')).not.toBeInTheDocument()
  })
})
