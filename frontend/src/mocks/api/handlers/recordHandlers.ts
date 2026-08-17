import { http, HttpResponse } from 'msw'
import type { PagedResponseRecordSearchResponse, RecordCreateRequest, RecordSearchResponse, RecordUpdateRequest } from '../../../api/generated/data-contracts'
import { apiBaseUrl } from '../../../api/config'
import { getAuthenticatedMockUser, getMockUserById } from '../auth'
import { mockApiCategories, mockApiDb, toRecordResponse, type StoredMockRecord } from '../db'
import { canViewMockRecord } from '../recordAccess'
import { apiErrorResponse, forbiddenResponse, unauthorizedResponse } from '../responses'

function parseNonNegativeInt(value: string | null, fallback: number) {
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : fallback
}

function validateRecordBody(body: RecordCreateRequest | RecordUpdateRequest) {
  const fieldErrors = []
  if (!body.title?.trim()) fieldErrors.push({ field: 'title', message: 'Başlık boş bırakılamaz' })
  if (!body.description?.trim()) fieldErrors.push({ field: 'description', message: 'Açıklama boş bırakılamaz' })
  if (!mockApiCategories.some((category) => category.id === body.categoryId)) {
    fieldErrors.push({ field: 'categoryId', message: 'Kategori seçimi geçersiz' })
  }
  return fieldErrors
}

export const recordHandlers = [
  http.get(`${apiBaseUrl}/api/records`, ({ request }) => {
    const user = getAuthenticatedMockUser(request)
    if (!user) return unauthorizedResponse()

    const url = new URL(request.url)
    const status = url.searchParams.get('status')
    const categoryId = url.searchParams.get('categoryId')
    const query = url.searchParams.get('q')?.trim().toLocaleLowerCase('tr-TR')
    const creator = url.searchParams.get('creator')?.trim().toLocaleLowerCase('tr-TR')
    const from = url.searchParams.get('from')
    const to = url.searchParams.get('to')
    const page = parseNonNegativeInt(url.searchParams.get('page'), 0)
    const size = Math.max(1, parseNonNegativeInt(url.searchParams.get('size'), 10))

    const filtered = mockApiDb.records.filter((record) => (
      canViewMockRecord(user, record) &&
      (!status || record.status === status) &&
      (!categoryId || record.categoryId === Number(categoryId)) &&
      (!query || `${record.title} ${record.description}`.toLocaleLowerCase('tr-TR').includes(query)) &&
      (!creator || (() => {
        const recordCreator = getMockUserById(record.createdBy)
        return Boolean(recordCreator && `${recordCreator.firstName} ${recordCreator.lastName}`.toLocaleLowerCase('tr-TR').includes(creator))
      })()) &&
      (!from || record.createdAt >= from) &&
      (!to || record.createdAt <= to)
    ))
    const pageContent: RecordSearchResponse[] = filtered.slice(page * size, page * size + size).map((record) => ({
      ...toRecordResponse(record),
      createdBy: record.createdBy,
      assignedTo: record.assignedTo ?? undefined,
      updatedAt: record.updatedAt,
    }))
    const totalPages = filtered.length === 0 ? 0 : Math.ceil(filtered.length / size)

    const response: PagedResponseRecordSearchResponse = {
      content: pageContent,
      page,
      size,
      totalElements: filtered.length,
      totalPages,
    }
    return HttpResponse.json(response)
  }),

  http.get(`${apiBaseUrl}/api/records/:id`, ({ params, request }) => {
    const user = getAuthenticatedMockUser(request)
    if (!user) return unauthorizedResponse()
    const record = mockApiDb.records.find((item) => item.id === params.id)
    if (!record) return apiErrorResponse(404, 'RESOURCE_NOT_FOUND', `Kayıt bulunamadı: ${params.id}`)
    if (!canViewMockRecord(user, record)) return forbiddenResponse('Bu kaydı görüntüleme yetkiniz yok')
    return HttpResponse.json(toRecordResponse(record))
  }),

  http.post(`${apiBaseUrl}/api/records`, async ({ request }) => {
    const user = getAuthenticatedMockUser(request)
    if (!user) return unauthorizedResponse()
    if (user.role !== 'CALISAN') return forbiddenResponse()

    const body = await request.json() as RecordCreateRequest
    const fieldErrors = validateRecordBody(body)
    if (fieldErrors.length) {
      return apiErrorResponse(400, 'VALIDATION_ERROR', 'Girilen veriler geçersiz', fieldErrors)
    }

    const now = new Date().toISOString()
    const record: StoredMockRecord = {
      id: crypto.randomUUID(),
      title: body.title.trim(),
      description: body.description.trim(),
      categoryId: body.categoryId,
      status: 'TASLAK',
      createdAt: now,
      updatedAt: now,
      createdBy: user.id,
      assignedTo: null,
      lastDeputyId: null,
    }
    mockApiDb.records = [record, ...mockApiDb.records]
    return HttpResponse.json(toRecordResponse(record), { status: 201 })
  }),

  http.put(`${apiBaseUrl}/api/records/:id`, async ({ params, request }) => {
    const user = getAuthenticatedMockUser(request)
    if (!user) return unauthorizedResponse()
    const record = mockApiDb.records.find((item) => item.id === params.id)
    if (!record) return apiErrorResponse(404, 'RESOURCE_NOT_FOUND', `Kayıt bulunamadı: ${params.id}`)
    if (user.role !== 'CALISAN' || record.createdBy !== user.id) return forbiddenResponse()
    if (!['TASLAK', 'DUZENLEME_BEKLIYOR'].includes(record.status)) {
      return apiErrorResponse(400, 'BUSINESS_RULE_VIOLATION', 'Bu kayıt şu anki durumunda düzenlenemez')
    }

    const body = await request.json() as RecordUpdateRequest
    const fieldErrors = validateRecordBody(body)
    if (fieldErrors.length) {
      return apiErrorResponse(400, 'VALIDATION_ERROR', 'Girilen veriler geçersiz', fieldErrors)
    }

    const updated = {
      ...record,
      title: body.title.trim(),
      description: body.description.trim(),
      categoryId: body.categoryId,
      updatedAt: new Date().toISOString(),
    }
    mockApiDb.records = mockApiDb.records.map((item) => item.id === record.id ? updated : item)
    return HttpResponse.json(toRecordResponse(updated))
  }),

  http.delete(`${apiBaseUrl}/api/records/:id`, ({ params, request }) => {
    const user = getAuthenticatedMockUser(request)
    if (!user) return unauthorizedResponse()
    const record = mockApiDb.records.find((item) => item.id === params.id)
    if (!record) return apiErrorResponse(404, 'RESOURCE_NOT_FOUND', `Kayıt bulunamadı: ${params.id}`)
    if (user.role !== 'CALISAN' || record.createdBy !== user.id) return forbiddenResponse()
    if (record.status !== 'TASLAK') {
      return apiErrorResponse(400, 'BUSINESS_RULE_VIOLATION', 'Sadece taslak durumundaki kayıtlar silinebilir')
    }
    mockApiDb.records = mockApiDb.records.filter((item) => item.id !== record.id)
    return new HttpResponse(null, { status: 204 })
  }),
]
