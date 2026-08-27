import { z } from 'zod';

import { apiRequest } from './client';

export const notificationTypeSchema = z.enum([
  'RECORD_SUBMITTED',
  'RECORD_FORWARDED',
  'RECORD_APPROVED',
  'RECORD_REJECTED',
  'RECORD_RETURNED',
]);

export const notificationItemSchema = z.object({
  createdAt: z.string(),
  id: z.string().uuid(),
  message: z.string(),
  notificationType: notificationTypeSchema,
  read: z.boolean(),
  recordId: z.string().uuid(),
});

export const notificationPageSchema = z.object({
  content: z.array(notificationItemSchema),
  page: z.number().int().nonnegative(),
  size: z.number().int().nonnegative(),
  totalElements: z.number().int().nonnegative(),
  totalPages: z.number().int().nonnegative(),
});

export type NotificationType = z.infer<typeof notificationTypeSchema>;
export type NotificationItem = z.infer<typeof notificationItemSchema>;
export type NotificationPage = z.infer<typeof notificationPageSchema>;

export type NotificationListQuery = {
  page?: number;
  size?: number;
};

export async function getNotifications(
  query: NotificationListQuery = {},
): Promise<NotificationPage> {
  const params = new URLSearchParams({
    page: String(query.page ?? 0),
    size: String(query.size ?? 20),
  });

  const response = await apiRequest<unknown>(
    `/api/notifications?${params.toString()}`,
  );
  return notificationPageSchema.parse(response);
}

export async function getUnreadNotifications(): Promise<NotificationItem[]> {
  const response = await apiRequest<unknown>('/api/notifications/unread');
  return z.array(notificationItemSchema).parse(response);
}

export async function getUnreadNotificationCount(): Promise<number> {
  const response = await apiRequest<unknown>('/api/notifications/unread/count');
  return z.number().int().nonnegative().parse(response);
}

export function markNotificationAsRead(notificationId: string): Promise<void> {
  return apiRequest<void>(`/api/notifications/${notificationId}/read`, {
    method: 'PUT',
  });
}
