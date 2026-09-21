import { apiHttpClient } from './client'
import { ApiClientError } from './errors'
import type {
  AssignableUser,
  PerformSubtaskActionRequest,
  Subtask,
  SubtaskListResponse,
  SplitIntoSubtasksRequest,
  SplitIntoSubtasksResponse,
} from '../types/subtask'

/**
 * Backend'de SubtaskController hazır olana kadar üretilen (`generated/`)
 * istemci yerine `apiHttpClient.request` doğrudan kullanılıyor -
 * docs/PARENT_SUBTASK_GOREV_DAGILIMI.md §2.2'deki sözleşmenin birebir
 * karşılığı. Backend mergelendiğinde `npm run api:generate` ile üretilen
 * `SubtaskController` sınıfına geçilip bu dosya kaldırılabilir.
 */

function invalidSubtaskResponse(): never {
  throw new ApiClientError({
    code: 'INVALID_SUBTASK_RESPONSE',
    message: 'Sunucu geçerli alt görev bilgisi döndürmedi.',
    status: 0,
  })
}

export async function splitIntoSubtasks(
  recordId: string,
  request: SplitIntoSubtasksRequest,
): Promise<SplitIntoSubtasksResponse> {
  const response = await apiHttpClient.request<SplitIntoSubtasksResponse>({
    path: `/api/records/${recordId}/subtasks/split`,
    method: 'POST',
    body: request,
    secure: true,
    type: 'application/json',
  })
  if (
    response.parentRecordId !== recordId ||
    !response.approvalPolicy ||
    typeof response.requiredApprovals !== 'number' ||
    !Array.isArray(response.subtasks)
  ) {
    return invalidSubtaskResponse()
  }
  return response
}

export async function getSubtasks(recordId: string): Promise<SubtaskListResponse> {
  const response = await apiHttpClient.request<SubtaskListResponse>({
    path: `/api/records/${recordId}/subtasks`,
    method: 'GET',
    secure: true,
  })
  if (!Array.isArray(response.subtasks)) {
    return invalidSubtaskResponse()
  }
  return response
}

export async function getAssignableUsers(recordId: string): Promise<AssignableUser[]> {
  const response = await apiHttpClient.request<{ users: AssignableUser[] }>({
    path: `/api/records/${recordId}/subtasks/assignable-users`,
    method: 'GET',
    secure: true,
  })
  return (response.users ?? []).filter((user) => Boolean(user.id && user.fullName?.trim()))
}

export async function performSubtaskAction(
  subtaskId: string,
  request: PerformSubtaskActionRequest,
): Promise<Subtask> {
  const response = await apiHttpClient.request<Subtask>({
    path: `/api/subtasks/${subtaskId}/actions`,
    method: 'POST',
    body: request,
    secure: true,
    type: 'application/json',
  })
  if (!response.id || !response.status) {
    return invalidSubtaskResponse()
  }
  return response
}
