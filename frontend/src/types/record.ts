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

/**
 * Kaydın atama türü (B11). İstemci türü iki nullable alanı karşılaştırarak çıkarsamaz;
 * `kind` üzerinden dallanır — `NONE` (atama yok) ile `DEPARTMENT` (departman kuyruğu)
 * ancak böyle ayrılabilir.
 */
export type RecordAssignment =
  | { kind: 'USER'; userId: string; userFullName: string | null }
  | { kind: 'DEPARTMENT'; departmentId: number; departmentName: string | null }
  | { kind: 'NONE' }

export type WorkflowRecord = {
  id: string
  recordNumber: string
  title: string
  description: string
  categoryId: number
  category: string
  status: RecordStatus
  createdBy: string
  createdById?: string
  /** Gösterime hazır atama etiketi; `assignment` üzerinden türetilir. */
  assignedTo: string | null
  assignedToId?: string | null
  lastDeputyId?: string | null
  assignment: RecordAssignment
  lastAction: string
  createdAt: string
  updatedAt: string
  attachments: { id: string; name: string; size: string }[]
  history: RecordHistoryItem[]
}
