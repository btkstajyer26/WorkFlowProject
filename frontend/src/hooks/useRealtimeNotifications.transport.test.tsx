import { QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { setApiAccessToken } from '../api/client'
import { createAppQueryClient } from '../query/createQueryClient'
import { useRealtimeNotifications } from './useRealtimeNotifications'

vi.unmock('@stomp/stompjs')

// Real STOMP client/parser; only the network is controlled. close() deliberately
// does not finish until finishClose(), as with an asynchronous browser socket.
class Socket {
  static instances: Socket[] = []
  readyState = 0
  binaryType = 'arraybuffer'
  onopen?: (event: object) => void
  onmessage?: (event: { data: string }) => void
  onclose?: (event: object) => void
  onerror?: (event: object) => void
  frames: string[] = []
  constructor() { Socket.instances.push(this) }
  send(frame: string) {
    this.frames.push(frame)
    if (frame.startsWith('DISCONNECT')) {
      const receipt = frame.match(/\nreceipt:([^\n]+)/)?.[1]
      queueMicrotask(() => this.receive(`RECEIPT\nreceipt-id:${receipt}\n\n\0`))
    }
  }
  close() { this.readyState = 2 }
  finishClose() {
    this.readyState = 3
    this.onclose?.({ code: 1000 })
  }
  receive(data: string) { this.onmessage?.({ data }) }
  connect() {
    this.readyState = 1
    this.onopen?.({})
    this.receive('CONNECTED\nversion:1.2\nheart-beat:0,0\n\n\0')
  }
  get subscriptions() { return this.frames.filter(frame => frame.startsWith('SUBSCRIBE')) }
  notify(id: string) {
    const subscription = this.subscriptions[0].match(/\nid:([^\n]+)/)![1]
    this.receive(`MESSAGE\nsubscription:${subscription}\nmessage-id:${id}\n\n${JSON.stringify({ id, recordId: 'record-1' })}\0`)
  }
}

async function flush() { await act(async () => { await vi.advanceTimersByTimeAsync(0) }) }

function mount(strict = false) {
  const queryClient = createAppQueryClient()
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  return { queryClient, ...renderHook(() => useRealtimeNotifications(true), { wrapper, reactStrictMode: strict }) }
}

beforeEach(() => {
  vi.useFakeTimers()
  vi.stubGlobal('WebSocket', Socket)
  Socket.instances = []
  setApiAccessToken('test-token')
})

afterEach(async () => {
  for (const socket of Socket.instances) socket.finishClose()
  await flush()
  vi.unstubAllGlobals()
  vi.useRealTimers()
})

describe('notification transport lifecycle', () => {
  it('StrictMode opens one socket and subscribes once; reconnect replaces it', async () => {
    const { unmount } = mount(true)
    await flush()
    expect(Socket.instances).toHaveLength(1)
    const first = Socket.instances[0]
    act(() => first.connect())
    expect(first.subscriptions).toHaveLength(1)
    act(() => first.finishClose())
    await act(async () => { await vi.advanceTimersByTimeAsync(5_000) })
    expect(Socket.instances).toHaveLength(2)
    const second = Socket.instances[1]
    act(() => second.connect())
    expect(second.subscriptions).toHaveLength(1)
    expect(Socket.instances.filter(socket => socket.readyState === 1)).toHaveLength(1)
    unmount()
    await flush()
    second.finishClose()
    await act(async () => { await vi.advanceTimersByTimeAsync(30_000) })
    expect(Socket.instances).toHaveLength(2)
  })

  it('a new mount waits for actual socket close, not a synthetic close callback', async () => {
    const firstMount = mount()
    await flush()
    const first = Socket.instances[0]
    act(() => first.connect())
    firstMount.unmount()
    const secondMount = mount()
    await flush()
    expect(first.readyState).toBe(2)
    expect(first.frames.filter(frame => frame.startsWith('UNSUBSCRIBE'))).toHaveLength(1)
    expect(Socket.instances).toHaveLength(1)
    act(() => first.finishClose())
    await flush()
    expect(Socket.instances).toHaveLength(2)
    secondMount.unmount()
  })

  it('simultaneous hook owners share one connection and subscription', async () => {
    const first = mount()
    const second = mount()
    const firstInvalidations = vi.spyOn(first.queryClient, 'invalidateQueries')
    const secondInvalidations = vi.spyOn(second.queryClient, 'invalidateQueries')
    await flush()
    expect(Socket.instances).toHaveLength(1)
    const socket = Socket.instances[0]
    act(() => socket.connect())
    expect(socket.subscriptions).toHaveLength(1)
    act(() => socket.notify('notification-1'))
    expect(firstInvalidations).toHaveBeenCalledTimes(5)
    expect(secondInvalidations).toHaveBeenCalledTimes(5)
    first.unmount()
    expect(socket.readyState).toBe(1)
    act(() => socket.notify('notification-2'))
    expect(firstInvalidations).toHaveBeenCalledTimes(5)
    expect(secondInvalidations).toHaveBeenCalledTimes(10)
    second.unmount()
    await flush()
    expect(socket.readyState).toBe(2)
  })

  it('processes the same persisted notification once even when delivered again after reconnect', async () => {
    const { queryClient, unmount } = mount()
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries')
    await flush()
    const first = Socket.instances[0]
    act(() => first.connect())
    act(() => first.notify('notification-1'))
    expect(invalidate).toHaveBeenCalledTimes(5)
    act(() => first.notify('notification-1'))
    expect(invalidate).toHaveBeenCalledTimes(5)
    act(() => first.finishClose())
    await act(async () => { await vi.advanceTimersByTimeAsync(5_000) })
    const second = Socket.instances[1]
    act(() => second.connect())
    act(() => second.notify('notification-1'))
    expect(invalidate).toHaveBeenCalledTimes(5)
    act(() => second.notify('notification-2'))
    expect(invalidate).toHaveBeenCalledTimes(10)
    unmount()
    await flush()
  })
})
