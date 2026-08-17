import { QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { describe, expect, it } from 'vitest'
import { api, setApiAccessToken } from '../api/client'
import { createAppQueryClient } from '../query/createQueryClient'
import { useNotificationCenter } from './useNotificationCenter'

async function loginAsEmployee() {
  const session = await api.auth.login({
    email: 'john.doe@kurum.gov.tr',
    password: 'demo123',
  })
  setApiAccessToken(session.accessToken!)
}

function createWrapper() {
  const queryClient = createAppQueryClient()
  return function QueryWrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  }
}

describe('useNotificationCenter', () => {
  it('okunmamış bildirimleri API’den yükler ve okundu işleminden sonra cacheleri yeniler', async () => {
    await loginAsEmployee()
    const { result } = renderHook(
      () => useNotificationCenter({ enabled: true, unreadOnly: true }),
      { wrapper: createWrapper() },
    )

    await waitFor(() => expect(result.current.isPending).toBe(false))
    expect(result.current.unreadCount).toBe(1)
    expect(result.current.notifications).toEqual([
      expect.objectContaining({
        id: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb1',
        isRead: false,
      }),
    ])

    act(() => {
      result.current.markRead('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb1')
    })

    await waitFor(() => expect(result.current.unreadCount).toBe(0))
    await waitFor(() => expect(result.current.notifications).toEqual([]))
  })
})
