import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { NotificationsPage } from './NotificationsPage'
import type { NotificationItem } from '../types/notification'

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

describe('NotificationsPage', () => {
  it('tek bildirimi ve tüm bildirimleri okundu yapma aksiyonlarını iletir', async () => {
    const user = userEvent.setup()
    const onMarkRead = vi.fn()
    const onMarkAllRead = vi.fn()

    render(
      <MemoryRouter>
        <NotificationsPage
          notifications={notifications}
          onMarkRead={onMarkRead}
          onMarkAllRead={onMarkAllRead}
        />
      </MemoryRouter>,
    )

    await user.click(screen.getByRole('button', { name: 'Okundu yap' }))
    expect(onMarkRead).toHaveBeenCalledWith('notification-unread')

    await user.click(screen.getByRole('button', { name: 'Tümünü okundu yap' }))
    expect(onMarkAllRead).toHaveBeenCalledOnce()
  })

  it('okunmamış filtresinde yalnız okunmamış bildirimi gösterir', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <NotificationsPage notifications={notifications} onMarkRead={() => undefined} onMarkAllRead={() => undefined} />
      </MemoryRouter>,
    )

    await user.click(screen.getByRole('button', { name: /Okunmamış/ }))
    expect(screen.getByText('İncelemeniz gereken yeni bir kayıt var.')).toBeInTheDocument()
    expect(screen.queryByText('Kaydınız sonuçlandırıldı.')).not.toBeInTheDocument()
  })
})
