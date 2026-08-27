import {
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';

import {
  getNotifications,
  getUnreadNotificationCount,
  getUnreadNotifications,
  markNotificationAsRead,
  type NotificationItem,
  type NotificationListQuery,
  type NotificationPage,
} from '@/api/notifications';

export const notificationQueryKeys = {
  all: ['notifications'] as const,
  list: (query: NotificationListQuery) =>
    [...notificationQueryKeys.lists(), query.page ?? 0, query.size ?? 20] as const,
  lists: () => [...notificationQueryKeys.all, 'list'] as const,
  unreadCount: () => [...notificationQueryKeys.all, 'unreadCount'] as const,
  unreadList: () => [...notificationQueryKeys.all, 'unread'] as const,
};

export function notificationsQueryOptions(query: NotificationListQuery = {}) {
  return queryOptions({
    queryFn: () => getNotifications(query),
    queryKey: notificationQueryKeys.list(query),
    staleTime: 30 * 1000,
  });
}

export function useNotifications(query: NotificationListQuery = {}, enabled = true) {
  return useQuery({
    ...notificationsQueryOptions(query),
    enabled,
  });
}

export function useUnreadNotifications(enabled = true) {
  return useQuery({
    enabled,
    queryFn: getUnreadNotifications,
    queryKey: notificationQueryKeys.unreadList(),
    staleTime: 30 * 1000,
  });
}

export function useUnreadNotificationCount(enabled = true) {
  return useQuery({
    enabled,
    queryFn: getUnreadNotificationCount,
    queryKey: notificationQueryKeys.unreadCount(),
    staleTime: 30 * 1000,
  });
}

export function useMarkNotificationAsRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (notificationId: string) => markNotificationAsRead(notificationId),
    onSuccess: async (_, notificationId) => {
      // Optimistik veya anlık güncelleme için unreadCount azaltılabilir
      queryClient.setQueryData<number>(
        notificationQueryKeys.unreadCount(),
        (prev) => (typeof prev === 'number' && prev > 0 ? prev - 1 : 0),
      );

      // Listelerdeki ilgili bildirimi okundu olarak işaretle
      queryClient.setQueriesData<NotificationPage>(
        { queryKey: notificationQueryKeys.lists() },
        (oldData) => {
          if (!oldData) return oldData;
          return {
            ...oldData,
            content: oldData.content.map((item) =>
              item.id === notificationId ? { ...item, read: true } : item,
            ),
          };
        },
      );

      queryClient.setQueryData<NotificationItem[]>(
        notificationQueryKeys.unreadList(),
        (oldData) => {
          if (!oldData) return oldData;
          return oldData.filter((item) => item.id !== notificationId);
        },
      );

      await queryClient.invalidateQueries({ queryKey: notificationQueryKeys.all });
    },
  });
}
