import { useCallback, useMemo, useState } from 'react';
import { useFocusEffect, useRouter } from 'expo-router';
import Files from 'lucide-react-native/icons/files';
import SlidersHorizontal from 'lucide-react-native/icons/sliders-horizontal';
import {
  ActivityIndicator,
  FlatList,
  Pressable,
  RefreshControl,
  View,
} from 'react-native';

import {
  defaultRecordListFilters,
  RecordFiltersPanel,
  type RecordListFilterValues,
  toApiDateRange,
} from '@/components/records/RecordFiltersPanel';
import { RecordListItem } from '@/components/records/RecordListItem';
import { AppButton } from '@/components/ui/AppButton';
import { AppCard } from '@/components/ui/AppCard';
import { AppText } from '@/components/ui/AppText';
import { Screen } from '@/components/ui/Screen';
import { useCategories } from '@/query/categories';
import { useInfiniteRecords } from '@/query/records';
import { useAppTheme } from '@/theme/ThemeProvider';
import { appTokens } from '@/theme/theme';

function countActiveFilters(filters: RecordListFilterValues) {
  return [
    filters.categoryId,
    filters.fromDate,
    filters.q,
    filters.status,
    filters.toDate,
  ].filter(Boolean).length;
}

export default function RecordsScreen() {
  const router = useRouter();
  const { colors } = useAppTheme();
  const [filtersVisible, setFiltersVisible] = useState(false);
  const [filters, setFilters] = useState(defaultRecordListFilters);
  const dateRange = useMemo(() => toApiDateRange(filters), [filters]);
  const categoriesQuery = useCategories();
  const recordsQuery = useInfiniteRecords({
    categoryId: filters.categoryId,
    from: dateRange.from,
    q: filters.q || undefined,
    size: 15,
    sort: filters.sort,
    status: filters.status,
    to: dateRange.to,
  });
  const records = useMemo(
    () => recordsQuery.data?.pages.flatMap((page) => page.content) ?? [],
    [recordsQuery.data],
  );
  const categoryNameById = useMemo(
    () =>
      new Map(
        (categoriesQuery.data ?? []).map((category) => [category.id, category.name]),
      ),
    [categoriesQuery.data],
  );
  const totalElements = recordsQuery.data?.pages[0]?.totalElements ?? 0;
  const activeFilterCount = countActiveFilters(filters);
  const fetchNextRecordsPage = recordsQuery.fetchNextPage;
  const refetchRecords = recordsQuery.refetch;
  const refreshing =
    recordsQuery.isRefetching && !recordsQuery.isFetchingNextPage;

  useFocusEffect(
    useCallback(() => {
      void refetchRecords();
    }, [refetchRecords]),
  );

  const refreshRecords = async () => {
    await Promise.all([refetchRecords(), categoriesQuery.refetch()]);
  };

  const clearFilters = () => {
    setFilters(defaultRecordListFilters);
    setFiltersVisible(false);
  };

  if (recordsQuery.isPending) {
    return (
      <Screen className="items-center justify-center" edges={['left', 'right']}>
        <ActivityIndicator color={appTokens.brand[600]} size="large" />
        <AppText className="mt-3" tone="muted">
          Kayıtlar yükleniyor…
        </AppText>
      </Screen>
    );
  }

  if (recordsQuery.isError) {
    return (
      <Screen className="justify-center px-5" edges={['left', 'right']}>
        <AppCard className="gap-4 p-5">
          <View className="items-center gap-2">
            <AppText accessibilityRole="header" variant="heading">
              Kayıtlar yüklenemedi
            </AppText>
            <AppText className="text-center" tone="muted">
              Bağlantınızı kontrol edip yeniden deneyin.
            </AppText>
          </View>
          <AppButton label="Tekrar dene" onPress={() => void recordsQuery.refetch()} />
        </AppCard>
      </Screen>
    );
  }

  return (
    <Screen edges={['left', 'right']}>
      <FlatList
        contentContainerClassName="px-5 py-5"
        data={records}
        ItemSeparatorComponent={() => <View className="h-3" />}
        keyExtractor={(record) => record.id}
        ListEmptyComponent={
          <AppCard className="items-center gap-3 px-5 py-8">
            <View className="size-12 items-center justify-center rounded-app-lg bg-brand-100 dark:bg-brand-900/40">
              <Files color={appTokens.brand[600]} size={23} />
            </View>
            <AppText className="text-center" variant="heading">
              {activeFilterCount > 0
                ? 'Filtrelere uygun kayıt bulunamadı'
                : 'Henüz görüntülenecek kayıt yok'}
            </AppText>
            <AppText className="text-center" tone="muted">
              {activeFilterCount > 0
                ? 'Filtreleri değiştirerek yeniden deneyebilirsiniz.'
                : 'Yetkiniz kapsamındaki kayıtlar burada listelenecek.'}
            </AppText>
            {activeFilterCount > 0 ? (
              <AppButton label="Filtreleri temizle" onPress={clearFilters} />
            ) : null}
          </AppCard>
        }
        ListFooterComponent={
          <View className="items-center py-5">
            {recordsQuery.isFetchingNextPage ? (
              <ActivityIndicator color={appTokens.brand[600]} />
            ) : null}
            {recordsQuery.isFetchNextPageError ? (
              <View className="gap-3">
                <AppText className="text-center" tone="danger">
                  Sonraki sayfa yüklenemedi.
                </AppText>
                <AppButton
                  label="Tekrar dene"
                  onPress={() => void fetchNextRecordsPage()}
                  variant="secondary"
                />
              </View>
            ) : null}
          </View>
        }
        ListHeaderComponent={
          <View className="mb-5 gap-4">
            <View className="flex-row items-start justify-between gap-3">
              <View className="min-w-0 flex-1 gap-1">
                <AppText accessibilityRole="header" variant="title">
                  Kayıtlar
                </AppText>
                <AppText tone="muted">
                  {totalElements} kayıt bulundu
                </AppText>
              </View>
              <Pressable
                accessibilityLabel={`Filtreler${
                  activeFilterCount ? `, ${activeFilterCount} etkin` : ''
                }`}
                accessibilityRole="button"
                className={`min-h-12 flex-row items-center gap-2 rounded-app-md border px-4 ${
                  activeFilterCount > 0
                    ? 'border-brand-600 bg-brand-100 dark:border-brand-400 dark:bg-brand-900/40'
                    : 'border-app-border bg-app-surface dark:border-app-border-dark dark:bg-app-surface-dark'
                }`}
                onPress={() => setFiltersVisible((visible) => !visible)}
              >
                <SlidersHorizontal color={appTokens.brand[600]} size={18} />
                <AppText tone={activeFilterCount > 0 ? 'brand' : 'muted'} variant="label">
                  Filtrele{activeFilterCount > 0 ? ` (${activeFilterCount})` : ''}
                </AppText>
              </Pressable>
            </View>

            {categoriesQuery.isError ? (
              <AppCard className="gap-3 border-amber-200 bg-amber-50 p-4 dark:border-amber-900/70 dark:bg-amber-950/40">
                <AppText variant="label">Kategoriler alınamadı</AppText>
                <AppText tone="muted">
                  Kayıtlar kategori numarasıyla gösterilecek.
                </AppText>
                <AppButton
                  label="Kategorileri yenile"
                  onPress={() => void categoriesQuery.refetch()}
                  variant="secondary"
                />
              </AppCard>
            ) : null}

            {filtersVisible ? (
              <RecordFiltersPanel
                categories={categoriesQuery.data ?? []}
                initialValues={filters}
                onApply={(nextFilters) => {
                  setFilters(nextFilters);
                  setFiltersVisible(false);
                }}
                onClear={clearFilters}
              />
            ) : null}
          </View>
        }
        onEndReached={() => {
          if (recordsQuery.hasNextPage && !recordsQuery.isFetchingNextPage) {
            void recordsQuery.fetchNextPage();
          }
        }}
        onEndReachedThreshold={0.4}
        refreshControl={
          <RefreshControl
            onRefresh={() => void refreshRecords()}
            refreshing={refreshing}
            tintColor={colors.textMuted}
          />
        }
        renderItem={({ item }) => (
          <RecordListItem
            categoryName={
              categoryNameById.get(item.categoryId) ?? `Kategori #${item.categoryId}`
            }
            onPress={() =>
              router.push({
                params: { id: item.id },
                pathname: '/kayitlar/[id]',
              })
            }
            record={item}
          />
        )}
      />
    </Screen>
  );
}
