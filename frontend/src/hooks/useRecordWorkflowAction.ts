import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { WorkflowActionRequest } from '../api/generated/data-contracts'
import { ApiClientError } from '../api/errors'
import {
  getAvailableWorkflowActions,
  getWorkflowTargetDepartments,
  performWorkflowAction,
} from '../api/workflow'
import { queryKeys } from '../query/queryKeys'
import { roleLabelOf, type AuthUser } from '../types/auth'
import type { WorkflowRecord } from '../types/record'

const actionLabels: Record<NonNullable<WorkflowActionRequest['action']>, string> = {
  GONDER: 'Başkan Yardımcısına gönderildi',
  DEPARTMANA_GONDER: 'Departmana gönderildi',
  TEKRAR_GONDER: 'Yeniden incelemeye gönderildi',
  BASKANA_ILET: 'Başkana iletildi',
  CALISANA_GERI_GONDER: 'Çalışana geri gönderildi',
  BASKAN_YARDIMCISINA_GERI_GONDER: 'Başkan Yardımcısına geri gönderildi',
  ONAYLA: 'Kayıt onaylandı',
  REDDET: 'Kayıt reddedildi',
}

/** B10/WEB-1: kaydın şu anki durumu için hangi aksiyon düğmelerinin gösterileceği - sunucu hesaplar. */
export function useAvailableWorkflowActions(recordId: string, enabled = true) {
  return useQuery({
    queryKey: queryKeys.workflow.availableActions(recordId),
    queryFn: () => getAvailableWorkflowActions(recordId),
    enabled: enabled && Boolean(recordId),
  })
}

/** Yalnız departman hedefi gereken bir aksiyon seçiliyken çağrılır (`enabled`). */
export function useWorkflowTargetDepartments(recordId: string, enabled: boolean) {
  return useQuery({
    queryKey: queryKeys.workflow.targetDepartments(recordId),
    queryFn: () => getWorkflowTargetDepartments(recordId),
    enabled: enabled && Boolean(recordId),
    staleTime: 0,
  })
}

const conflictCodes = new Set(['VERSION_CONFLICT', 'WORKFLOW_RECORD_LOCKED', 'WORKFLOW_VERSION_CONFLICT'])

export function useRecordWorkflowAction(recordId: string, actor: AuthUser) {
  const queryClient = useQueryClient()

  const refreshAfterConflict = () => Promise.all([
    queryClient.invalidateQueries({ queryKey: queryKeys.records.detail(recordId) }),
    queryClient.invalidateQueries({ queryKey: queryKeys.workflow.availableActions(recordId) }),
    queryClient.invalidateQueries({ queryKey: queryKeys.workflow.targetDepartments(recordId) }),
  ])

  return useMutation({
    mutationFn: (request: WorkflowActionRequest) => performWorkflowAction(recordId, request),
    onError: async (error) => {
      if (error instanceof ApiClientError && conflictCodes.has(error.code)) {
        await refreshAfterConflict()
      }
    },
    onSuccess: async (result, request) => {
      const action = result.action
      const actionLabel = actionLabels[action]
      queryClient.setQueriesData<WorkflowRecord>(
        { queryKey: queryKeys.records.detail(recordId) },
        (record) => record ? {
          ...record,
          status: result.newStatus,
          assignedToId: result.assignedTo ?? null,
          lastAction: actionLabel,
          updatedAt: result.performedAt,
          history: [
            ...record.history,
            {
              id: `workflow-${action}-${result.performedAt}`,
              action: actionLabel,
              actor: `${actor.firstName} ${actor.lastName}`,
              actorId: result.performedBy,
              role: roleLabelOf(actor),
              note: request.comment?.trim() || undefined,
              date: result.performedAt,
            },
          ],
        } : record,
      )

      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.records.lists() }),
        queryClient.invalidateQueries({ queryKey: queryKeys.workflow.availableActions(recordId) }),
        queryClient.invalidateQueries({ queryKey: queryKeys.workflow.targetDepartments(recordId) }),
        request.action === 'CALISANA_GERI_GONDER'
          ? Promise.resolve()
          : queryClient.invalidateQueries({
              queryKey: queryKeys.records.detail(recordId),
              refetchType: 'none',
            }),
        queryClient.invalidateQueries({ queryKey: queryKeys.notifications.all }),
      ])
    },
  })
}
