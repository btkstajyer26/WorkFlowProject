import { http, HttpResponse } from 'msw'
import { apiBaseUrl } from '../../../api/config'
import { getAuthenticatedMockUser } from '../auth'
import { mockApiDb, type StoredMockFile } from '../db'
import { canViewMockRecord } from '../recordAccess'
import { apiErrorResponse, forbiddenResponse, unauthorizedResponse } from '../responses'

export const fileHandlers = [
  http.get(`${apiBaseUrl}/api/records/:id/files`, ({ params, request }) => {
    const user = getAuthenticatedMockUser(request)
    if (!user) return unauthorizedResponse()

    const record = mockApiDb.records.find((item) => item.id === params.id)
    if (!record) return apiErrorResponse(404, 'RESOURCE_NOT_FOUND', `Kayıt bulunamadı: ${params.id}`)
    if (!canViewMockRecord(user, record)) return forbiddenResponse('Bu kaydın dosyalarını görüntüleme yetkiniz yok')

    const files = mockApiDb.files.filter((file) => file.recordId === params.id)
    return HttpResponse.json(files)
  }),

  http.post(`${apiBaseUrl}/api/records/:id/files`, async ({ params, request }) => {
    const user = getAuthenticatedMockUser(request)
    if (!user) return unauthorizedResponse()
    if (user.role !== 'CALISAN') return forbiddenResponse('Yalnızca çalışanlar dosya yükleyebilir')

    const record = mockApiDb.records.find((item) => item.id === params.id)
    if (!record) return apiErrorResponse(404, 'RESOURCE_NOT_FOUND', `Kayıt bulunamadı: ${params.id}`)
    if (record.createdBy !== user.id) return forbiddenResponse()
    if (!['TASLAK', 'DUZENLEME_BEKLIYOR'].includes(record.status)) {
      return apiErrorResponse(400, 'BUSINESS_RULE_VIOLATION', 'Kilitli kayda dosya eklenemez')
    }

    const formData = await request.formData()
    const file = formData.get('file') as File | null
    if (!file || typeof file === 'string') {
      return apiErrorResponse(400, 'BAD_REQUEST', 'Dosya bulunamadı')
    }

    const newFile: StoredMockFile = {
      id: crypto.randomUUID(),
      recordId: String(params.id),
      originalName: file.name,
      mimeType: file.type || 'application/octet-stream',
      fileSize: file.size,
      uploadedBy: user.id,
      uploadedAt: new Date().toISOString(),
    }

    mockApiDb.files = [...mockApiDb.files, newFile]
    return HttpResponse.json([newFile])
  }),

  http.delete(`${apiBaseUrl}/api/files/:id`, ({ params, request }) => {
    const user = getAuthenticatedMockUser(request)
    if (!user) return unauthorizedResponse()
    if (user.role !== 'CALISAN') return forbiddenResponse()

    const file = mockApiDb.files.find((item) => item.id === params.id)
    if (!file) return apiErrorResponse(404, 'RESOURCE_NOT_FOUND', `Dosya bulunamadı: ${params.id}`)

    const record = mockApiDb.records.find((item) => item.id === file.recordId)
    if (!record || record.createdBy !== user.id) return forbiddenResponse()
    if (!['TASLAK', 'DUZENLEME_BEKLIYOR'].includes(record.status)) {
      return apiErrorResponse(400, 'BUSINESS_RULE_VIOLATION', 'Kilitli kayıttan dosya silinemez')
    }

    mockApiDb.files = mockApiDb.files.filter((item) => item.id !== params.id)
    return new HttpResponse(null, { status: 204 })
  }),

  http.get(`${apiBaseUrl}/api/files/:id/preview`, ({ params, request }) => {
    const user = getAuthenticatedMockUser(request)
    if (!user) return unauthorizedResponse()

    const file = mockApiDb.files.find((item) => item.id === params.id)
    if (!file) return apiErrorResponse(404, 'RESOURCE_NOT_FOUND', `Dosya bulunamadı: ${params.id}`)

    return new HttpResponse(new Blob(['fake preview content'], { type: file.mimeType }), {
      headers: {
        'Content-Type': file.mimeType,
        'Content-Disposition': `inline; filename="${file.originalName}"`,
      },
    })
  }),

  http.get(`${apiBaseUrl}/api/files/:id/download`, ({ params, request }) => {
    const user = getAuthenticatedMockUser(request)
    if (!user) return unauthorizedResponse()

    const file = mockApiDb.files.find((item) => item.id === params.id)
    if (!file) return apiErrorResponse(404, 'RESOURCE_NOT_FOUND', `Dosya bulunamadı: ${params.id}`)

    return new HttpResponse(new Blob(['fake download content'], { type: file.mimeType }), {
      headers: {
        'Content-Type': file.mimeType,
        'Content-Disposition': `attachment; filename="${file.originalName}"`,
      },
    })
  }),
]
