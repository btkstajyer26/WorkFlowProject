import type { RecordSearchResponse, SearchData } from './generated/data-contracts'
import { apiHttpClient } from './client'
import { listCategories } from './categories'
import { ApiClientError } from './errors'
import type { RecordStatus } from '../types/record'

const recordStatuses: RecordStatus[] = [
  'TASLAK',
  'BSK_YRD_INCELEMESINDE',
  'BASKAN_INCELEMESINDE',
  'DUZENLEME_BEKLIYOR',
  'ONAYLANDI',
  'REDDEDILDI',
]

export type RecordSearchQuery = {
  text?: string
  status?: RecordStatus
  categoryId?: number
  userId?: string
  createdFrom?: string
  createdTo?: string
  page?: number
  size?: number
}

export type RecordSearchListItem = {
  id: string
  title: string
  description: string
  category: { id: number; name: string }
  status: RecordStatus
  createdBy: string
  assignedTo: string | null
  createdAt: string
  updatedAt: string | null
}

export type RecordSearchResult = {
  content: RecordSearchListItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

function invalidSearchResponse(message: string): never {
  throw new ApiClientError({
    code: 'INVALID_RECORD_SEARCH_RESPONSE',
    message,
    status: 0,
  })
}

function isRecordStatus(value: string | undefined): value is RecordStatus {
  return Boolean(value && recordStatuses.includes(value as RecordStatus))
}

function normalizeRecord(
  record: RecordSearchResponse,
  categoryNames: Map<number, string>,
): RecordSearchListItem {
  const categoryName = record.categoryId ? categoryNames.get(record.categoryId) : undefined
  if (
    !record.id ||
    !record.title?.trim() ||
    !record.description?.trim() ||
    !record.categoryId ||
    !categoryName ||
    !isRecordStatus(record.status) ||
    !record.createdBy ||
    !record.createdAt
  ) {
    return invalidSearchResponse('Sunucu geçerli kayıt arama bilgileri döndürmedi.')
  }

  return {
    id: record.id,
    title: record.title.trim(),
    description: record.description.trim(),
    category: { id: record.categoryId, name: categoryName },
    status: record.status,
    createdBy: record.createdBy,
    assignedTo: record.assignedTo ?? null,
    createdAt: record.createdAt,
    updatedAt: record.updatedAt ?? null,
  }
}

function requirePageNumber(value: number | undefined, field: string, minimum = 0) {
  if (!Number.isSafeInteger(value) || value! < minimum) {
    return invalidSearchResponse(`Sunucu geçerli ${field} bilgisi döndürmedi.`)
  }
  return value!
}

function startOfDay(value?: string) {
  return value ? `${value}T00:00:00` : undefined
}

function endOfDay(value?: string) {
  return value ? `${value}T23:59:59.999999` : undefined
}

export async function searchRecords({
  page = 0,
  size = 10,
  createdFrom,
  createdTo,
  ...criteria
}: RecordSearchQuery = {}): Promise<RecordSearchResult> {
  const [response, categories] = await Promise.all([
    apiHttpClient.request<SearchData>({
      path: '/api/records/search',
      method: 'GET',
      query: {
        ...criteria,
        startDate: startOfDay(createdFrom),
        endDate: endOfDay(createdTo),
        page,
        size,
        sort: 'createdAt,desc',
      },
      secure: true,
    }),
    listCategories(),
  ])
  const categoryNames = new Map(categories.map((category) => [category.id, category.name]))

  return {
    content: (response.content ?? []).map((record) => normalizeRecord(record, categoryNames)),
    page: requirePageNumber(response.page, 'sayfa'),
    size: requirePageNumber(response.size, 'sayfa boyutu', 1),
    totalElements: requirePageNumber(response.totalElements, 'toplam kayıt sayısı'),
    totalPages: requirePageNumber(response.totalPages, 'toplam sayfa sayısı'),
  }
}
