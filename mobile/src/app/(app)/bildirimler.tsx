import { useRouter } from 'expo-router';
import CheckCircle2 from 'lucide-react-native/icons/circle-check-big';
import RotateCcw from 'lucide-react-native/icons/rotate-ccw';
import Send from 'lucide-react-native/icons/send';
import XCircle from 'lucide-react-native/icons/circle-x';
import { useCallback, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  Pressable,
  RefreshControl,
  TouchableOpacity,
  View,
} from 'react-native';

import type { NotificationItem, NotificationType } from '@/api/notifications';
import { EmptyState } from '@/components/states/EmptyState';
import { ErrorState } from '@/components/states/ErrorState';
import { LoadingState } from '@/components/states/LoadingState';
import { AppText } from '@/components/ui/AppText';
import { Screen } from '@/components/ui/Screen';
import {
  useInfiniteNotifications,
  useMarkNotificationAsRead,
  useUnreadNotifications,
} from '@/query/notifications';
import { appTokens } from '@/theme/theme';

type FilterType = 'all' | 'unread';

function getNotificationTypeMeta(type: NotificationType) {
  switch (type) {
    case 'RECORD_SUBMITTED':
      return {
        bgColor: 'bg-brand-50 dark:bg-brand-950/40',
        color: appTokens.brand[600],
        icon: Send,
        label: 'İncelemeye Sunuldu',
        textColor: 'text-brand-700 dark:text-brand-300',
      };
    case 'RECORD_FORWARDED':
      return {
        bgColor: 'bg-indigo-50 dark:bg-indigo-950/40',
        color: '#4f46e5',
        icon: Send,
        label: 'Onaya İletildi',
        textColor: 'text-indigo-700 dark:text-indigo-300',
      };
    case 'RECORD_APPROVED':
      return {
        bgColor: 'bg-emerald-50 dark:bg-emerald-950/40',
        color: '#059669',
        icon: CheckCircle2,
        label: 'Onaylandı',
        textColor: 'text-emerald-700 dark:text-emerald-300',
      };
    case 'RECORD_REJECTED':
      return {
        bgColor: 'bg-rose-50 dark:bg-rose-950/40',
        color: '#e11d48',
        icon: XCircle,
        label: 'Reddedildi',
        textColor: 'text-rose-700 dark:text-rose-300',
      };
    case 'RECORD_RETURNED':
      return {
        bgColor: 'bg-amber-50 dark:bg-amber-950/40',
        color: '#d97706',
        icon: RotateCcw,
        label: 'Düzeltme İsteği',
        textColor: 'text-amber-700 dark:text-amber-300',
      };
  }
}

function formatNotificationDate(dateString: string): string {
  const date = new Date(dateString);
  if (Number.isNaN(date.getTime())) return 'Tarih bilgisi yok';

  return new Intl.DateTimeFormat('tr-TR', {
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    month: 'short',
  }).format(date);
}

export default function NotificationsScreen() {
  const router = useRouter();
  const [filter, setFilter] = useState<FilterType>('all');
  const notificationsQuery = useInfiniteNotifications(20);
  const unreadNotificationsQuery = useUnreadNotifications();
  const markAsReadMutation = useMarkNotificationAsRead();

  const allItems = useMemo(
    () => notificationsQuery.data?.pages.flatMap((page) => page.content) ?? [],
    [notificationsQuery.data?.pages],
  );

  const unreadItems = useMemo(
    () => unreadNotificationsQuery.data ?? [],
    [unreadNotificationsQuery.data],
  );

  const items = useMemo(() => {
    if (filter === 'unread') {
      return unreadItems;
    }
    return allItems;
  }, [allItems, filter, unreadItems]);

  const totalCount = notificationsQuery.data?.pages[0]?.totalElements ?? 0;
  const unreadCount = unreadItems.length;

  const handleLoadMore = useCallback(() => {
    if (
      filter === 'all' &&
      notificationsQuery.hasNextPage &&
      !notificationsQuery.isFetchingNextPage
    ) {
      void notificationsQuery.fetchNextPage();
    }
  }, [filter, notificationsQuery]);

  const handleRefresh = useCallback(() => {
    void Promise.all([
      notificationsQuery.refetch(),
      unreadNotificationsQuery.refetch(),
    ]);
  }, [notificationsQuery, unreadNotificationsQuery]);

  const handleNotificationPress = useCallback(
    (item: NotificationItem) => {
      if (!item.read) {
        markAsReadMutation.mutate(item.id);
      }
      router.push(`/(app)/kayitlar/${item.recordId}`);
    },
    [markAsReadMutation, router],
  );

  if (notificationsQuery.isLoading) {
    return (
      <Screen>
        <LoadingState message="Bildirimler yükleniyor…" />
      </Screen>
    );
  }

  if (notificationsQuery.isError && !notificationsQuery.data) {
    return (
      <Screen>
        <ErrorState
          message="Bildirimler alınamadı. Lütfen tekrar deneyin."
          onRetry={() => void notificationsQuery.refetch()}
          title="Bir Hata Oluştu"
        />
      </Screen>
    );
  }

  if (
    filter === 'unread' &&
    unreadNotificationsQuery.isError &&
    !unreadNotificationsQuery.data
  ) {
    return (
      <Screen>
        <ErrorState
          message="Okunmamış bildirimler alınamadı. Lütfen tekrar deneyin."
          onRetry={() => void unreadNotificationsQuery.refetch()}
          title="Bir Hata Oluştu"
        />
      </Screen>
    );
  }

  return (
    <Screen className="p-4">
      {/* Header & Filter Tabs */}
      <View className="mb-3 gap-2.5">
        <View className="flex-row items-center justify-between">
          <AppText variant="title">Bildirimler</AppText>
        </View>

        <View className="flex-row gap-2">
          <Pressable
            accessibilityRole="tab"
            accessibilityState={{ selected: filter === 'all' }}
            className={`rounded-full px-3.5 py-1.5 ${
              filter === 'all'
                ? 'bg-brand-600 dark:bg-brand-500'
                : 'bg-slate-100 dark:bg-slate-800'
            }`}
            onPress={() => setFilter('all')}
          >
            <AppText
              className={`text-xs font-semibold ${
                filter === 'all'
                  ? 'text-white'
                  : 'text-slate-600 dark:text-slate-300'
              }`}
            >
              Tümü{' '}
              <AppText
                className={`text-xs ${
                  filter === 'all'
                    ? 'font-normal text-white/80'
                    : 'text-slate-400 dark:text-slate-500'
                }`}
              >
                ({totalCount})
              </AppText>
            </AppText>
          </Pressable>

          <Pressable
            accessibilityRole="tab"
            accessibilityState={{ selected: filter === 'unread' }}
            className={`rounded-full px-3.5 py-1.5 ${
              filter === 'unread'
                ? 'bg-brand-600 dark:bg-brand-500'
                : 'bg-slate-100 dark:bg-slate-800'
            }`}
            onPress={() => setFilter('unread')}
          >
            <AppText
              className={`text-xs font-semibold ${
                filter === 'unread'
                  ? 'text-white'
                  : 'text-slate-600 dark:text-slate-300'
              }`}
            >
              Okunmamış{' '}
              <AppText
                className={`text-xs ${
                  filter === 'unread'
                    ? 'font-normal text-white/80'
                    : 'text-slate-400 dark:text-slate-500'
                }`}
              >
                ({unreadCount})
              </AppText>
            </AppText>
          </Pressable>
        </View>
      </View>

      {/* Notifications List */}
      <FlatList
        contentContainerClassName={items.length === 0 ? 'flex-1' : 'gap-2 pb-6'}
        data={items}
        keyExtractor={(item) => item.id}
        ListEmptyComponent={
          <EmptyState
            message={
              filter === 'unread'
                ? 'Tüm bildirimlerinizi okudunuz.'
                : 'Henüz gelen bir bildirim bulunmuyor.'
            }
            title={
              filter === 'unread'
                ? 'Okunmamış Bildirim Yok'
                : 'Bildirim Bulunamadı'
            }
          />
        }
        ListFooterComponent={
          filter === 'all' && notificationsQuery.isFetchingNextPage ? (
            <View className="items-center py-4">
              <ActivityIndicator color={appTokens.brand[600]} size="small" />
            </View>
          ) : null
        }
        onEndReached={handleLoadMore}
        onEndReachedThreshold={0.35}
        refreshControl={
          <RefreshControl
            colors={[appTokens.brand[600]]}
            onRefresh={handleRefresh}
            refreshing={
              (notificationsQuery.isRefetching &&
                !notificationsQuery.isFetchingNextPage) ||
              unreadNotificationsQuery.isRefetching
            }
            tintColor={appTokens.brand[600]}
          />
        }
        renderItem={({ item }) => {
          const meta = getNotificationTypeMeta(item.notificationType);
          const Icon = meta.icon;

          return (
            <TouchableOpacity
              accessibilityHint="İlgili kaydın detayına yönlendirir"
              accessibilityLabel={`${meta.label}: ${item.message}`}
              accessibilityRole="button"
              activeOpacity={0.7}
              className={`flex-row items-start gap-3 rounded-2xl border p-3.5 ${
                item.read
                  ? 'border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900/30'
                  : 'border-brand-200/60 bg-brand-50/40 dark:border-slate-500/20 dark:bg-slate-400/30'
              }`}
              onPress={() => handleNotificationPress(item)}
            >
              {/* Type Icon */}
              <View className={`rounded-xl p-2.5 ${meta.bgColor}`}>
                <Icon color={meta.color} size={18} />
              </View>

              {/* Content */}
              <View className="flex-1 gap-1">
                <View className="flex-row items-center justify-between">
                  <AppText className={`text-xs font-semibold ${meta.textColor}`}>
                    {meta.label}
                  </AppText>
                  <AppText tone="muted" variant="caption">
                    {formatNotificationDate(item.createdAt)}
                  </AppText>
                </View>

                <AppText
                  className={item.read ? 'text-slate-600 dark:text-slate-300' : 'font-medium text-slate-900 dark:text-white'}
                  variant="body"
                >
                  {item.message}
                </AppText>
              </View>

              {/* Unread indicator dot */}
              {!item.read ? (
                <View className="mt-1.5 h-2 w-2 rounded-full bg-brand-600 dark:bg-brand-400" />
              ) : null}
            </TouchableOpacity>
          );
        }}
        showsVerticalScrollIndicator={false}
      />
    </Screen>
  );
}
