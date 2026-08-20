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

const recordListItemSchema = z.object({
  assignedTo: z.string().uuid().nullish(),
  categoryId: z.number().int(),
  createdAt: z.string(),
  createdBy: z.string().uuid(),
  createdByFullName: z.string().nullish(),
  description: z.string(),
  id: z.string().uuid(),
  status: recordStatusSchema,
  title: z.string(),
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
export type RecordListItem = z.infer<typeof recordListItemSchema>;
export type RecordPage = z.infer<typeof recordPageSchema>;

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
