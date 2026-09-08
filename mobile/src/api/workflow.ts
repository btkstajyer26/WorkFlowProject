import { z } from 'zod';

import { apiRequest } from './client';
import { recordStatusSchema } from './records';

export const workflowActionSchema = z.enum([
  'GONDER',
  'TEKRAR_GONDER',
  'BASKANA_ILET',
  'CALISANA_GERI_GONDER',
  'BASKAN_YARDIMCISINA_GERI_GONDER',
  'ONAYLA',
  'REDDET',
  'DEPARTMANA_GONDER',
]);

const availableWorkflowActionSchema = z.object({
  action: workflowActionSchema,
  commentRequired: z.boolean(),
  displayName: z.string(),
  targetDepartmentRequired: z.boolean(),
  targetUserRequired: z.boolean(),
});

const availableWorkflowActionsResponseSchema = z.object({
  actions: z.array(availableWorkflowActionSchema),
  recordId: z.string().uuid(),
  status: recordStatusSchema,
  version: z.number().int(),
});

const targetDepartmentsResponseSchema = z.object({
  departments: z.array(z.object({
    id: z.number().int(),
    name: z.string(),
  })),
});

const workflowActionResponseSchema = z.object({
  action: workflowActionSchema,
  assignedTo: z.string().uuid().nullish(),
  newStatus: recordStatusSchema,
  performedAt: z.string(),
  performedBy: z.string().uuid(),
  previousStatus: recordStatusSchema,
  recordId: z.string().uuid(),
});

export type WorkflowAction = z.infer<typeof workflowActionSchema>;
export type AvailableWorkflowAction = z.infer<
  typeof availableWorkflowActionSchema
>;
export type AvailableWorkflowActionsResponse = z.infer<
  typeof availableWorkflowActionsResponseSchema
>;

export type WorkflowActionResponse = z.infer<
  typeof workflowActionResponseSchema
>;
export type WorkflowActionRequest = {
  action: WorkflowAction;
  comment?: string;
  targetDepartmentId?: number;
  targetUserId?: string;
};

export async function getAvailableWorkflowActions(
  recordId: string,
): Promise<AvailableWorkflowActionsResponse> {
  const response = await apiRequest<unknown>(
    `/api/records/${recordId}/workflow/available-actions`,
  );

  return availableWorkflowActionsResponseSchema.parse(response);
}

export async function getWorkflowTargetDepartments(recordId: string) {
  const response = await apiRequest<unknown>(
    `/api/records/${recordId}/workflow/target-departments`,
  );
  return targetDepartmentsResponseSchema.parse(response);
}

export async function performWorkflowAction(
  recordId: string,
  request: WorkflowActionRequest,
): Promise<WorkflowActionResponse> {
  const response = await apiRequest<unknown>(
    `/api/records/${recordId}/workflow/actions`,
    { json: request, method: 'POST' },
  );
  return workflowActionResponseSchema.parse(response);
}
