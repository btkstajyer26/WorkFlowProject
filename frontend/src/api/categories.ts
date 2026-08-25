import { api } from './client'
import { ApiClientError } from './errors'

export type RecordCategoryOption = {
  id: number
  name: string
}

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
  return api.categories.getAllCategories().then(normalizeCategories)
}
