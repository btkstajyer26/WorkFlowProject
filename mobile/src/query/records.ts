import {
  type InfiniteData,
  queryOptions,
  useInfiniteQuery,
  useMutation,
  useQueries,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';

import {
  createRecord,
  deleteRecord,
  getRecord,
  getRecords,
  type RecordDetail,
  type RecordFilters,
  type RecordMutationRequest,
  type RecordPage,
  type RecordStatus,
  updateRecord,
} from '@/api/records';

export const recordQueryKeys = {
  all: ['records'] as const,
  count: (status: RecordStatus) =>
    [...recordQueryKeys.counts(), status] as const,
  counts: () => [...recordQueryKeys.all, 'count'] as const,
  detail: (recordId: string) =>
    [...recordQueryKeys.details(), recordId] as const,
  details: () => [...recordQueryKeys.all, 'detail'] as const,
  infiniteList: (filters: InfiniteRecordFilters) =>
    [...recordQueryKeys.lists(), 'infinite', filters] as const,
  list: (filters: RecordFilters) =>
    [...recordQueryKeys.lists(), filters] as const,
  lists: () => [...recordQueryKeys.all, 'list'] as const,
};

export type InfiniteRecordFilters = Omit<RecordFilters, 'page'>;

export function recordsQueryOptions(filters: RecordFilters) {
  return queryOptions({
    queryFn: () => getRecords(filters),
    queryKey: recordQueryKeys.list(filters),
    staleTime: 30 * 1000,
  });
}

export function useRecords(filters: RecordFilters, enabled = true) {
  return useQuery({
    ...recordsQueryOptions(filters),
    enabled,
  });
}

export function useRecord(recordId: string, enabled = true) {
  return useQuery({
    enabled: enabled && Boolean(recordId),
    queryFn: () => getRecord(recordId),
    queryKey: recordQueryKeys.detail(recordId),
  });
}

export function useInfiniteRecords(
  filters: InfiniteRecordFilters,
  enabled = true,
) {
  return useInfiniteQuery<
    RecordPage,
    Error,
    InfiniteData<RecordPage>,
    ReturnType<typeof recordQueryKeys.infiniteList>,
    number
  >({
    enabled,
    getNextPageParam: (lastPage) => {
      const nextPage = lastPage.page + 1;
      return nextPage < lastPage.totalPages ? nextPage : undefined;
    },
    initialPageParam: 0,
    queryFn: ({ pageParam }) => getRecords({ ...filters, page: pageParam }),
    queryKey: recordQueryKeys.infiniteList(filters),
    staleTime: 30 * 1000,
  });
}

export function useRecordCounts(
  statuses: readonly RecordStatus[],
  enabled = true,
) {
  return useQueries({
    queries: statuses.map((status) => ({
      enabled,
      queryFn: async () => {
        const page = await getRecords({ page: 0, size: 1, status });
        return page.totalElements;
      },
      queryKey: recordQueryKeys.count(status),
      staleTime: 30 * 1000,
    })),
  });
}

export function useCreateRecord() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createRecord,
    onSuccess: async (record) => {
      queryClient.setQueryData<RecordDetail>(
        recordQueryKeys.detail(record.id),
        record,
      );
      await queryClient.invalidateQueries({ queryKey: recordQueryKeys.all });
    },
  });
}

export function useUpdateRecord(recordId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: RecordMutationRequest) =>
      updateRecord(recordId, request),
    onSuccess: async (record) => {
      queryClient.setQueryData<RecordDetail>(
        recordQueryKeys.detail(recordId),
        record,
      );
      await queryClient.invalidateQueries({ queryKey: recordQueryKeys.all });
    },
  });
}

export function useDeleteRecord(recordId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => deleteRecord(recordId),
    onSuccess: async () => {
      queryClient.removeQueries({ queryKey: recordQueryKeys.detail(recordId) });
      await queryClient.invalidateQueries({ queryKey: recordQueryKeys.all });
    },
  });
}
