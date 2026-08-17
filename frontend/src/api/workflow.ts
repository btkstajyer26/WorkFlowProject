import type {
  WorkflowActionRequest,
  WorkflowActionResponse,
} from './generated/data-contracts'
import { api } from './client'
import { ApiClientError } from './errors'
import type { RecordStatus } from '../types/record'

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
