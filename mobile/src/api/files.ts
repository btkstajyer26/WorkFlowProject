import * as FileSystem from 'expo-file-system/legacy';

import { API_BASE_URL, apiRequest } from './client';

export interface RecordFile {
  id: string;
  name: string;
  size: number;
  mimeType: string;
  createdAt: string;
  recordId: string;
}

export const getRecordFiles = async (recordId: string): Promise<RecordFile[]> => {
  return apiRequest<RecordFile[]>(`/api/records/${recordId}/files`);
};

export const uploadRecordFile = async (
  recordId: string,
  file: { uri: string; name: string; mimeType?: string }
): Promise<RecordFile> => {
  const formData = new FormData();

  formData.append('file', {
    uri: file.uri,
    name: file.name,
    type: file.mimeType || 'application/octet-stream',
  } as never);

  return apiRequest<RecordFile>(`/api/records/${recordId}/files`, {
    method: 'POST',
    body: formData,
  });
};

export const deleteRecordFile = async (fileId: string): Promise<void> => {
  await apiRequest<void>(`/api/files/${fileId}`, { method: 'DELETE' });
};

export const downloadFileToLocal = async (
  fileId: string,
  fileName: string,
  token?: string
): Promise<string> => {
  const targetDir = FileSystem.documentDirectory || FileSystem.cacheDirectory || '';
  const fileUri = `${targetDir}${fileName}`;

  const downloadRes = await FileSystem.downloadAsync(
    `${API_BASE_URL}/api/files/${fileId}/download`,
    fileUri,
    {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    }
  );

  if (downloadRes.status !== 200) {
    throw new Error(`Dosya indirilemedi (Durum: ${downloadRes.status})`);
  }

  return downloadRes.uri;
};
