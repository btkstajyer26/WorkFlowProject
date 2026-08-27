import { useState } from 'react';
import { ActivityIndicator, Alert, View } from 'react-native';

import { ApiClientError } from '@/api/errors';
import { FileList } from '@/components/files/FileList';
import { FilePickerButton } from '@/components/files/FilePickerButton';
import { AppButton } from '@/components/ui/AppButton';
import { AppCard } from '@/components/ui/AppCard';
import { AppText } from '@/components/ui/AppText';
import {
  useDeleteFileMutation,
  useRecordFiles,
  useUploadFilesMutation,
} from '@/query/files';
import type {
  SelectedFile,
  UploadProgressItem,
} from '@/services/files/uploadQueue';
import { appTokens } from '@/theme/theme';

type RecordFilesIntegrationSlotProps = {
  canModify: boolean;
  recordId: string;
};

function errorMessage(error: unknown, fallback: string): string {
  return error instanceof ApiClientError ? error.message : fallback;
}

export function RecordFilesIntegrationSlot({
  canModify,
  recordId,
}: RecordFilesIntegrationSlotProps) {
  const filesQuery = useRecordFiles(recordId);
  const uploadMutation = useUploadFilesMutation(recordId);
  const deleteMutation = useDeleteFileMutation(recordId);
  const [actionError, setActionError] = useState('');
  const [uploadProgress, setUploadProgress] = useState<UploadProgressItem[]>([]);
  const failedUploads = uploadProgress.filter(
    (item) => item.status === 'error',
  );

  const uploadFiles = async (files: SelectedFile[]) => {
    setActionError('');
    setUploadProgress([]);

    try {
      await uploadMutation.mutateAsync({
        files,
        onProgress: setUploadProgress,
      });
    } catch (error) {
      setActionError(
        errorMessage(error, 'Dosyalar yüklenemedi. Lütfen tekrar deneyin.'),
      );
    }
  };

  const deleteFile = (fileId: string) => {
    Alert.alert(
      'Dosya silinsin mi?',
      'Dosya bu kayıttan kaldırılacak. Bu işlem geri alınamaz.',
      [
        { style: 'cancel', text: 'Vazgeç' },
        {
          onPress: () => {
            setActionError('');
            deleteMutation.mutate(fileId, {
              onError: (error) => {
                setActionError(
                  errorMessage(
                    error,
                    'Dosya silinemedi. Lütfen tekrar deneyin.',
                  ),
                );
              },
            });
          },
          style: 'destructive',
          text: 'Sil',
        },
      ],
    );
  };

  return (
    <AppCard className="gap-4">
      <View className="flex-row items-center justify-between gap-3">
        <View className="flex-1 gap-1">
          <AppText variant="heading">Ek dosyalar</AppText>
          <AppText tone="muted" variant="caption">
            {filesQuery.data?.length ?? 0} dosya
          </AppText>
        </View>
        {filesQuery.isFetching && !filesQuery.isPending ? (
          <ActivityIndicator color={appTokens.brand[600]} size="small" />
        ) : null}
      </View>

      {filesQuery.isPending ? (
        <View className="items-center gap-3 py-5">
          <ActivityIndicator color={appTokens.brand[600]} />
          <AppText tone="muted">Dosyalar yükleniyor…</AppText>
        </View>
      ) : filesQuery.isError && !filesQuery.data ? (
        <View className="gap-3">
          <AppText tone="danger">Dosyalar yüklenemedi.</AppText>
          <AppButton
            label="Tekrar dene"
            onPress={() => void filesQuery.refetch()}
            variant="secondary"
          />
        </View>
      ) : (
        <FileList
          canDelete={canModify && !deleteMutation.isPending}
          files={filesQuery.data ?? []}
          onDeleteFile={deleteFile}
        />
      )}

      {canModify && !filesQuery.isError ? (
        <View className="gap-2">
          <FilePickerButton
            disabled={uploadMutation.isPending || deleteMutation.isPending}
            multiple
            onFilesSelected={(files) => void uploadFiles(files)}
          />
          <AppText tone="muted" variant="caption">
            PDF, Word, Excel, PNG veya JPEG · En fazla 10 MB
          </AppText>
        </View>
      ) : null}

      {uploadMutation.isPending ? (
        <AppText accessibilityLiveRegion="polite" tone="muted">
          Dosyalar sırayla yükleniyor…
        </AppText>
      ) : null}

      {failedUploads.map((item, index) => (
        <AppText
          key={`${item.fileName}-${index}`}
          tone="danger"
          variant="caption"
        >
          {item.fileName}: {item.error ?? 'Dosya yüklenemedi.'}
        </AppText>
      ))}

      {actionError ? (
        <AppText accessibilityLiveRegion="polite" tone="danger">
          {actionError}
        </AppText>
      ) : null}
    </AppCard>
  );
}
