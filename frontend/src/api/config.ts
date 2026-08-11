const defaultApiBaseUrl = 'http://localhost:8080'

export const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || defaultApiBaseUrl).replace(/\/+$/, '')

export const apiMode = import.meta.env.VITE_API_MODE === 'backend' ? 'backend' : 'mock'

export const isApiMockEnabled = import.meta.env.DEV && apiMode === 'mock'
