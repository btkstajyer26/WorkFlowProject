import { useMutation, useQueryClient } from '@tanstack/react-query';

import { ApiClientError } from '@/api/errors';
import {
  performWorkflowAction,
  type WorkflowActionRequest,
} from '@/api/workflow';

import { auditLogQueryKeys } from './auditLogs';
import { recordQueryKeys } from './records';

const conflictCodes = new Set([
  'VERSION_CONFLICT',
  'WORKFLOW_RECORD_LOCKED',
  'WORKFLOW_VERSION_CONFLICT',
]);

export function useRecordWorkflow(recordId: string) {
  const queryClient = useQueryClient();

  const refreshRecord = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: recordQueryKeys.all }),
      queryClient.invalidateQueries({
        queryKey: auditLogQueryKeys.record(recordId),
      }),
    ]);
  };

  return useMutation({
    mutationFn: (request: WorkflowActionRequest) =>
      performWorkflowAction(recordId, request),
    onError: async (error) => {
      if (error instanceof ApiClientError && conflictCodes.has(error.code)) {
        await refreshRecord();
      }
    },
    onSuccess: refreshRecord,
  });
}
