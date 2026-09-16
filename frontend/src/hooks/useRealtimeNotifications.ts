import { Client, type StompSubscription } from '@stomp/stompjs'
import { type QueryClient, useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'
import { getApiAccessToken, subscribeApiAccessToken } from '../api/client'
import { notificationWebSocketUrl } from '../api/notificationWebSocket'
import { queryKeys } from '../query/queryKeys'

// All hook owners in this tab share the authenticated transport. QueryClient is
// replaced on account/role changes; its delivery history must not cross sessions.
const owners = new Map<QueryClient, number>()
const delivered = new WeakMap<QueryClient, Set<string>>()
let disconnecting: Promise<void> = Promise.resolve()
let releaseConnection: (() => void) | undefined

function startConnection() {
  let disposed = false
  let subscription: StompSubscription | undefined
  const client = new Client({
    brokerURL: notificationWebSocketUrl(),
    reconnectDelay: 5_000,
    connectionTimeout: 10_000,
  })

  const stop = () => {
    const previous = disconnecting
    if (client.connected) subscription?.unsubscribe()
    subscription = undefined
    const socket = client.webSocket
    const closing = client.deactivate()
    // force:true synthesizes onclose before the native socket actually closes.
    // Close the transport without waiting for a broker receipt, but keep the
    // real close promise so a replacement cannot overlap the old connection.
    socket?.close()
    disconnecting = Promise.all([previous, closing]).then(() => undefined)
    client.connectHeaders = {}
  }

  client.beforeConnect = async () => {
    await disconnecting
    const token = getApiAccessToken()
    if (disposed || !token) {
      stop()
      return
    }
    client.connectHeaders = { Authorization: `Bearer ${token}` }
  }

  client.onWebSocketClose = () => { subscription = undefined }
  client.onConnect = () => {
    if (disposed || !getApiAccessToken() || subscription) return
    subscription = client.subscribe('/user/queue/notifications', (message) => {
      if (disposed || !getApiAccessToken()) return
      let id: string | undefined
      let recordId: string | undefined
      try {
        const payload: unknown = JSON.parse(message.body)
        if (payload && typeof payload === 'object') {
          if ('id' in payload && typeof payload.id === 'string' && payload.id.trim()) id = payload.id
          if ('recordId' in payload && typeof payload.recordId === 'string' && payload.recordId.trim()) {
            recordId = payload.recordId
          }
        }
      } catch {
        // Malformed payloads still signal a notification cache refresh.
      }

      for (const queryClient of owners.keys()) {
        let seen = delivered.get(queryClient)
        if (!seen) {
          seen = new Set<string>()
          delivered.set(queryClient, seen)
        }
        if (id && seen.has(id)) continue
        if (id) seen.add(id)
        void queryClient.invalidateQueries({ queryKey: queryKeys.notifications.lists() })
        void queryClient.invalidateQueries({ queryKey: queryKeys.notifications.unread })
        void queryClient.invalidateQueries({ queryKey: queryKeys.notifications.unreadCount })
        // Payload only identifies the record; API remains authoritative.
        if (recordId) {
          void queryClient.invalidateQueries({ queryKey: queryKeys.records.detail(recordId) })
          void queryClient.invalidateQueries({ queryKey: queryKeys.records.lists() })
        }
      }
    })
  }

  const syncConnection = () => {
    if (getApiAccessToken()) client.activate()
    else {
      for (const queryClient of owners.keys()) delivered.delete(queryClient)
      stop()
    }
  }
  const unsubscribeToken = subscribeApiAccessToken(syncConnection)
  syncConnection()

  return () => {
    disposed = true
    unsubscribeToken()
    stop()
  }
}

export function useRealtimeNotifications(enabled: boolean) {
  const queryClient = useQueryClient()
  useEffect(() => {
    if (!enabled) return
    owners.set(queryClient, (owners.get(queryClient) ?? 0) + 1)
    releaseConnection ??= startConnection()
    return () => {
      const remaining = (owners.get(queryClient) ?? 1) - 1
      if (remaining) owners.set(queryClient, remaining)
      else owners.delete(queryClient)
      if (owners.size === 0) {
        releaseConnection?.()
        releaseConnection = undefined
      }
    }
  }, [enabled, queryClient])
}
