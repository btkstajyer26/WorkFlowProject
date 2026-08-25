import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback, useMemo } from 'react'
import { listCategories, type RecordCategoryOption } from '../api/categories'
import { queryKeys } from '../query/queryKeys'
import { CategoryContext, type CategoryStatus } from './categoryState'

const emptyCategories: RecordCategoryOption[] = []

export function CategoryProvider({ children }: { children: React.ReactNode }) {
  const queryClient = useQueryClient()
  const categoriesQuery = useQuery({
    queryKey: queryKeys.categories,
    queryFn: listCategories,
    staleTime: 5 * 60_000,
    retry: false,
  })
  const categories = categoriesQuery.data ?? emptyCategories
  const status: CategoryStatus = categoriesQuery.isPending
    ? 'loading'
    : categoriesQuery.isError ? 'error' : 'ready'

  const reloadCategories = useCallback(() => {
    void queryClient.invalidateQueries({ queryKey: queryKeys.categories })
  }, [queryClient])

  const value = useMemo(() => ({ categories, status, reloadCategories }), [categories, reloadCategories, status])

  return <CategoryContext.Provider value={value}>{children}</CategoryContext.Provider>
}
