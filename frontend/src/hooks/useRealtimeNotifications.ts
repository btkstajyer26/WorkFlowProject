import { Client } from '@stomp/stompjs'
import { useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'
import { getApiAccessToken, subscribeApiAccessToken } from '../api/client'
import { notificationWebSocketUrl } from '../api/notificationWebSocket'
import { queryKeys } from '../query/queryKeys'

// Yeni mount, önceki authenticated uygulamanın socket kapanışını bekler.
let disconnecting: Promise<void> = Promise.resolve()

export function useRealtimeNotifications(enabled: boolean) {
  const queryClient = useQueryClient()

  useEffect(() => {
    if (!enabled) return
    let disposed = false
    const client = new Client({
      brokerURL: notificationWebSocketUrl(),
      reconnectDelay: 5_000,
      connectionTimeout: 10_000,
    })

    const stop = () => {
      disconnecting = client.deactivate({ force: true })
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

    client.onConnect = () => {
      if (disposed || !getApiAccessToken()) return
      client.subscribe('/user/queue/notifications', (message) => {
        if (disposed || !getApiAccessToken()) return
        void queryClient.invalidateQueries({ queryKey: queryKeys.notifications.lists() })
        void queryClient.invalidateQueries({ queryKey: queryKeys.notifications.unread })
        void queryClient.invalidateQueries({ queryKey: queryKeys.notifications.unreadCount })

        // Payload yalnızca hangi kaydın tekrar okunacağını belirler; durum ve
        // aksiyon yetkileri her zaman mevcut API üzerinden alınır.
        try {
          const payload: unknown = JSON.parse(message.body)
          if (payload && typeof payload === 'object' && 'recordId' in payload
            && typeof payload.recordId === 'string' && payload.recordId.trim()) {
            void queryClient.invalidateQueries({ queryKey: queryKeys.records.detail(payload.recordId) })
            void queryClient.invalidateQueries({ queryKey: queryKeys.records.lists() })
          }
        } catch {
          // Geçersiz payload da notification cache'leri için değişiklik sinyalidir.
        }
      })
    }

    const syncConnection = () => {
      if (getApiAccessToken()) client.activate()
      else stop()
    }
    const unsubscribeToken = subscribeApiAccessToken(syncConnection)
    syncConnection()

    return () => {
      disposed = true
      unsubscribeToken()
      stop()
    }
  }, [enabled, queryClient])
}
