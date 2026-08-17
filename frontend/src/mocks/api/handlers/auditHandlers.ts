import { http, HttpResponse } from 'msw'
import { apiBaseUrl } from '../../../api/config'
import { getAuthenticatedMockUser } from '../auth'
import { mockApiDb } from '../db'
import { canViewMockRecord } from '../recordAccess'
import { apiErrorResponse, forbiddenResponse, unauthorizedResponse } from '../responses'

export const auditHandlers = [
  http.get(`${apiBaseUrl}/api/audit-logs/record/:recordId`, ({ params, request }) => {
    const user = getAuthenticatedMockUser(request)
    if (!user) return unauthorizedResponse()
    const record = mockApiDb.records.find((item) => item.id === params.recordId)
    if (!record) return apiErrorResponse(404, 'RESOURCE_NOT_FOUND', `Kayıt bulunamadı: ${params.recordId}`)
    if (!canViewMockRecord(user, record)) return forbiddenResponse('Bu kaydı görüntüleme yetkiniz yok')
    return HttpResponse.json(
      mockApiDb.auditLogs
        .filter((log) => log.recordId === record.id)
        .toSorted((left, right) => left.createdAt!.localeCompare(right.createdAt!)),
    )
  }),
]
