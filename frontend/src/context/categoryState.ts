import { createContext, useContext } from 'react'
import type { RecordCategoryOption } from '../api/categories'

export type CategoryStatus = 'loading' | 'ready' | 'error'

type CategoryContextValue = {
  categories: RecordCategoryOption[]
  status: CategoryStatus
  reloadCategories: () => void
}

export const CategoryContext = createContext<CategoryContextValue | null>(null)

export function useCategories() {
  const context = useContext(CategoryContext)
  if (!context) throw new Error('useCategories must be used inside CategoryProvider.')
  return context
}
