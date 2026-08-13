import { setupServer } from 'msw/node'
import { apiHandlers } from './handlers'

export const apiMockServer = setupServer(...apiHandlers)
