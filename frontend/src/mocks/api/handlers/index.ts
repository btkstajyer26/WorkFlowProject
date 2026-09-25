import { adminHandlers } from './adminHandlers'
import { auditHandlers } from './auditHandlers'
import { authHandlers } from './authHandlers'
import { categoryHandlers } from './categoryHandlers'
import { fileHandlers } from './fileHandlers'
import { notificationHandlers } from './notificationHandlers'
import { recordHandlers } from './recordHandlers'
import { subtaskHandlers } from './subtaskHandlers'
import { workflowHandlers } from './workflowHandlers'

export const apiHandlers = [
  ...authHandlers,
  ...categoryHandlers,
  ...notificationHandlers,
  ...recordHandlers,
  ...workflowHandlers,
  ...subtaskHandlers,
  ...auditHandlers,
  ...adminHandlers,
  ...fileHandlers,
]

