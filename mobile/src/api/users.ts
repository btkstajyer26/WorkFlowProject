import { z } from 'zod';

import { apiRequest } from './client';

export const userRoleSchema = z.enum([
  'CALISAN',
  'BASKAN_YARDIMCISI',
  'BASKAN',
  'ADMIN',
]);

const currentUserSchema = z.object({
  active: z.boolean(),
  createdAt: z.string(),
  email: z.string().email(),
  firstName: z.string().min(1),
  id: z.string().uuid(),
  lastName: z.string().min(1),
  roleName: userRoleSchema,
});

export type UserRole = z.infer<typeof userRoleSchema>;
export type CurrentUser = z.infer<typeof currentUserSchema>;

export async function getCurrentUser(): Promise<CurrentUser> {
  const response = await apiRequest<unknown>('/api/users/me');
  return currentUserSchema.parse(response);
}
