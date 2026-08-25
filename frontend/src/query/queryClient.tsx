import { QueryClientProvider } from '@tanstack/react-query'
import { useState, type ReactNode } from 'react'
import { createAppQueryClient } from './createQueryClient'

export function AppQueryProvider({ children }: { children: ReactNode }) {
  const [client] = useState(createAppQueryClient)
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>
}
