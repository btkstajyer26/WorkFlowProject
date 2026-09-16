import { Client, type IFrame, type IMessage } from '@stomp/stompjs'
import { QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { clearApiAccessToken, setApiAccessToken } from '../api/client'
import { createAppQueryClient } from '../query/createQueryClient'
import { queryKeys } from '../query/queryKeys'
import { useRealtimeNotifications } from './useRealtimeNotifications'

vi.mock('@stomp/stompjs')

function mount(enabled = true, strict = false) {
  const queryClient = createAppQueryClient()
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  return {
    queryClient,
    ...renderHook(({ enabled }) => useRealtimeNotifications(enabled), {
      initialProps: { enabled }, wrapper, reactStrictMode: strict,
    }),
  }
}

function latestClient() {
  return vi.mocked(Client).mock.instances.at(-1)!
}

async function connect(client: Client) {
  await client.beforeConnect(client)
  client.onConnect({} as IFrame)
}

beforeEach(() => {
  vi.mocked(Client).mockClear()
})

describe('useRealtimeNotifications', () => {
  it('public uygulamada veya access token yokken bağlantı başlatmaz', () => {
    const { rerender } = mount(false)
    expect(Client).not.toHaveBeenCalled()
    rerender({ enabled: true })
    expect(latestClient().activate).not.toHaveBeenCalled()
    act(() => setApiAccessToken('access-token'))
    expect(latestClient().activate).toHaveBeenCalledOnce()
  })

  it('CONNECT ve reconnect güncel tokenı kullanır, private destination abonesi olur', async () => {
    setApiAccessToken('first-token')
    mount()
    const client = latestClient()
    await connect(client)
    expect(client.connectHeaders).toEqual({ Authorization: 'Bearer first-token' })
    expect(client.subscribe).toHaveBeenCalledWith('/user/queue/notifications', expect.any(Function))

    act(() => setApiAccessToken('refreshed-token'))
    await client.beforeConnect(client)
    expect(client.connectHeaders).toEqual({ Authorization: 'Bearer refreshed-token' })
    expect(Client).toHaveBeenCalledOnce()
  })

  it('bildirim cachelerini ve yalnızca ilgili kayıt detayını/listelerini yeniler', async () => {
    setApiAccessToken('access-token')
    const { queryClient } = mount()
    const affected = [
      queryKeys.notifications.list(), queryKeys.notifications.unread,
      queryKeys.notifications.unreadCount, queryKeys.records.detail('record-1', 'revision'),
      queryKeys.records.list({ page: 0 }),
    ]
    const unaffected = [queryKeys.records.detail('record-2'), queryKeys.categories]
    for (const key of [...affected, ...unaffected]) queryClient.setQueryData(key, [])
    const client = latestClient()
    await connect(client)
    const receive = vi.mocked(client.subscribe).mock.calls[0][1]
    act(() => receive({ body: JSON.stringify({ recordId: 'record-1' }) } as IMessage))
    for (const key of affected) expect(queryClient.getQueryState(key)?.isInvalidated).toBe(true)
    for (const key of unaffected) expect(queryClient.getQueryState(key)?.isInvalidated).toBe(false)
  })

  it.each(['invalid-json', 'null', '{}', '{"recordId":42}'])('bozuk payload polling/cache yenilemeyi engellemez: %s', async (body) => {
    setApiAccessToken('access-token')
    const { queryClient } = mount()
    queryClient.setQueryData(queryKeys.notifications.unreadCount, 1)
    queryClient.setQueryData(queryKeys.records.list({}), [])
    const client = latestClient()
    await connect(client)
    act(() => vi.mocked(client.subscribe).mock.calls[0][1]({ body } as IMessage))
    expect(queryClient.getQueryState(queryKeys.notifications.unreadCount)?.isInvalidated).toBe(true)
    expect(queryClient.getQueryState(queryKeys.records.list({}))?.isInvalidated).toBe(false)
  })

  it('token temizlenince bağlantıyı kapatır ve geç gelen mesajları yok sayar', async () => {
    setApiAccessToken('access-token')
    const { queryClient } = mount()
    const client = latestClient()
    await connect(client)
    const receive = vi.mocked(client.subscribe).mock.calls[0][1]
    queryClient.setQueryData(queryKeys.notifications.unreadCount, 1)
    act(() => clearApiAccessToken())
    expect(client.deactivate).toHaveBeenCalledWith()
    await client.beforeConnect(client)
    expect(client.connectHeaders).toEqual({})
    act(() => receive({ body: '{}' } as IMessage))
    expect(queryClient.getQueryState(queryKeys.notifications.unreadCount)?.isInvalidated).toBe(false)
  })

  it('StrictMode eski clientı kapatır; unmount token aboneliğini de kaldırır', async () => {
    setApiAccessToken('access-token')
    const { unmount } = mount(true, true)
    const [oldClient, client] = vi.mocked(Client).mock.instances
    expect(oldClient.deactivate).toHaveBeenCalledWith()
    await oldClient.beforeConnect(oldClient)
    expect(oldClient.connectHeaders).toEqual({})
    await connect(client)
    unmount()
    expect(client.deactivate).toHaveBeenCalledWith()
    const activationCount = vi.mocked(client.activate).mock.calls.length
    act(() => setApiAccessToken('later-token'))
    expect(client.activate).toHaveBeenCalledTimes(activationCount)
  })
})
