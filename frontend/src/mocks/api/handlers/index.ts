import { adminHandlers } from './adminHandlers'
import { auditHandlers } from './auditHandlers'
import { authHandlers } from './authHandlers'
import { categoryHandlers } from './categoryHandlers'
import { notificationHandlers } from './notificationHandlers'
import { recordHandlers } from './recordHandlers'
import { recordSearchHandlers } from './recordSearchHandlers'
import { workflowHandlers } from './workflowHandlers'

export const apiHandlers = [
  ...authHandlers,
  ...categoryHandlers,
  ...notificationHandlers,
  ...recordHandlers,
  ...recordSearchHandlers,
  ...workflowHandlers,
  ...auditHandlers,
  ...adminHandlers,
]
