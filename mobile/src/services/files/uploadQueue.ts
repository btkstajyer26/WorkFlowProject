import { uploadRecordFile, RecordFile } from '../../api/files';
import { validateFile } from '../../utils/fileValidators';

export interface SelectedFile {
  uri: string;
  name: string;
  size?: number;
  mimeType?: string;
}

export interface UploadProgressItem {
  fileName: string;
  status: 'pending' | 'uploading' | 'completed' | 'error';
  error?: string;
}

export interface QueueUploadOptions {
  recordId: string;
  files: SelectedFile[];
  onProgress?: (progress: UploadProgressItem[]) => void;
}

/**
 * Backend tekli upload desteklediği için seçilen dosyaları sırayla (sequential) yükler.
 */
export const processUploadQueue = async ({
  recordId,
  files,
  onProgress,
}: QueueUploadOptions): Promise<RecordFile[]> => {
  const uploadedFiles: RecordFile[] = [];
  const progressState: UploadProgressItem[] = files.map((f) => ({
    fileName: f.name,
    status: 'pending',
  }));

  const updateProgress = (index: number, status: UploadProgressItem['status'], error?: string) => {
    progressState[index] = { fileName: files[index].name, status, error };
    if (onProgress) {
      onProgress([...progressState]);
    }
  };

  for (let i = 0; i < files.length; i++) {
    const file = files[i];
    
    // 1. İstemci taraflı doğrulama (10 MB ve format kontrolü)
    const validation = validateFile(file);
    if (!validation.isValid) {
      updateProgress(i, 'error', validation.error);
      continue;
    }

    // 2. Yükleme adımı
    updateProgress(i, 'uploading');
    try {
      const result = await uploadRecordFile(recordId, {
        uri: file.uri,
        name: file.name,
        mimeType: file.mimeType || 'application/octet-stream',
      });
      uploadedFiles.push(result);
      updateProgress(i, 'completed');
    } catch (err: any) {
      updateProgress(i, 'error', err?.message || 'Yükleme sırasında hata oluştu.');
    }
  }

  return uploadedFiles;
};