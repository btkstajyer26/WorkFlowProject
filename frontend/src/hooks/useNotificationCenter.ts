import {
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import {
  getUnreadNotificationCount,
  listNotifications,
  listUnreadNotifications,
  markNotificationAsRead,
  type NotificationListItem,
} from '../api/notifications'
import { queryKeys } from '../query/queryKeys'

const notificationPageSize = 20
const notificationRefetchInterval = 30_000

export type NotificationViewItem = {
  id: string
  recordId: string
  message: string
  isRead: boolean
  createdAt: string
}

function toViewItem(item: NotificationListItem): NotificationViewItem {
  return {
    id: item.id,
    recordId: item.recordId,
    message: item.message,
    isRead: item.read,
    createdAt: item.createdAt,
  }
}

export function useUnreadNotificationCount(enabled: boolean) {
  return useQuery({
    queryKey: queryKeys.notifications.unreadCount,
    queryFn: getUnreadNotificationCount,
    enabled,
    refetchInterval: notificationRefetchInterval,
  })
}

export function useNotificationCenter({
  enabled,
  unreadOnly,
}: {
  enabled: boolean
  unreadOnly: boolean
}) {
  const queryClient = useQueryClient()
  const allNotificationsQuery = useInfiniteQuery({
    queryKey: queryKeys.notifications.list(),
    queryFn: ({ pageParam }) => listNotifications({
      page: pageParam,
      size: notificationPageSize,
    }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => (
      lastPage.page + 1 < lastPage.totalPages ? lastPage.page + 1 : undefined
    ),
    enabled,
    refetchInterval: notificationRefetchInterval,
  })
  const unreadNotificationsQuery = useQuery({
    queryKey: queryKeys.notifications.unread,
    queryFn: listUnreadNotifications,
    enabled: enabled && unreadOnly,
    refetchInterval: notificationRefetchInterval,
  })
  const unreadCountQuery = useUnreadNotificationCount(enabled)
  const markReadMutation = useMutation({
    mutationFn: markNotificationAsRead,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.notifications.lists() }),
        queryClient.invalidateQueries({ queryKey: queryKeys.notifications.unread }),
        queryClient.invalidateQueries({ queryKey: queryKeys.notifications.unreadCount }),
      ])
    },
  })

  const allItems = allNotificationsQuery.data?.pages
    .flatMap((page) => page.content)
    .map(toViewItem) ?? []
  const unreadItems = unreadNotificationsQuery.data?.map(toViewItem) ?? []
  const activeQuery = unreadOnly ? unreadNotificationsQuery : allNotificationsQuery

  return {
    notifications: unreadOnly ? unreadItems : allItems,
    totalCount: allNotificationsQuery.data?.pages[0]?.totalElements ?? allItems.length,
    unreadCount: unreadCountQuery.data ?? 0,
    isPending: activeQuery.isPending || unreadCountQuery.isPending,
    isError: activeQuery.isError || unreadCountQuery.isError,
    error: activeQuery.error ?? unreadCountQuery.error,
    retry: async () => {
      await Promise.all([activeQuery.refetch(), unreadCountQuery.refetch()])
    },
    markRead: (notificationId: string) => markReadMutation.mutate(notificationId),
    markingId: markReadMutation.variables,
    markReadError: markReadMutation.error,
    hasNextPage: !unreadOnly && allNotificationsQuery.hasNextPage,
    isFetchingNextPage: allNotificationsQuery.isFetchingNextPage,
    fetchNextPage: () => allNotificationsQuery.fetchNextPage(),
  }
}
