import { z } from 'zod';

import { apiRequest } from './client';

const currentUserSchema = z.object({
  active: z.boolean(),
  createdAt: z.string(),
  email: z.string().email(),
  firstName: z.string().min(1),
  id: z.string().uuid(),
  lastName: z.string().min(1),
  permissionCodes: z.array(z.string()),
  roleId: z.number().int().nullable(),
  roleName: z.string().nullable(),
  systemKey: z.string().nullable(),
});

export type CurrentUser = z.infer<typeof currentUserSchema>;

export async function getCurrentUser(): Promise<CurrentUser> {
  const response = await apiRequest<unknown>('/api/users/me');
  return currentUserSchema.parse(response);
}
