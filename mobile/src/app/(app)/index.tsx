import { useMemo } from 'react';
import { useRouter } from 'expo-router';
import CheckCircle2 from 'lucide-react-native/icons/circle-check-big';
import Clock3 from 'lucide-react-native/icons/clock-3';
import FilePenLine from 'lucide-react-native/icons/file-pen-line';
import Files from 'lucide-react-native/icons/files';
import type { LucideIcon } from 'lucide-react-native';
import {
  ActivityIndicator,
  Pressable,
  RefreshControl,
  ScrollView,
  View,
} from 'react-native';

import type { RecordStatus } from '@/api/records';
import type { UserRole } from '@/api/users';
import {
  DashboardSummaryCard,
  type SummaryTone,
} from '@/components/records/DashboardSummaryCard';
import { RecordStatusBadge } from '@/components/records/RecordStatusBadge';
import { AppButton } from '@/components/ui/AppButton';
import { AppCard } from '@/components/ui/AppCard';
import { AppText } from '@/components/ui/AppText';
import { Screen } from '@/components/ui/Screen';
import { userRoleLabels } from '@/constants/userRoles';
import { useCurrentUser } from '@/query/currentUser';
import { useRecordCounts, useRecords } from '@/query/records';
import { useAppTheme } from '@/theme/ThemeProvider';
import { appTokens } from '@/theme/theme';

type DashboardCardConfig = {
  icon: LucideIcon;
  label: string;
  statuses: RecordStatus[];
  tone: SummaryTone;
};

const dashboardCards: Record<UserRole, DashboardCardConfig[]> = {
  ADMIN: [],
  BASKAN: [
    {
      icon: Clock3,
      label: 'Onay bekleyenler',
      statuses: ['BASKAN_INCELEMESINDE'],
      tone: 'warning',
    },
    {
      icon: CheckCircle2,
      label: 'Onaylananlar',
      statuses: ['ONAYLANDI'],
      tone: 'success',
    },
    {
      icon: FilePenLine,
      label: 'Reddedilenler',
      statuses: ['REDDEDILDI'],
      tone: 'danger',
    },
    {
      icon: Files,
      label: 'Toplam sonuçlanan',
      statuses: ['ONAYLANDI', 'REDDEDILDI'],
      tone: 'brand',
    },
  ],
  BASKAN_YARDIMCISI: [
    {
      icon: Files,
      label: 'İncelenecekler',
      statuses: ['BSK_YRD_INCELEMESINDE'],
      tone: 'info',
    },
    {
      icon: Clock3,
      label: 'Başkan incelemesinde',
      statuses: ['BASKAN_INCELEMESINDE'],
      tone: 'warning',
    },
    {
      icon: FilePenLine,
      label: 'Düzeltmede olanlar',
      statuses: ['DUZENLEME_BEKLIYOR'],
      tone: 'brand',
    },
    {
      icon: CheckCircle2,
      label: 'Sonuçlananlar',
      statuses: ['ONAYLANDI', 'REDDEDILDI'],
      tone: 'success',
    },
  ],
  CALISAN: [
    {
      icon: Files,
      label: 'Taslaklarım',
      statuses: ['TASLAK'],
      tone: 'brand',
    },
    {
      icon: FilePenLine,
      label: 'Düzeltme bekleyen',
      statuses: ['DUZENLEME_BEKLIYOR'],
      tone: 'warning',
    },
    {
      icon: Clock3,
      label: 'Onay aşamasında',
      statuses: ['BSK_YRD_INCELEMESINDE', 'BASKAN_INCELEMESINDE'],
      tone: 'info',
    },
    {
      icon: CheckCircle2,
      label: 'Sonuçlananlar',
      statuses: ['ONAYLANDI', 'REDDEDILDI'],
      tone: 'success',
    },
  ],
};

function formatRecordDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Tarih bilgisi yok';

  return new Intl.DateTimeFormat('tr-TR', {
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    month: 'short',
  }).format(date);
}

export default function DashboardScreen() {
  const router = useRouter();
  const { colors } = useAppTheme();
  const currentUser = useCurrentUser();
  const role = currentUser.data?.roleName;
  const cards = useMemo(() => (role ? dashboardCards[role] : []), [role]);
  const dashboardStatuses = useMemo(
    () => [...new Set(cards.flatMap((card) => card.statuses))],
    [cards],
  );
  const recordsEnabled = currentUser.isSuccess && role !== 'ADMIN';
  const countQueries = useRecordCounts(dashboardStatuses, recordsEnabled);
  const recentRecordsQuery = useRecords(
    { page: 0, size: 3, sort: 'createdAt,desc' },
    recordsEnabled,
  );

  if (currentUser.isPending) {
    return (
      <Screen className="items-center justify-center" edges={['left', 'right']}>
        <ActivityIndicator color={appTokens.brand[600]} size="large" />
        <AppText className="mt-3" tone="muted">
          Paneliniz hazırlanıyor…
        </AppText>
      </Screen>
    );
  }

  if (currentUser.isError) {
    return (
      <Screen className="justify-center px-5" edges={['left', 'right']}>
        <AppCard className="gap-4 p-5">
          <View className="items-center gap-2">
            <AppText accessibilityRole="header" variant="heading">
              Panel yüklenemedi
            </AppText>
            <AppText className="text-center" tone="muted">
              Kullanıcı bilgileriniz alınamadı. Bağlantınızı kontrol edip yeniden
              deneyin.
            </AppText>
          </View>
          <AppButton label="Tekrar dene" onPress={() => void currentUser.refetch()} />
        </AppCard>
      </Screen>
    );
  }

  const user = currentUser.data;

  if (user.roleName === 'ADMIN') {
    return (
      <Screen className="justify-center px-5" edges={['left', 'right']}>
        <AppCard className="gap-2 p-5">
          <AppText accessibilityRole="header" variant="heading">
            Mobil panel kullanılamıyor
          </AppText>
          <AppText tone="muted">
            Admin rolü mobil uygulamanın ilk sürümünde desteklenmiyor.
          </AppText>
        </AppCard>
      </Screen>
    );
  }

  const countByStatus = new Map(
    dashboardStatuses.map((status, index) => [
      status,
      countQueries[index]?.data ?? 0,
    ]),
  );
  const countsPending = countQueries.some((query) => query.isPending);
  const dashboardError =
    countQueries.some((query) => query.isError) || recentRecordsQuery.isError;
  const recentRecords = recentRecordsQuery.data?.content ?? [];
  const refreshing =
    currentUser.isRefetching ||
    countQueries.some((query) => query.isRefetching) ||
    recentRecordsQuery.isRefetching;

  const refreshDashboard = async () => {
    await Promise.all([
      currentUser.refetch(),
      recentRecordsQuery.refetch(),
      ...countQueries.map((query) => query.refetch()),
    ]);
  };

  return (
    <Screen edges={['left', 'right']}>
      <ScrollView
        contentContainerClassName="gap-5 px-5 py-5"
        refreshControl={
          <RefreshControl
            onRefresh={() => void refreshDashboard()}
            refreshing={refreshing}
            tintColor={colors.textMuted}
          />
        }
      >
        <View className="gap-1">
          <AppText accessibilityRole="header" variant="title">
            Hoş geldiniz, {user.firstName}
          </AppText>
          <AppText tone="muted">
            {userRoleLabels[user.roleName]} olarak kayıt süreçlerinizi buradan takip
            edebilirsiniz.
          </AppText>
        </View>

        <View accessibilityLabel="Kayıt özeti" className="flex-row flex-wrap gap-3">
          {cards.map((card) => (
            <DashboardSummaryCard
              icon={card.icon}
              isLoading={countsPending}
              key={card.label}
              label={card.label}
              onPress={() => router.push('/kayitlar')}
              tone={card.tone}
              value={card.statuses.reduce(
                (total, status) => total + (countByStatus.get(status) ?? 0),
                0,
              )}
            />
          ))}
        </View>

        {dashboardError ? (
          <AppCard className="gap-3 border-rose-200 bg-rose-50 p-4 dark:border-rose-900/70 dark:bg-rose-950/40">
            <AppText tone="danger" variant="label">
              Kayıt özeti yenilenemedi
            </AppText>
            <AppText tone="muted">
              Bağlantınızı kontrol edip yeniden deneyin. Mevcut veriler ekranda
              kalmaya devam eder.
            </AppText>
            <AppButton
              label="Yeniden dene"
              onPress={() => void refreshDashboard()}
              variant="secondary"
            />
          </AppCard>
        ) : null}

        <AppCard className="overflow-hidden p-0">
          <View className="flex-row items-center justify-between border-b border-app-border-subtle px-4 py-4 dark:border-app-border-dark">
            <View className="min-w-0 flex-1 pr-3">
              <AppText variant="heading">Son kayıtlar</AppText>
              <AppText tone="muted" variant="caption">
                Erişim kapsamınızdaki en yeni kayıtlar
              </AppText>
            </View>
            <Pressable
              accessibilityRole="button"
              hitSlop={8}
              onPress={() => router.push('/kayitlar')}
            >
              <AppText tone="brand" variant="label">
                Tümünü gör
              </AppText>
            </Pressable>
          </View>

          {recentRecordsQuery.isPending ? (
            <View className="items-center gap-2 px-4 py-8">
              <ActivityIndicator color={appTokens.brand[600]} />
              <AppText tone="muted">Son kayıtlar yükleniyor…</AppText>
            </View>
          ) : null}

          {!recentRecordsQuery.isPending &&
          !recentRecordsQuery.isError &&
          recentRecords.length === 0 ? (
            <View className="items-center gap-3 px-5 py-8">
              <View className="size-12 items-center justify-center rounded-app-lg bg-brand-100 dark:bg-brand-900/40">
                <Files color={appTokens.brand[600]} size={23} />
              </View>
              <AppText className="text-center" variant="label">
                Henüz görüntülenecek kayıt yok
              </AppText>
              <AppText className="text-center" tone="muted">
                Yetkiniz kapsamındaki kayıtlar oluşturulduğunda burada görünecek.
              </AppText>
              {user.roleName === 'CALISAN' ? (
                <AppButton
                  label="Yeni kayıt oluştur"
                  onPress={() => router.push('/olustur')}
                  variant="secondary"
                />
              ) : null}
            </View>
          ) : null}

          {recentRecords.map((record, index) => (
            <Pressable
              accessibilityHint="Kayıt detayını açar"
              accessibilityLabel={record.title}
              accessibilityRole="button"
              className={`gap-2 px-4 py-4 active:bg-app-surface-muted dark:active:bg-app-surface-muted-dark ${
                index > 0
                  ? 'border-t border-app-border-subtle dark:border-app-border-dark'
                  : ''
              }`}
              key={record.id}
              onPress={() =>
                router.push({
                  params: { id: record.id },
                  pathname: '/kayitlar/[id]',
                })
              }
            >
              <View className="flex-row items-start justify-between gap-3">
                <AppText className="min-w-0 flex-1" numberOfLines={2} variant="label">
                  {record.title}
                </AppText>
                <RecordStatusBadge status={record.status} />
              </View>
              <View className="flex-row items-center justify-between gap-3">
                <AppText className="min-w-0 flex-1" numberOfLines={1} tone="muted" variant="caption">
                  {record.createdByFullName?.trim() || record.createdBy}
                </AppText>
                <AppText tone="muted" variant="caption">
                  {formatRecordDate(record.createdAt)}
                </AppText>
              </View>
            </Pressable>
          ))}
        </AppCard>
      </ScrollView>
    </Screen>
  );
}
