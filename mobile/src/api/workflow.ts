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
]);

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
export type WorkflowActionResponse = z.infer<
  typeof workflowActionResponseSchema
>;
export type WorkflowActionRequest = {
  action: WorkflowAction;
  comment?: string;
};

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
