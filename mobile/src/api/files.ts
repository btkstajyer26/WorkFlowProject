import { apiClient } from './client';
import * as FileSystem from 'expo-file-system/legacy';

export interface RecordFile {
  id: string;
  name: string;
  size: number;
  mimeType: string;
  createdAt: string;
  recordId: string;
}

export const getRecordFiles = async (recordId: string): Promise<RecordFile[]> => {
  return apiClient.get<RecordFile[]>(`/api/records/${recordId}/files`);
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
  } as any);

  return apiClient.post<RecordFile>(`/api/records/${recordId}/files`, formData);
};

export const deleteRecordFile = async (fileId: string): Promise<void> => {
  return apiClient.delete<void>(`/api/files/${fileId}`);
};

export const downloadFileToLocal = async (
  fileId: string,
  fileName: string,
  token?: string
): Promise<string> => {
  const targetDir = FileSystem.documentDirectory || FileSystem.cacheDirectory || '';
  const fileUri = `${targetDir}${fileName}`;
  const apiUrl = process.env.EXPO_PUBLIC_API_URL || 'https://api.workflowproject.com';

  const downloadRes = await FileSystem.downloadAsync(
    `${apiUrl}/api/files/${fileId}/download`,
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