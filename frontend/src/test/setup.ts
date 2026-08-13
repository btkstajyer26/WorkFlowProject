import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { clearAuthSession } from '../auth/authSession'
import { clearCategoryCache } from '../api/categories'
import { resetMockAuthState } from '../mocks/api/auth'
import { resetMockApiDb } from '../mocks/api/db'
import { apiMockServer } from '../mocks/api/server'

beforeAll(() => {
  apiMockServer.listen({ onUnhandledRequest: 'error' })
})

afterEach(() => {
  cleanup()
  apiMockServer.resetHandlers()
  resetMockAuthState()
  resetMockApiDb()
  clearCategoryCache()
  clearAuthSession()
  window.sessionStorage.clear()
  window.localStorage.clear()
})

afterAll(() => {
  apiMockServer.close()
})

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => undefined,
    removeListener: () => undefined,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
    dispatchEvent: () => false,
  }),
})
