import { createContext, useContext } from 'react'
import type { WorkflowActionInput } from '../domain/workflow'
import type { AuthUser } from '../types/auth'
import type { NotificationItem } from '../types/notification'
import type { WorkflowRecord } from '../types/record'

export type RecordAttachment = WorkflowRecord['attachments'][number]

export type RecordDraftInput = {
  title: string
  category: string
  description: string
  attachments: RecordAttachment[]
}

export type WorkflowContextValue = {
  user: AuthUser
  records: WorkflowRecord[]
  visibleRecords: WorkflowRecord[]
  notifications: NotificationItem[]
  unreadNotificationCount: number
  createDraft: (input: RecordDraftInput) => WorkflowRecord
  createAndSubmit: (input: RecordDraftInput) => WorkflowRecord
  updateEditableRecord: (recordId: string, input: RecordDraftInput) => WorkflowRecord
  updateAndSubmit: (recordId: string, input: RecordDraftInput) => WorkflowRecord
  deleteDraft: (recordId: string) => void
  applyAction: (recordId: string, input: Omit<WorkflowActionInput, 'actor'>) => WorkflowRecord
  markNotificationRead: (notificationId: string) => void
  markAllNotificationsRead: () => void
}

export const WorkflowContext = createContext<WorkflowContextValue | null>(null)

export function useWorkflow() {
  const context = useContext(WorkflowContext)
  if (!context) throw new Error('useWorkflow must be used inside WorkflowProvider.')
  return context
}
