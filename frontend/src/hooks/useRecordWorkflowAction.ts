import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { WorkflowActionRequest } from '../api/generated/data-contracts'
import { performWorkflowAction } from '../api/workflow'
import { queryKeys } from '../query/queryKeys'

export function useRecordWorkflowAction(recordId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: WorkflowActionRequest) => performWorkflowAction(recordId, request),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.records.lists() }),
        queryClient.invalidateQueries({
          queryKey: queryKeys.records.detail(recordId),
          refetchType: 'none',
        }),
        queryClient.invalidateQueries({ queryKey: queryKeys.notifications.all }),
      ])
    },
  })
}
