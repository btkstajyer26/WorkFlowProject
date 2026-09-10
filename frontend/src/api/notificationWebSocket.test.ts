import { describe, expect, it } from 'vitest'
import { notificationWebSocketUrl } from './notificationWebSocket'

describe('notificationWebSocketUrl', () => {
  it.each([
    ['http://example.test:8080', 'ws://example.test:8080/ws'],
    ['https://example.test/', 'wss://example.test/ws'],
    ['https://example.test/backend/', 'wss://example.test/backend/ws'],
    ['https://example.test?token=ignored#fragment', 'wss://example.test/ws'],
  ])('%s adresini dönüştürür', (base, expected) => {
    expect(notificationWebSocketUrl(base)).toBe(expected)
  })

  it('relative base URL için mevcut origin kullanır', () => {
    const expected = new URL('/backend/ws', window.location.origin)
    expected.protocol = expected.protocol === 'https:' ? 'wss:' : 'ws:'
    expect(notificationWebSocketUrl('/backend')).toBe(expected.toString())
  })
})
