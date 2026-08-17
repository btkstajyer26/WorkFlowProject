import { QueryClient } from '@tanstack/react-query'
import { ApiClientError } from '../api/errors'

function shouldRetry(failureCount: number, error: unknown) {
  if (failureCount >= 2) return false
  if (!(error instanceof ApiClientError)) return true
  return error.status === 0 || error.status >= 500
}

export function createAppQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 15_000,
        refetchOnReconnect: true,
        refetchOnWindowFocus: true,
        retry: shouldRetry,
      },
      mutations: {
        retry: false,
      },
    },
  })
}
