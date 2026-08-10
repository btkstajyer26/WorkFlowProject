import type { WorkflowRole } from './auth'

export type RecordStatus =
  | 'TASLAK'
  | 'BSK_YRD_INCELEMESINDE'
  | 'BASKAN_INCELEMESINDE'
  | 'DUZENLEME_BEKLIYOR'
  | 'ONAYLANDI'
  | 'REDDEDILDI'

export type RecordHistoryItem = {
  id: string
  action: string
  actor: string
  actorId?: string
  role: string
  note?: string
  date: string
}

export type RecordNote = {
  id: string
  recordId: string
  authorId: string
  author: string
  authorRole: WorkflowRole
  body: string
  createdAt: string
  updatedAt: string
  version: number
}

export type WorkflowRecord = {
  id: string
  recordNumber: string
  title: string
  description: string
  category: string
  status: RecordStatus
  createdBy: string
  createdById?: string
  assignedTo: string | null
  assignedToId?: string | null
  lastDeputyId?: string | null
  lastAction: string
  createdAt: string
  updatedAt: string
  attachments: { id: string; name: string; size: string }[]
  notes: RecordNote[]
  history: RecordHistoryItem[]
}
