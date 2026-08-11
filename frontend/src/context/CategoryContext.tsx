import { useCallback, useEffect, useMemo, useState } from 'react'
import { clearCategoryCache, listCategories, type RecordCategoryOption } from '../api/categories'
import { CategoryContext, type CategoryStatus } from './categoryState'

export function CategoryProvider({ children }: { children: React.ReactNode }) {
  const [categories, setCategories] = useState<RecordCategoryOption[]>([])
  const [status, setStatus] = useState<CategoryStatus>('loading')
  const [reloadVersion, setReloadVersion] = useState(0)

  useEffect(() => {
    let active = true
    setStatus('loading')

    void listCategories()
      .then((loadedCategories) => {
        if (!active) return
        setCategories(loadedCategories)
        setStatus('ready')
      })
      .catch(() => {
        if (!active) return
        setCategories([])
        setStatus('error')
      })

    return () => {
      active = false
    }
  }, [reloadVersion])

  const reloadCategories = useCallback(() => {
    clearCategoryCache()
    setReloadVersion((current) => current + 1)
  }, [])

  const value = useMemo(() => ({ categories, status, reloadCategories }), [categories, reloadCategories, status])

  return <CategoryContext.Provider value={value}>{children}</CategoryContext.Provider>
}
