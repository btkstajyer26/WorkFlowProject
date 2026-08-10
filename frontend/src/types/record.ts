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
  history: RecordHistoryItem[]
}
