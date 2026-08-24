import { queryOptions, useQuery } from '@tanstack/react-query';

import { getCategories } from '@/api/categories';

export const categoryQueryKey = ['categories'] as const;

export const categoriesQueryOptions = queryOptions({
  queryFn: getCategories,
  queryKey: categoryQueryKey,
  staleTime: 5 * 60 * 1000,
});

export function useCategories() {
  return useQuery(categoriesQueryOptions);
}
