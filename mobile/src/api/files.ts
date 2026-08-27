import * as FileSystem from 'expo-file-system/legacy';
import * as IntentLauncher from 'expo-intent-launcher';
import * as Sharing from 'expo-sharing';
import { Platform } from 'react-native';

import {
  API_BASE_URL,
  apiAuthenticatedOperation,
  apiRequest,
} from './client';

export interface RecordFile {
  id: string;
  originalName: string;
  fileSize: number;
  mimeType: string;
  uploadedAt: string;
  uploadedBy: string;
  recordId: string;
}

export const getRecordFiles = async (recordId: string): Promise<RecordFile[]> => {
  return apiRequest<RecordFile[]>(`/api/records/${recordId}/files`);
};

export const uploadRecordFile = async (
  recordId: string,
  file: { uri: string; name: string; mimeType?: string },
): Promise<RecordFile> => {
  const formData = new FormData();

  formData.append('file', {
    uri: file.uri,
    name: file.name,
    type: file.mimeType || 'application/octet-stream',
  } as never);

  const uploadedFiles = await apiRequest<RecordFile[]>(`/api/records/${recordId}/files`, {
    method: 'POST',
    body: formData,
  });

  const uploadedFile = uploadedFiles[0];
  if (!uploadedFile) {
    throw new Error('Sunucu yüklenen dosya bilgisini döndürmedi.');
  }

  return uploadedFile;
};

export const deleteRecordFile = async (fileId: string): Promise<void> => {
  await apiRequest<void>(`/api/files/${fileId}`, { method: 'DELETE' });
};

export const downloadFileToLocal = async (
  fileId: string,
  fileName: string,
): Promise<string> => {
  const targetDir = FileSystem.documentDirectory || FileSystem.cacheDirectory || '';
  const safeFileName = fileName.replace(/[\\/:*?"<>|]/g, '_');
  const fileUri = `${targetDir}${safeFileName}`;

  const downloadRes = await apiAuthenticatedOperation((accessToken) =>
    FileSystem.downloadAsync(
      `${API_BASE_URL}/api/files/${fileId}/download`,
      fileUri,
      {
        headers: accessToken
          ? { Authorization: `Bearer ${accessToken}` }
          : {},
      },
    ),
  );

  if (downloadRes.status !== 200) {
    throw new Error(`Dosya indirilemedi (Durum: ${downloadRes.status})`);
  }

  return downloadRes.uri;
};

export interface ShareOptions {
  dialogTitle?: string;
  mimeType?: string;
  uti?: string;
}

async function openFileWithAndroidViewer(
  fileUri: string,
  mimeType?: string,
): Promise<boolean> {
  if (Platform.OS !== 'android') return false;

  try {
    const contentUri = await FileSystem.getContentUriAsync(fileUri);
    await IntentLauncher.startActivityAsync('android.intent.action.VIEW', {
      data: contentUri,
      flags: 1,
      type: mimeType || 'application/octet-stream',
    });
    return true;
  } catch {
    return false;
  }
}

export const openOrShareFile = async (
  fileUri: string,
  options?: ShareOptions,
): Promise<boolean> => {
  const opened = await openFileWithAndroidViewer(fileUri, options?.mimeType);
  if (opened) return true;

  const isAvailable = await Sharing.isAvailableAsync();
  if (!isAvailable) {
    return false;
  }

  await Sharing.shareAsync(fileUri, {
    dialogTitle: options?.dialogTitle,
    mimeType: options?.mimeType,
    UTI: options?.uti,
  });
  return true;
};

export const downloadAndOpenFile = async (
  fileId: string,
  fileName: string,
  mimeType?: string,
): Promise<{ shared: boolean; uri: string }> => {
  const localUri = await downloadFileToLocal(fileId, fileName);
  const shared = await openOrShareFile(localUri, {
    dialogTitle: fileName,
    mimeType,
  });
  return { shared, uri: localUri };
};

