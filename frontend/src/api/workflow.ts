import type {
  WorkflowActionRequest,
  WorkflowActionResponse,
} from './generated/data-contracts'
import { api } from './client'
import { ApiClientError } from './errors'
import type { RecordStatus } from '../types/record'

export type WorkflowActionCode = NonNullable<WorkflowActionRequest['action']>

/** APP-9: sunucu tarafında hesaplanmış, kaydın şu anki durumu için gerçekten alınabilecek aksiyonlar. */
export type AvailableWorkflowAction = {
  action: WorkflowActionCode
  displayName: string
  commentRequired: boolean
  targetDepartmentRequired: boolean
  targetUserRequired: boolean
}

export type AvailableWorkflowActions = {
  recordId: string
  status: RecordStatus
  version: number
  actions: AvailableWorkflowAction[]
}

export type WorkflowTargetDepartment = {
  id: number
  name: string
}

const recordStatuses: RecordStatus[] = [
  'TASLAK',
  'BSK_YRD_INCELEMESINDE',
  'BASKAN_INCELEMESINDE',
  'DUZENLEME_BEKLIYOR',
  'ONAYLANDI',
  'REDDEDILDI',
]

function invalidWorkflowResponse(): never {
  throw new ApiClientError({
    code: 'INVALID_WORKFLOW_RESPONSE',
    message: 'Sunucu geçerli iş akışı sonucu döndürmedi.',
    status: 0,
  })
}

export async function performWorkflowAction(recordId: string, request: WorkflowActionRequest) {
  const response = await api.workflow.performAction({ recordId }, request)
  if (
    response.recordId !== recordId ||
    response.action !== request.action ||
    !response.previousStatus ||
    !recordStatuses.includes(response.previousStatus) ||
    !response.newStatus ||
    !recordStatuses.includes(response.newStatus) ||
    !response.performedBy ||
    !response.performedAt
  ) {
    return invalidWorkflowResponse()
  }
  return response as Required<Pick<
    WorkflowActionResponse,
    'recordId' | 'action' | 'previousStatus' | 'newStatus' | 'performedBy' | 'performedAt'
  >> & WorkflowActionResponse
}

function invalidAvailableActionsResponse(): never {
  throw new ApiClientError({
    code: 'INVALID_AVAILABLE_ACTIONS_RESPONSE',
    message: 'Sunucu geçerli yetkili aksiyon bilgisi döndürmedi.',
    status: 0,
  })
}

/**
 * B10/WEB-1: hangi aksiyon düğmelerinin gösterileceğine sunucu karar verir -
 * bu uçtan gelen liste yalnız görünürlük içindir, yetki denetimi değildir;
 * `performWorkflowAction` yetkiyi kendisi ayrıca doğrular.
 */
export async function getAvailableWorkflowActions(recordId: string): Promise<AvailableWorkflowActions> {
  const response = await api.workflowQuery.availableActions({ recordId })
  if (!response.recordId || !response.status || typeof response.version !== 'number') {
    return invalidAvailableActionsResponse()
  }

  const actions = (response.actions ?? [])
    .filter((action) => Boolean(action.action && action.displayName?.trim()))
    .map((action) => ({
      action: action.action!,
      displayName: action.displayName!.trim(),
      commentRequired: action.commentRequired === true,
      targetDepartmentRequired: action.targetDepartmentRequired === true,
      targetUserRequired: action.targetUserRequired === true,
    }))

  return {
    recordId: response.recordId,
    status: response.status,
    version: response.version,
    actions,
  }
}

/** Departman hedefi gerektiren aksiyonlar için uygun hedef departman listesi. Kişi seçici bilinçli olarak yapılmaz. */
export async function getWorkflowTargetDepartments(recordId: string): Promise<WorkflowTargetDepartment[]> {
  const response = await api.workflowQuery.targetDepartments({ recordId })
  return (response.departments ?? [])
    .filter((department) => typeof department.id === 'number' && Boolean(department.name?.trim()))
    .map((department) => ({ id: department.id!, name: department.name!.trim() }))
}
