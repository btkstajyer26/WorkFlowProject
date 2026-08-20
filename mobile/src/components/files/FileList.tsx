import React from 'react';
import { View, Text, TouchableOpacity, Alert } from 'react-native';
import { RecordFile, downloadFileToLocal } from '../../api/files';
import { formatFileSize } from '../../utils/fileFormatters';
import { EmptyState } from '../states/EmptyState';

interface FileListProps {
  files: RecordFile[];
  onDeleteFile?: (fileId: string) => void;
  canDelete?: boolean;
}

export const FileList: React.FC<FileListProps> = ({
  files,
  onDeleteFile,
  canDelete = false,
}) => {
  if (!files || files.length === 0) {
    return <EmptyState title="Dosya Yok" message="Bu kayda henüz bir dosya eklenmemiş." />;
  }

  const handleDownload = async (file: RecordFile) => {
    try {
await downloadFileToLocal(file.id, file.name);
      Alert.alert('Başarılı', `Dosya indirildi: ${file.name}`);
    } catch (error: any) {
      Alert.alert('İndirme Hatası', error?.message || 'Dosya indirilemedi.');
    }
  };

  return (
    <View className="space-y-2">
      {files.map((file) => (
        <View
          key={file.id}
          className="flex-row items-center justify-between p-3 bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-lg"
        >
          <View className="flex-1 mr-3">
            <Text
              numberOfLines={1}
              className="text-sm font-medium text-gray-800 dark:text-gray-100"
            >
              {file.name}
            </Text>
            <Text className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
              {formatFileSize(file.size)}
            </Text>
          </View>

          <View className="flex-row items-center space-x-2">
            <TouchableOpacity
              onPress={() => handleDownload(file)}
              className="bg-gray-100 dark:bg-slate-800 px-3 py-1.5 rounded-md"
              accessibilityRole="button"
              accessibilityLabel={`${file.name} dosyasını indir`}
            >
              <Text className="text-xs font-semibold text-gray-700 dark:text-gray-300">İndir</Text>
            </TouchableOpacity>

            {canDelete && onDeleteFile && (
              <TouchableOpacity
                onPress={() => onDeleteFile(file.id)}
                className="bg-red-50 dark:bg-red-950/40 px-3 py-1.5 rounded-md"
                accessibilityRole="button"
                accessibilityLabel={`${file.name} dosyasını sil`}
              >
                <Text className="text-xs font-semibold text-red-600 dark:text-red-400">Sil</Text>
              </TouchableOpacity>
            )}
          </View>
        </View>
      ))}
    </View>
  );
};