import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { deleteRecordFile, listRecordFiles, uploadRecordFile } from '../api/files'
import { queryKeys } from '../query/queryKeys'

export function useRecordFiles(recordId: string, enabled = true) {
  const queryClient = useQueryClient()
  const filesQuery = useQuery({
    queryKey: queryKeys.records.files(recordId),
    queryFn: () => listRecordFiles(recordId),
    enabled,
  })
  const refresh = () => queryClient.invalidateQueries({ queryKey: queryKeys.records.files(recordId) })
  const uploadMutation = useMutation({
    mutationFn: (file: File) => uploadRecordFile(recordId, file),
    onSuccess: refresh,
  })
  const deleteMutation = useMutation({
    mutationFn: (fileId: string) => deleteRecordFile(fileId),
    onSuccess: refresh,
  })

  return {
    files: filesQuery.data ?? [],
    isPending: filesQuery.isPending,
    error: filesQuery.error,
    retry: filesQuery.refetch,
    upload: uploadMutation.mutateAsync,
    uploading: uploadMutation.isPending,
    uploadError: uploadMutation.error,
    remove: deleteMutation.mutateAsync,
    deletingId: deleteMutation.isPending ? deleteMutation.variables : undefined,
    deleteError: deleteMutation.error,
  }
}
