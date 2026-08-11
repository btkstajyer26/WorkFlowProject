import { adminHandlers } from './adminHandlers'
import { auditHandlers } from './auditHandlers'
import { authHandlers } from './authHandlers'
import { categoryHandlers } from './categoryHandlers'
import { recordHandlers } from './recordHandlers'
import { workflowHandlers } from './workflowHandlers'

export const apiHandlers = [
  ...authHandlers,
  ...categoryHandlers,
  ...recordHandlers,
  ...workflowHandlers,
  ...auditHandlers,
  ...adminHandlers,
]
