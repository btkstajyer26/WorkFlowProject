import { apiBaseUrl } from './config'

export function notificationWebSocketUrl(baseUrl = apiBaseUrl) {
  const url = new URL(baseUrl, window.location.origin)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  url.pathname = `${url.pathname.replace(/\/+$/, '')}/ws`
  url.search = ''
  url.hash = ''
  return url.toString()
}
