/**
 * Parent/Subtask dinamik alt akışı. Subtask'lar genel workflow motorunun
 * (workflow_transitions) DIŞINDA, ayrı ve basit bir 5 durumlu akışta ilerler -
 * bkz. docs/PARENT_SUBTASK_GOREV_DAGILIMI.md §1-2.
 */
export type SubtaskStatus = 'DEGERLENDIRME' | 'ISLEM' | 'ONAY' | 'TAMAMLANDI' | 'REDDEDILDI'

export type ApprovalPolicy = 'UNANIMOUS' | 'MAJORITY'

export type SubtaskActionCode = 'DEGERLENDIRMEYI_TAMAMLA' | 'ISLEMI_TAMAMLA' | 'ONAYLA' | 'REDDET'

export type Subtask = {
  id: string
  parentRecordId: string
  title: string
  description: string
  assignedTo: string
  assignedToName: string
  status: SubtaskStatus
  resolutionComment: string | null
  createdAt: string
  completedAt: string | null
}

export type SubtaskListResponse = {
  approvalPolicy: ApprovalPolicy | null
  requiredApprovals: number | null
  subtasks: Subtask[]
}

export type SplitIntoSubtasksItem = {
  title: string
  description: string
  assigneeUserId: string
}

export type SplitIntoSubtasksRequest = {
  approvalPolicy: ApprovalPolicy
  subtasks: SplitIntoSubtasksItem[]
}

export type SplitIntoSubtasksResponse = {
  parentRecordId: string
  approvalPolicy: ApprovalPolicy
  requiredApprovals: number
  subtasks: Subtask[]
}

export type PerformSubtaskActionRequest = {
  action: SubtaskActionCode
  comment?: string
}

export type AssignableUser = {
  id: string
  fullName: string
}
