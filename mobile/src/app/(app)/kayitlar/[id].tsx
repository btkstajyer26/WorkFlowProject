import { useState } from 'react';
import { useLocalSearchParams, useRouter } from 'expo-router';
import ArrowLeft from 'lucide-react-native/icons/arrow-left';
import {
  ActivityIndicator,
  Modal,
  Pressable,
  RefreshControl,
  ScrollView,
  View,
} from 'react-native';

import { ApiClientError } from '@/api/errors';
import { RecordFilesIntegrationSlot } from '@/components/records/RecordFilesIntegrationSlot';
import { RecordForm } from '@/components/records/RecordForm';
import { RecordHistory } from '@/components/records/RecordHistory';
import { RecordStatusBadge } from '@/components/records/RecordStatusBadge';
import { RecordWorkflowActions } from '@/components/records/RecordWorkflowActions';
import { AppButton } from '@/components/ui/AppButton';
import { AppCard } from '@/components/ui/AppCard';
import { AppText } from '@/components/ui/AppText';
import { Screen } from '@/components/ui/Screen';
import { useRecordAuditLogs } from '@/query/auditLogs';
import { useCategories } from '@/query/categories';
import { useCurrentUser } from '@/query/currentUser';
import { useDeleteRecord, useRecord, useUpdateRecord } from '@/query/records';
import { useAppTheme } from '@/theme/ThemeProvider';
import { appTokens } from '@/theme/theme';

function formatDate(value: string) {
  return new Intl.DateTimeFormat('tr-TR', {
    dateStyle: 'long',
    timeStyle: 'short',
  }).format(new Date(value));
}

export default function RecordDetailScreen() {
  const router = useRouter();
  const { colors } = useAppTheme();
  const { id } = useLocalSearchParams<{ id?: string | string[] }>();
  const recordId = Array.isArray(id) ? id[0] : (id ?? '');
  const [editing, setEditing] = useState(false);
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [deleteError, setDeleteError] = useState('');
  const recordQuery = useRecord(recordId);
  const historyQuery = useRecordAuditLogs(recordId, Boolean(recordId));
  const categoriesQuery = useCategories();
  const currentUserQuery = useCurrentUser();
  const updateMutation = useUpdateRecord(recordId);
  const deleteMutation = useDeleteRecord(recordId);
  const record = recordQuery.data;
  const currentUser = currentUserQuery.data;
  const categoryName = categoriesQuery.data?.find(
    (category) => category.id === record?.categoryId,
  )?.name;
  const canEdit = Boolean(
    record &&
      currentUser?.roleName === 'CALISAN' &&
      currentUser.id === record.createdBy &&
      (record.status === 'TASLAK' || record.status === 'DUZENLEME_BEKLIYOR'),
  );
  const canDelete = Boolean(
    record &&
      currentUser?.roleName === 'CALISAN' &&
      currentUser.id === record.createdBy &&
      record.status === 'TASLAK',
  );
  const refreshing =
    recordQuery.isRefetching ||
    historyQuery.isRefetching ||
    categoriesQuery.isRefetching;

  const refresh = async () => {
    await Promise.all([
      recordQuery.refetch(),
      historyQuery.refetch(),
      categoriesQuery.refetch(),
      currentUserQuery.refetch(),
    ]);
  };

  const confirmDelete = async () => {
    try {
      setDeleteError('');
      await deleteMutation.mutateAsync();
      setDeleteModalVisible(false);
      router.replace('/kayitlar');
    } catch (error) {
      setDeleteError(
        error instanceof ApiClientError
          ? error.message
          : 'Kayıt silinemedi. Lütfen tekrar deneyin.',
      );
    }
  };

  if (!recordId) {
    return (
      <Screen className="justify-center px-5" edges={['left', 'right']}>
        <AppCard className="gap-4">
          <AppText variant="heading">Geçersiz kayıt bağlantısı</AppText>
          <AppButton
            label="Kayıtlara dön"
            onPress={() => router.replace('/kayitlar')}
          />
        </AppCard>
      </Screen>
    );
  }

  if (recordQuery.isPending || currentUserQuery.isPending) {
    return (
      <Screen className="items-center justify-center" edges={['left', 'right']}>
        <ActivityIndicator color={appTokens.brand[600]} size="large" />
        <AppText className="mt-3" tone="muted">
          Kayıt yükleniyor…
        </AppText>
      </Screen>
    );
  }

  if (recordQuery.isError || currentUserQuery.isError || !record || !currentUser) {
    return (
      <Screen className="justify-center px-5" edges={['left', 'right']}>
        <AppCard className="gap-4">
          <AppText variant="heading">Kayıt görüntülenemedi</AppText>
          <AppText tone="muted">
            Kayıt bulunamadı veya bu kaydı görüntüleme yetkiniz yok.
          </AppText>
          <AppButton label="Tekrar dene" onPress={() => void refresh()} />
          <AppButton
            label="Kayıtlara dön"
            onPress={() => router.back()}
            variant="secondary"
          />
        </AppCard>
      </Screen>
    );
  }

  return (
    <Screen edges={['left', 'right']}>
      <ScrollView
        contentContainerClassName="gap-5 px-5 py-5"
        refreshControl={
          <RefreshControl
            onRefresh={() => void refresh()}
            refreshing={refreshing}
            tintColor={colors.textMuted}
          />
        }
      >
        <Pressable
          accessibilityLabel="Kayıtlara dön"
          accessibilityRole="button"
          className="min-h-11 flex-row items-center gap-2 self-start"
          onPress={() => router.back()}
        >
          <ArrowLeft color={colors.textMuted} size={20} />
          <AppText tone="muted" variant="label">
            Kayıtlara dön
          </AppText>
        </Pressable>

        {editing ? (
          <View className="gap-3">
            <AppText accessibilityRole="header" variant="title">
              Kaydı düzenle
            </AppText>
            <RecordForm
              categories={categoriesQuery.data ?? []}
              initialValues={{
                categoryId: record.categoryId,
                description: record.description,
                title: record.title,
              }}
              onCancel={() => setEditing(false)}
              onSubmit={async (values) => {
                await updateMutation.mutateAsync(values);
                setEditing(false);
              }}
              submitLabel="Değişiklikleri kaydet"
            />
          </View>
        ) : (
          <>
            <AppCard className="gap-4 p-5">
              <View className="gap-3">
                <RecordStatusBadge status={record.status} />
                <AppText accessibilityRole="header" variant="title">
                  {record.title}
                </AppText>
              </View>
              <View className="gap-1">
                <AppText tone="muted" variant="caption">
                  Kategori
                </AppText>
                <AppText>{categoryName ?? `Kategori #${record.categoryId}`}</AppText>
              </View>
              <View className="gap-1">
                <AppText tone="muted" variant="caption">
                  Oluşturan
                </AppText>
                <AppText>{record.createdByFullName ?? record.createdBy}</AppText>
              </View>
              <View className="gap-1">
                <AppText tone="muted" variant="caption">
                  Oluşturulma tarihi
                </AppText>
                <AppText>{formatDate(record.createdAt)}</AppText>
              </View>
              <View className="gap-1">
                <AppText tone="muted" variant="caption">
                  Açıklama
                </AppText>
                <AppText>{record.description}</AppText>
              </View>
            </AppCard>

            {canEdit || canDelete ? (
              <AppCard className="gap-3">
                <AppText variant="heading">Kayıt yönetimi</AppText>
                {canEdit ? (
                  <AppButton
                    label="Kaydı düzenle"
                    onPress={() => setEditing(true)}
                  />
                ) : null}
                {canDelete ? (
                  <AppButton
                    label="Taslağı sil"
                    onPress={() => setDeleteModalVisible(true)}
                    variant="secondary"
                  />
                ) : null}
              </AppCard>
            ) : null}

            <RecordWorkflowActions record={record} user={currentUser} />
            <RecordFilesIntegrationSlot recordId={record.id} />

            {historyQuery.isPending ? (
              <AppCard className="items-center gap-3">
                <ActivityIndicator color={appTokens.brand[600]} />
                <AppText tone="muted">İşlem geçmişi yükleniyor…</AppText>
              </AppCard>
            ) : historyQuery.isError ? (
              <AppCard className="gap-3">
                <AppText tone="danger">İşlem geçmişi yüklenemedi.</AppText>
                <AppButton
                  label="Geçmişi yeniden yükle"
                  onPress={() => void historyQuery.refetch()}
                  variant="secondary"
                />
              </AppCard>
            ) : (
              <RecordHistory logs={historyQuery.data ?? []} />
            )}
          </>
        )}
      </ScrollView>

      <Modal
        animationType="fade"
        onRequestClose={() => setDeleteModalVisible(false)}
        transparent
        visible={deleteModalVisible}
      >
        <View className="flex-1 justify-end bg-black/50 p-5">
          <Pressable
            className="absolute inset-0"
            onPress={() => setDeleteModalVisible(false)}
          />
          <AppCard className="gap-4 p-5">
            <AppText variant="heading">Taslak silinsin mi?</AppText>
            <AppText tone="muted">
              Bu işlem geri alınamaz. Yalnızca taslak durumundaki kendi
              kayıtlarınızı silebilirsiniz.
            </AppText>
            {deleteError ? <AppText tone="danger">{deleteError}</AppText> : null}
            <AppButton
              isLoading={deleteMutation.isPending}
              label="Taslağı sil"
              onPress={() => void confirmDelete()}
            />
            <AppButton
              disabled={deleteMutation.isPending}
              label="Vazgeç"
              onPress={() => setDeleteModalVisible(false)}
              variant="secondary"
            />
          </AppCard>
        </View>
      </Modal>
    </Screen>
  );
}
