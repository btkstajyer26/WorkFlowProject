import { api } from './client'
import { ApiClientError } from './errors'

export type RecordCategoryOption = {
  id: number
  name: string
}

let categoryCache: RecordCategoryOption[] | null = null
let categoryRequest: Promise<RecordCategoryOption[]> | null = null
let categoryGeneration = 0

function normalizeCategories(value: Awaited<ReturnType<typeof api.categories.getAllCategories>>) {
  const categories = value.flatMap((category) => (
    Number.isInteger(category.id) && category.id! > 0 && category.name?.trim()
      ? [{ id: category.id!, name: category.name.trim() }]
      : []
  ))

  if (categories.length !== value.length || categories.length === 0) {
    throw new ApiClientError({
      code: 'INVALID_CATEGORY_RESPONSE',
      message: 'Sunucu geçerli kategori bilgileri döndürmedi.',
      status: 0,
    })
  }

  return categories
}

export function listCategories() {
  if (categoryCache) return Promise.resolve(categoryCache)
  if (categoryRequest) return categoryRequest

  const requestGeneration = categoryGeneration
  const request = api.categories.getAllCategories()
    .then(normalizeCategories)
    .then((categories) => {
      if (requestGeneration === categoryGeneration) categoryCache = categories
      return categories
    })

  let trackedRequest: Promise<RecordCategoryOption[]>
  trackedRequest = request.finally(() => {
    if (categoryRequest === trackedRequest) categoryRequest = null
  })
  categoryRequest = trackedRequest

  return categoryRequest
}

export function clearCategoryCache() {
  categoryGeneration += 1
  categoryCache = null
  categoryRequest = null
}
