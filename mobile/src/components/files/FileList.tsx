import { ExternalLink, Trash2 } from 'lucide-react-native';
import { useState } from 'react';
import { ActivityIndicator, Alert, TouchableOpacity, View } from 'react-native';

import { downloadAndOpenFile, type RecordFile } from '@/api/files';
import { EmptyState } from '@/components/states/EmptyState';
import { AppText } from '@/components/ui/AppText';
import { formatFileSize } from '@/utils/fileFormatters';

interface FileListProps {
  canDelete?: boolean;
  files: RecordFile[];
  onDeleteFile?: (fileId: string) => void;
}

export function FileList({
  canDelete = false,
  files,
  onDeleteFile,
}: FileListProps) {
  const [openingFileId, setOpeningFileId] = useState<string | null>(null);

  if (!files || files.length === 0) {
    return (
      <EmptyState
        message="Bu kayda henüz bir dosya eklenmemiş."
        title="Dosya Yok"
      />
    );
  }

  const handleOpenOrShare = async (file: RecordFile) => {
    setOpeningFileId(file.id);
    try {
      const { shared } = await downloadAndOpenFile(
        file.id,
        file.originalName,
        file.mimeType,
      );
      if (!shared) {
        Alert.alert('İndirildi', `${file.originalName} cihazınıza indirildi.`);
      }
    } catch (error: unknown) {
      Alert.alert(
        'Dosya Açılamadı',
        error instanceof Error ? error.message : 'Dosya indirilirken bir hata oluştu.',
      );
    } finally {
      setOpeningFileId(null);
    }
  };

  return (
    <View className="gap-2">
      {files.map((file) => {
        const isOpening = openingFileId === file.id;

        return (
          <View
            className="flex-row items-center justify-between rounded-xl border border-slate-200 bg-white p-3 dark:border-slate-800 dark:bg-slate-900"
            key={file.id}
          >
            <View className="mr-3 flex-1">
              <AppText numberOfLines={1} variant="body">
                {file.originalName}
              </AppText>
              <AppText className="mt-0.5" tone="muted" variant="caption">
                {formatFileSize(file.fileSize)}
              </AppText>
            </View>

            <View className="flex-row items-center gap-2">
              <TouchableOpacity
                accessibilityHint="Dosyayı cihazda açar veya paylaşır"
                accessibilityLabel={`${file.originalName} dosyasını aç veya paylaş`}
                accessibilityRole="button"
                className="flex-row items-center gap-1.5 rounded-lg bg-slate-100 px-3 py-2 dark:bg-slate-800"
                disabled={isOpening}
                onPress={() => void handleOpenOrShare(file)}
              >
                {isOpening ? (
                  <ActivityIndicator size="small" />
                ) : (
                  <>
                    <ExternalLink color="#475569" size={14} />
                    <AppText className="font-semibold text-slate-700 dark:text-slate-200" variant="caption">
                      Aç
                    </AppText>
                  </>
                )}
              </TouchableOpacity>

              {canDelete && onDeleteFile ? (
                <TouchableOpacity
                  accessibilityHint="Dosyayı bu kayıttan siler"
                  accessibilityLabel={`${file.originalName} dosyasını sil`}
                  accessibilityRole="button"
                  className="rounded-lg bg-rose-50 p-2 dark:bg-rose-950/40"
                  disabled={isOpening}
                  onPress={() => onDeleteFile(file.id)}
                >
                  <Trash2 color="#e11d48" size={16} />
                </TouchableOpacity>
              ) : null}
            </View>
          </View>
        );
      })}
    </View>
  );
}

