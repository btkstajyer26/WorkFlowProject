import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getRecordFiles, deleteRecordFile, RecordFile } from '../api/files';
import { processUploadQueue, SelectedFile, UploadProgressItem } from '../services/files/uploadQueue';

// Query Key Sabitleri
export const fileKeys = {
  all: ['files'] as const,
  byRecord: (recordId: string) => [...fileKeys.all, 'record', recordId] as const,
};

// 1. Kayda ait dosyaları getiren hook
export const useRecordFiles = (recordId: string) => {
  return useQuery({
    queryKey: fileKeys.byRecord(recordId),
    queryFn: () => getRecordFiles(recordId),
    enabled: !!recordId,
  });
};

// 2. Sıralı dosya yükleme mutation hook'u
export const useUploadFilesMutation = (recordId: string) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      files,
      onProgress,
    }: {
      files: SelectedFile[];
      onProgress?: (progress: UploadProgressItem[]) => void;
    }) => processUploadQueue({ recordId, files, onProgress }),
    onSuccess: () => {
      // Yükleme tamamlanınca dosya listesini yenile
      queryClient.invalidateQueries({ queryKey: fileKeys.byRecord(recordId) });
    },
  });
};

// 3. Dosya silme mutation hook'u
export const useDeleteFileMutation = (recordId: string) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (fileId: string) => deleteRecordFile(fileId),
    onSuccess: () => {
      // Silme başarılı olunca listeyi güncelle
      queryClient.invalidateQueries({ queryKey: fileKeys.byRecord(recordId) });
    },
  });
};