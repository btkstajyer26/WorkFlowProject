import { z } from 'zod';

import { apiRequest } from './client';

const categorySchema = z.object({
  id: z.number().int(),
  name: z.string().min(1),
});

const categoriesSchema = z.array(categorySchema);

export type Category = z.infer<typeof categorySchema>;

export async function getCategories(): Promise<Category[]> {
  const response = await apiRequest<unknown>('/api/categories');
  return categoriesSchema.parse(response);
}
