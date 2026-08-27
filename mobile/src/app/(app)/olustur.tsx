import { useState } from 'react';
import { useRouter } from 'expo-router';
import { ActivityIndicator, Alert, Pressable, ScrollView, View } from 'react-native';

import { FilePickerButton } from '@/components/files/FilePickerButton';
import { RecordForm } from '@/components/records/RecordForm';
import { AppButton } from '@/components/ui/AppButton';
import { AppCard } from '@/components/ui/AppCard';
import { AppText } from '@/components/ui/AppText';
import { Screen } from '@/components/ui/Screen';
import { useCategories } from '@/query/categories';
import { useCurrentUser } from '@/query/currentUser';
import { useCreateRecord } from '@/query/records';
import {
  processUploadQueue,
  type SelectedFile,
  type UploadProgressItem,
} from '@/services/files/uploadQueue';
import { appTokens } from '@/theme/theme';
import { formatFileSize } from '@/utils/fileFormatters';

export default function CreateRecordScreen() {
  const router = useRouter();
  const categoriesQuery = useCategories();
  const currentUserQuery = useCurrentUser();
  const createMutation = useCreateRecord();
  const [selectedFiles, setSelectedFiles] = useState<SelectedFile[]>([]);
  const [uploadProgress, setUploadProgress] = useState<UploadProgressItem[]>([]);
  const [saving, setSaving] = useState(false);

  const addFiles = (files: SelectedFile[]) => {
    setSelectedFiles((currentFiles) => {
      const existingFiles = new Set(
        currentFiles.map((file) => `${file.name}-${file.size ?? 'unknown'}`),
      );
      const newFiles = files.filter(
        (file) => !existingFiles.has(`${file.name}-${file.size ?? 'unknown'}`),
      );

      return [...currentFiles, ...newFiles];
    });
  };

  const removeFile = (fileToRemove: SelectedFile) => {
    setSelectedFiles((currentFiles) =>
      currentFiles.filter((file) => file !== fileToRemove),
    );
  };

  const openRecord = (recordId: string) => {
    router.replace({
      params: { id: recordId },
      pathname: '/kayitlar/[id]',
    });
  };

  if (categoriesQuery.isPending || currentUserQuery.isPending) {
    return (
      <Screen className="items-center justify-center" edges={['left', 'right']}>
        <ActivityIndicator color={appTokens.brand[600]} size="large" />
        <AppText className="mt-3" tone="muted">
          Form hazırlanıyor…
        </AppText>
      </Screen>
    );
  }

  if (currentUserQuery.isError || currentUserQuery.data?.roleName !== 'CALISAN') {
    return (
      <Screen className="justify-center px-5" edges={['left', 'right']}>
        <AppCard className="gap-4">
          <AppText accessibilityRole="header" variant="heading">
            Yeni kayıt oluşturulamaz
          </AppText>
          <AppText tone="muted">
            Yeni kayıt oluşturma işlemi çalışan rolüne açıktır.
          </AppText>
          <AppButton
            label="Dashboard'a dön"
            onPress={() => router.replace('/')}
          />
        </AppCard>
      </Screen>
    );
  }

  return (
    <Screen edges={['left', 'right']}>
      <ScrollView
        contentContainerClassName="gap-5 px-5 py-5"
        keyboardShouldPersistTaps="handled"
      >
        <View className="gap-1">
          <AppText accessibilityRole="header" variant="title">
            Yeni kayıt
          </AppText>
          <AppText tone="muted">
            Bilgileri tamamlayıp kaydı taslak olarak oluşturun.
          </AppText>
        </View>

        {categoriesQuery.isError ? (
          <AppCard className="gap-3">
            <AppText tone="danger">Kategoriler yüklenemedi.</AppText>
            <AppText tone="muted">
              Kayıt oluşturabilmek için kategori listesinin yüklenmesi gerekir.
            </AppText>
            <AppButton
              label="Tekrar dene"
              onPress={() => void categoriesQuery.refetch()}
              variant="secondary"
            />
          </AppCard>
        ) : (
          <RecordForm
            categories={categoriesQuery.data ?? []}
            onSubmit={async (values) => {
              setSaving(true);
              try {
                const record = await createMutation.mutateAsync(values);

                if (selectedFiles.length === 0) {
                  openRecord(record.id);
                  return;
                }

                let finalProgress: UploadProgressItem[] = [];
                await processUploadQueue({
                  files: selectedFiles,
                  onProgress: (progress) => {
                    finalProgress = progress;
                    setUploadProgress(progress);
                  },
                  recordId: record.id,
                });

                const failedFileCount = finalProgress.filter(
                  (item) => item.status === 'error',
                ).length;

                if (failedFileCount > 0) {
                  Alert.alert(
                    'Taslak oluşturuldu',
                    `${failedFileCount} dosya yüklenemedi. Kayıt detayından tekrar deneyebilirsiniz.`,
                    [
                      {
                        onPress: () => openRecord(record.id),
                        text: 'Kayda git',
                      },
                    ],
                    { cancelable: false },
                  );
                  return;
                }

                openRecord(record.id);
              } finally {
                setSaving(false);
              }
            }}
            submitLabel="Taslağı oluştur"
          >
            <View className="gap-3 border-t border-app-border pt-5 dark:border-app-border-dark">
              <View className="gap-1">
                <AppText variant="label">Ek dosyalar</AppText>
                <AppText tone="muted" variant="caption">
                  PDF, Word, Excel, PNG veya JPEG · En fazla 10 MB
                </AppText>
              </View>

              <FilePickerButton
                disabled={saving}
                multiple
                onFilesSelected={addFiles}
              />

              {selectedFiles.length > 0 ? (
                <View
                  accessibilityLabel="Seçilen dosyalar"
                  accessibilityRole="list"
                  className="gap-2"
                >
                  {selectedFiles.map((file) => {
                    const progress = uploadProgress.find(
                      (item) => item.fileName === file.name,
                    );

                    return (
                      <View
                        className="flex-row items-center gap-3 rounded-app-md border border-app-border px-3 py-3 dark:border-app-border-dark"
                        key={`${file.name}-${file.size ?? 'unknown'}`}
                      >
                        <View className="min-w-0 flex-1 gap-0.5">
                          <AppText numberOfLines={1} variant="label">
                            {file.name}
                          </AppText>
                          <AppText tone="muted" variant="caption">
                            {progress?.status === 'uploading'
                              ? 'Yükleniyor…'
                              : formatFileSize(file.size)}
                          </AppText>
                        </View>
                        <Pressable
                          accessibilityLabel={`${file.name} dosyasını kaldır`}
                          accessibilityRole="button"
                          className="min-h-11 justify-center px-2"
                          disabled={saving}
                          onPress={() => removeFile(file)}
                        >
                          <AppText tone="danger" variant="label">
                            Kaldır
                          </AppText>
                        </Pressable>
                      </View>
                    );
                  })}
                </View>
              ) : null}
            </View>
          </RecordForm>
        )}
      </ScrollView>
    </Screen>
  );
}
