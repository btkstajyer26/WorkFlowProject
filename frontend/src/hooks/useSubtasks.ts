import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  getAssignableUsers,
  getSubtasks,
  performSubtaskAction,
  splitIntoSubtasks,
} from '../api/subtasks'
import { queryKeys } from '../query/queryKeys'
import type { PerformSubtaskActionRequest, SplitIntoSubtasksRequest } from '../types/subtask'

/** Parent'ın alt görev listesi. Parent hiç bölünmediyse `subtasks` boş döner. */
export function useSubtasks(recordId: string, enabled = true) {
  return useQuery({
    queryKey: queryKeys.subtasks.list(recordId),
    queryFn: () => getSubtasks(recordId),
    enabled: enabled && Boolean(recordId),
  })
}

/** Yalnız bölme diyaloğu açıkken çağrılır (`enabled`). */
export function useAssignableUsers(recordId: string, enabled: boolean) {
  return useQuery({
    queryKey: queryKeys.subtasks.assignableUsers(recordId),
    queryFn: () => getAssignableUsers(recordId),
    enabled: enabled && Boolean(recordId),
    staleTime: 0,
  })
}

export function useSplitIntoSubtasks(recordId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: SplitIntoSubtasksRequest) => splitIntoSubtasks(recordId, request),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.subtasks.list(recordId) }),
        queryClient.invalidateQueries({ queryKey: queryKeys.records.detail(recordId) }),
        queryClient.invalidateQueries({ queryKey: queryKeys.workflow.availableActions(recordId) }),
        queryClient.invalidateQueries({ queryKey: queryKeys.records.lists() }),
      ])
    },
  })
}

/**
 * Bir alt görevi ilerletir/sonuçlandırır. Bu, tamamlanan son alt görevse
 * Parent'ı da otomatik ilerletebileceği için kayıt detayını ve available-actions'ı
 * da geçersiz kılıyoruz - panel kendiliğinden güncellenir.
 */
export function usePerformSubtaskAction(recordId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ subtaskId, request }: { subtaskId: string; request: PerformSubtaskActionRequest }) =>
      performSubtaskAction(subtaskId, request),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.subtasks.list(recordId) }),
        queryClient.invalidateQueries({ queryKey: queryKeys.records.detail(recordId) }),
        queryClient.invalidateQueries({ queryKey: queryKeys.workflow.availableActions(recordId) }),
        queryClient.invalidateQueries({ queryKey: queryKeys.records.lists() }),
        queryClient.invalidateQueries({ queryKey: queryKeys.notifications.all }),
      ])
    },
  })
}
