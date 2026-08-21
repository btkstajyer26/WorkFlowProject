import { queryOptions, useQuery } from '@tanstack/react-query';

import { getRecordAuditLogs } from '@/api/auditLogs';

export const auditLogQueryKeys = {
  all: ['audit-logs'] as const,
  record: (recordId: string) =>
    [...auditLogQueryKeys.all, 'record', recordId] as const,
};

export function recordAuditLogsQueryOptions(recordId: string) {
  return queryOptions({
    queryFn: () => getRecordAuditLogs(recordId),
    queryKey: auditLogQueryKeys.record(recordId),
  });
}

export function useRecordAuditLogs(recordId: string, enabled = true) {
  return useQuery({
    ...recordAuditLogsQueryOptions(recordId),
    enabled: enabled && Boolean(recordId),
  });
}
