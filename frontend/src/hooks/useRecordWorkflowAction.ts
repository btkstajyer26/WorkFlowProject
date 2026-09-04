import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { WorkflowActionRequest } from '../api/generated/data-contracts'
import { performWorkflowAction } from '../api/workflow'
import { queryKeys } from '../query/queryKeys'
import { roleLabelOf, type AuthUser } from '../types/auth'
import type { WorkflowRecord } from '../types/record'

const actionLabels: Record<NonNullable<WorkflowActionRequest['action']>, string> = {
  GONDER: 'Başkan Yardımcısına gönderildi',
  TEKRAR_GONDER: 'Yeniden incelemeye gönderildi',
  BASKANA_ILET: 'Başkana iletildi',
  CALISANA_GERI_GONDER: 'Çalışana geri gönderildi',
  BASKAN_YARDIMCISINA_GERI_GONDER: 'Başkan Yardımcısına geri gönderildi',
  ONAYLA: 'Kayıt onaylandı',
  REDDET: 'Kayıt reddedildi',
}

export function useRecordWorkflowAction(recordId: string, actor: AuthUser) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: WorkflowActionRequest) => performWorkflowAction(recordId, request),
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
