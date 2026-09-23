import { api } from './client'
import { ApiClientError } from './errors'
import type { SubtaskView } from './generated/data-contracts'
import type {
  AssignableUser,
  PerformSubtaskActionRequest,
  Subtask,
  SubtaskListResponse,
  SplitIntoSubtasksRequest,
  SplitIntoSubtasksResponse,
} from '../types/subtask'

function invalidSubtaskResponse(): never {
  throw new ApiClientError({
    code: 'INVALID_SUBTASK_RESPONSE',
    message: 'Sunucu geçerli alt görev bilgisi döndürmedi.',
    status: 0,
  })
}

/** Üretilen `SubtaskView` her alanı isteğe bağlı taşır; burada zorunlu alanlar doğrulanıp daraltılır. */
function toSubtask(view: SubtaskView): Subtask {
  if (
    !view.id || !view.parentRecordId || !view.title || !view.assignedTo ||
    !view.assignedToName || !view.status || !view.createdAt
  ) {
    return invalidSubtaskResponse()
  }
  return {
    id: view.id,
    parentRecordId: view.parentRecordId,
    title: view.title,
    description: view.description ?? '',
    assignedTo: view.assignedTo,
    assignedToName: view.assignedToName,
    status: view.status,
    resolutionComment: view.resolutionComment ?? null,
    createdAt: view.createdAt,
    completedAt: view.completedAt ?? null,
  }
}

export async function splitIntoSubtasks(
  recordId: string,
  request: SplitIntoSubtasksRequest,
): Promise<SplitIntoSubtasksResponse> {
  const response = await api.subtasks.split({ recordId }, request)
  if (
    response.parentRecordId !== recordId ||
    !response.approvalPolicy ||
    typeof response.requiredApprovals !== 'number' ||
    !Array.isArray(response.subtasks)
  ) {
    return invalidSubtaskResponse()
  }
  return {
    parentRecordId: response.parentRecordId,
    approvalPolicy: response.approvalPolicy,
    requiredApprovals: response.requiredApprovals,
    subtasks: response.subtasks.map(toSubtask),
  }
}

export async function getSubtasks(recordId: string): Promise<SubtaskListResponse> {
  const response = await api.subtasks.list1({ recordId })
  if (!Array.isArray(response.subtasks)) {
    return invalidSubtaskResponse()
  }
  return {
    approvalPolicy: response.approvalPolicy ?? null,
    requiredApprovals: response.requiredApprovals ?? null,
    subtasks: response.subtasks.map(toSubtask),
  }
}

export async function getAssignableUsers(recordId: string): Promise<AssignableUser[]> {
  const response = await api.subtasks.assignableUsers({ recordId })
  return (response.users ?? [])
    .filter((user): user is Required<typeof user> => Boolean(user.id && user.fullName?.trim()))
    .map((user) => ({ id: user.id, fullName: user.fullName }))
}

export async function performSubtaskAction(
  subtaskId: string,
  request: PerformSubtaskActionRequest,
): Promise<Subtask> {
  const response = await api.subtasks.performAction({ subtaskId }, request)
  return toSubtask(response)
}
