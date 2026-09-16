import { z } from 'zod';

import { apiRequest } from './client';

export const recordStatusSchema = z.enum([
  'TASLAK',
  'BSK_YRD_INCELEMESINDE',
  'BASKAN_INCELEMESINDE',
  'DUZENLEME_BEKLIYOR',
  'ONAYLANDI',
  'REDDEDILDI',
]);

export const recordAssignmentSchema = z.object({
  departmentId: z.number().int().nullish(),
  departmentName: z.string().nullish(),
  kind: z.enum(['USER', 'DEPARTMENT', 'NONE']),
  userFullName: z.string().nullish(),
  userId: z.string().uuid().nullish(),
});

const recordDetailSchema = z.object({
  assignment: recordAssignmentSchema.nullish(),
  categoryId: z.number().int(),
  createdAt: z.string(),
  createdBy: z.string().uuid(),
  createdByFullName: z.string().nullish(),
  description: z.string(),
  id: z.string().uuid(),
  status: recordStatusSchema,
  title: z.string(),
  version: z.number().int(),
});

const recordListItemSchema = recordDetailSchema.extend({
  assignedTo: z.string().uuid().nullish(),
  updatedAt: z.string().nullish(),
});

const recordPageSchema = z.object({
  content: z.array(recordListItemSchema),
  page: z.number().int().nonnegative(),
  size: z.number().int().nonnegative(),
  totalElements: z.number().int().nonnegative(),
  totalPages: z.number().int().nonnegative(),
});

export type RecordStatus = z.infer<typeof recordStatusSchema>;
export type RecordAssignment = z.infer<typeof recordAssignmentSchema>;
export type RecordDetail = z.infer<typeof recordDetailSchema>;
export type RecordListItem = z.infer<typeof recordListItemSchema>;
export type RecordPage = z.infer<typeof recordPageSchema>;

export type RecordMutationRequest = {
  categoryId: number;
  description: string;
  title: string;
};

export type RecordFilters = {
  categoryId?: number;
  creator?: string;
  from?: string;
  page: number;
  q?: string;
  size: number;
  sort?: string;
  status?: RecordStatus;
  to?: string;
};

function setOptionalParam(
  params: URLSearchParams,
  key: string,
  value: number | string | undefined,
) {
  if (typeof value === 'number') {
    params.set(key, String(value));
    return;
  }

  if (value?.trim()) params.set(key, value.trim());
}

export async function getRecords(filters: RecordFilters): Promise<RecordPage> {
  const params = new URLSearchParams({
    page: String(filters.page),
    size: String(filters.size),
  });

  setOptionalParam(params, 'categoryId', filters.categoryId);
  setOptionalParam(params, 'creator', filters.creator);
  setOptionalParam(params, 'from', filters.from);
  setOptionalParam(params, 'q', filters.q);
  setOptionalParam(params, 'sort', filters.sort);
  setOptionalParam(params, 'status', filters.status);
  setOptionalParam(params, 'to', filters.to);

  const response = await apiRequest<unknown>(`/api/records?${params.toString()}`);
  return recordPageSchema.parse(response);
}

export async function getRecord(recordId: string): Promise<RecordDetail> {
  const response = await apiRequest<unknown>(`/api/records/${recordId}`);
  return recordDetailSchema.parse(response);
}

export async function createRecord(
  request: RecordMutationRequest,
): Promise<RecordDetail> {
  const response = await apiRequest<unknown>('/api/records', {
    json: request,
    method: 'POST',
  });
  return recordDetailSchema.parse(response);
}

export async function updateRecord(
  recordId: string,
  request: RecordMutationRequest,
): Promise<RecordDetail> {
  const response = await apiRequest<unknown>(`/api/records/${recordId}`, {
    json: request,
    method: 'PUT',
  });
  return recordDetailSchema.parse(response);
}

export function deleteRecord(recordId: string): Promise<void> {
  return apiRequest<void>(`/api/records/${recordId}`, { method: 'DELETE' });
}
