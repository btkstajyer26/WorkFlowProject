import { http, HttpResponse } from 'msw'
import type {
  PagedResponseRecordSearchResponse,
  RecordSearchResponse,
} from '../../../api/generated/data-contracts'
import { apiBaseUrl } from '../../../api/config'
import { getAuthenticatedMockUser } from '../auth'
import { mockApiDb, type StoredMockRecord } from '../db'
import { canViewMockRecord } from '../recordAccess'
import { unauthorizedResponse } from '../responses'

function parseNonNegativeInt(value: string | null, fallback: number) {
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : fallback
}

function toSearchResponse(record: StoredMockRecord): RecordSearchResponse {
  return {
    id: record.id,
    title: record.title,
    description: record.description,
    categoryId: record.categoryId,
    status: record.status,
    createdBy: record.createdBy,
    ...(record.assignedTo ? { assignedTo: record.assignedTo } : {}),
    createdAt: record.createdAt,
    updatedAt: record.updatedAt,
  }
}

export const recordSearchHandlers = [
  http.get(`${apiBaseUrl}/api/records/search`, ({ request }) => {
    const user = getAuthenticatedMockUser(request)
    if (!user) return unauthorizedResponse()

    const url = new URL(request.url)
    const text = url.searchParams.get('text')?.trim().toLocaleLowerCase('tr-TR')
    const status = url.searchParams.get('status')
    const categoryId = url.searchParams.get('categoryId')
    const createdBy = url.searchParams.get('userId')
    const startDate = url.searchParams.get('startDate')
    const endDate = url.searchParams.get('endDate')
    const page = parseNonNegativeInt(url.searchParams.get('page'), 0)
    const size = Math.max(1, parseNonNegativeInt(url.searchParams.get('size'), 10))

    const filtered = mockApiDb.records
      .filter((record) => (
        canViewMockRecord(user, record) &&
        (!text || `${record.title} ${record.description}`.toLocaleLowerCase('tr-TR').includes(text)) &&
        (!status || record.status === status) &&
        (!categoryId || record.categoryId === Number(categoryId)) &&
        (!createdBy || record.createdBy === createdBy) &&
        (!startDate || record.createdAt >= startDate) &&
        (!endDate || record.createdAt <= endDate)
      ))
      .sort((left, right) => right.createdAt.localeCompare(left.createdAt))

    const content = filtered.slice(page * size, page * size + size).map(toSearchResponse)
    const response: PagedResponseRecordSearchResponse = {
      content,
      page,
      size,
      totalElements: filtered.length,
      totalPages: filtered.length === 0 ? 0 : Math.ceil(filtered.length / size),
    }
    return HttpResponse.json(response)
  }),
]
