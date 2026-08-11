import { setupWorker } from 'msw/browser'
import { apiHandlers } from './handlers'

export const apiMockWorker = setupWorker(...apiHandlers)
