import React from 'react';
import { TouchableOpacity, Text, Alert } from 'react-native';
import * as DocumentPicker from 'expo-document-picker';
import { validateFile } from '../../utils/fileValidators';
import { SelectedFile } from '../../services/files/uploadQueue';

interface FilePickerButtonProps {
  onFilesSelected: (files: SelectedFile[]) => void;
  multiple?: boolean;
  disabled?: boolean;
}

export const FilePickerButton: React.FC<FilePickerButtonProps> = ({
  onFilesSelected,
  multiple = true,
  disabled = false,
}) => {
  const handlePickDocument = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({
        multiple,
        copyToCacheDirectory: true,
      });

      if (result.canceled || !result.assets) {
        return;
      }

      const validFiles: SelectedFile[] = [];

      for (const asset of result.assets) {
        const fileToValidate = {
          name: asset.name,
          size: asset.size,
          mimeType: asset.mimeType,
          uri: asset.uri,
        };

        const validation = validateFile(fileToValidate);
        if (!validation.isValid) {
          Alert.alert('Geçersiz Dosya', `${asset.name}: ${validation.error}`);
          continue;
        }

        validFiles.push(fileToValidate);
      }

      if (validFiles.length > 0) {
        onFilesSelected(validFiles);
      }
    } catch {
      Alert.alert('Hata', 'Dosya seçilirken bir hata oluştu.');
    }
  };

  return (
    <TouchableOpacity
      onPress={handlePickDocument}
      disabled={disabled}
      activeOpacity={0.8}
      className={`border border-dashed border-blue-500 bg-blue-50 dark:bg-slate-800 p-4 rounded-xl items-center justify-center ${
        disabled ? 'opacity-50' : ''
      }`}
      accessibilityRole="button"
      accessibilityLabel="Cihazdan dosya seç"
    >
      <Text className="text-blue-600 dark:text-blue-400 font-semibold text-sm">
        + Dosya Ekle (Maks. 10 MB)
      </Text>
    </TouchableOpacity>
  );
};