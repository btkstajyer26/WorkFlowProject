import { http, HttpResponse } from 'msw'
import { apiBaseUrl } from '../../../api/config'
import { getAuthenticatedMockUser } from '../auth'
import { mockApiDb, type StoredMockRecord } from '../db'
import { apiErrorResponse, forbiddenResponse, unauthorizedResponse } from '../responses'

function canViewAudit(userId: string, role: string, record: StoredMockRecord) {
  if (role === 'CALISAN') return record.createdBy === userId
  if (role === 'BASKAN_YARDIMCISI') return record.assignedTo === userId
  if (role === 'BASKAN') return record.status === 'BASKAN_INCELEMESINDE' || record.assignedTo === userId
  return false
}

export const auditHandlers = [
  http.get(`${apiBaseUrl}/api/audit-logs/record/:recordId`, ({ params, request }) => {
    const user = getAuthenticatedMockUser(request)
    if (!user) return unauthorizedResponse()
    const record = mockApiDb.records.find((item) => item.id === params.recordId)
    if (!record) return apiErrorResponse(404, 'RESOURCE_NOT_FOUND', `Kayıt bulunamadı: ${params.recordId}`)
    if (!canViewAudit(user.id, user.role, record)) return forbiddenResponse('Bu kaydı görüntüleme yetkiniz yok')
    return HttpResponse.json(mockApiDb.auditLogs.filter((log) => log.recordId === record.id))
  }),
]
