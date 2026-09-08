import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { ApiClientError } from '@/api/errors';
import {
  getAvailableWorkflowActions,
  performWorkflowAction,
  type WorkflowActionRequest,
} from '@/api/workflow';

import { auditLogQueryKeys } from './auditLogs';
import { recordQueryKeys } from './records';

export const workflowQueryKeys = {
  all: ['workflow'] as const,
  availableActions: (recordId: string) =>
    [...workflowQueryKeys.all, 'available-actions', recordId] as const,
};

export function useAvailableWorkflowActions(recordId: string, enabled = true) {
  return useQuery({
    enabled: enabled && Boolean(recordId),
    queryFn: () => getAvailableWorkflowActions(recordId),
    queryKey: workflowQueryKeys.availableActions(recordId),
  });
}

const conflictCodes = new Set([
  'VERSION_CONFLICT',
  'WORKFLOW_RECORD_LOCKED',
  'WORKFLOW_VERSION_CONFLICT',
]);

export function useRecordWorkflow(recordId: string) {
  const queryClient = useQueryClient();

  const refreshAfterConflict = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: recordQueryKeys.all }),
    queryClient.invalidateQueries({
      queryKey: workflowQueryKeys.availableActions(recordId),
    }),
      queryClient.invalidateQueries({
        queryKey: auditLogQueryKeys.record(recordId),
      }),
    ]);
  };

  const refreshAfterSuccess = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: recordQueryKeys.lists() }),
      queryClient.invalidateQueries({ queryKey: recordQueryKeys.counts() }),
    queryClient.invalidateQueries({
      queryKey: workflowQueryKeys.availableActions(recordId),
    }),
      queryClient.invalidateQueries({
        exact: true,
        queryKey: recordQueryKeys.detail(recordId),
        refetchType: 'none',
      }),
      queryClient.invalidateQueries({
        exact: true,
        queryKey: auditLogQueryKeys.record(recordId),
        refetchType: 'none',
      }),
    ]);
  };

  return useMutation({
    mutationFn: (request: WorkflowActionRequest) =>
      performWorkflowAction(recordId, request),
    onError: async (error) => {
      if (error instanceof ApiClientError && conflictCodes.has(error.code)) {
        await refreshAfterConflict();
      }
    },
    onSuccess: refreshAfterSuccess,
  });
}
