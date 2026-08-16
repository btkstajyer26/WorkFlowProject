const defaultApiBaseUrl = 'http://localhost:8080'

export const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || defaultApiBaseUrl).replace(/\/+$/, '')

const configuredApiMode = import.meta.env.VITE_API_MODE
const selectedApiMode = configuredApiMode === 'backend' || configuredApiMode === 'mock'
  ? configuredApiMode
  : import.meta.env.DEV ? 'mock' : 'backend'

// MSW geliştirme aracıdır. Production build yanlışlıkla `mock` değişkeniyle
// alınsa bile sahte veriyle veya handlersız bir ara durumda çalışmamalıdır.
export const apiMode = !import.meta.env.DEV && selectedApiMode === 'mock'
  ? 'backend'
  : selectedApiMode

export const isApiMockEnabled = import.meta.env.DEV && apiMode === 'mock'
