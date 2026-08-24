import { z } from 'zod';

import { apiRequest } from './client';
import { recordStatusSchema } from './records';

const auditLogSchema = z.object({
  action: z.string().min(1),
  comment: z.string().nullish(),
  createdAt: z.string(),
  errorCode: z.string().nullish(),
  httpMethod: z.string().nullish(),
  httpStatus: z.number().int().nullish(),
  id: z.string().uuid(),
  newStatus: recordStatusSchema.nullish(),
  previousStatus: recordStatusSchema.nullish(),
  recordId: z.string().uuid(),
  requestPath: z.string().nullish(),
  roleId: z.number().int().nullish(),
  roleName: z.string().nullish(),
  userFullName: z.string().nullish(),
  userId: z.string().uuid().nullish(),
});

const auditLogsSchema = z.array(auditLogSchema);

export type AuditLog = z.infer<typeof auditLogSchema>;

export async function getRecordAuditLogs(recordId: string): Promise<AuditLog[]> {
  const response = await apiRequest<unknown>(
    `/api/audit-logs/record/${recordId}`,
  );
  return auditLogsSchema.parse(response);
}
