import { http, HttpResponse } from 'msw'
import type {
  AvailableActionView,
  WorkflowActionRequest,
  WorkflowActionResponse,
} from '../../../api/generated/data-contracts'
import { apiBaseUrl } from '../../../api/config'
import {
  getAuthenticatedMockUser,
  getMockUserById,
  getMockUserByRole,
  type MockApiRole,
} from '../auth'
import { mockApiDb, type StoredMockRecord } from '../db'
import { apiErrorResponse, unauthorizedResponse } from '../responses'

type WorkflowAction = WorkflowActionRequest['action']
type RecordStatus = StoredMockRecord['status']
type ActorRequirement = 'CREATOR' | 'ASSIGNEE' | 'CREATOR_AND_ASSIGNEE'

type TransitionRule = {
  from: RecordStatus
  action: WorkflowAction
  role: MockApiRole
  actor: ActorRequirement
  to: RecordStatus
}

const transitionRules: TransitionRule[] = [
  { from: 'TASLAK', action: 'GONDER', role: 'CALISAN', actor: 'CREATOR', to: 'BSK_YRD_INCELEMESINDE' },
  { from: 'DUZENLEME_BEKLIYOR', action: 'TEKRAR_GONDER', role: 'CALISAN', actor: 'CREATOR_AND_ASSIGNEE', to: 'BSK_YRD_INCELEMESINDE' },
  { from: 'BSK_YRD_INCELEMESINDE', action: 'BASKANA_ILET', role: 'BASKAN_YARDIMCISI', actor: 'ASSIGNEE', to: 'BASKAN_INCELEMESINDE' },
  { from: 'BSK_YRD_INCELEMESINDE', action: 'CALISANA_GERI_GONDER', role: 'BASKAN_YARDIMCISI', actor: 'ASSIGNEE', to: 'DUZENLEME_BEKLIYOR' },
  { from: 'BASKAN_INCELEMESINDE', action: 'ONAYLA', role: 'BASKAN', actor: 'ASSIGNEE', to: 'ONAYLANDI' },
  { from: 'BASKAN_INCELEMESINDE', action: 'REDDET', role: 'BASKAN', actor: 'ASSIGNEE', to: 'REDDEDILDI' },
  { from: 'BASKAN_INCELEMESINDE', action: 'CALISANA_GERI_GONDER', role: 'BASKAN', actor: 'ASSIGNEE', to: 'DUZENLEME_BEKLIYOR' },
  { from: 'BASKAN_INCELEMESINDE', action: 'BASKAN_YARDIMCISINA_GERI_GONDER', role: 'BASKAN', actor: 'ASSIGNEE', to: 'BSK_YRD_INCELEMESINDE' },
]

const roleIds: Record<MockApiRole, number> = {
  CALISAN: 1,
  BASKAN_YARDIMCISI: 2,
  BASKAN: 3,
  ADMIN: 4,
}

function actorMatches(record: StoredMockRecord, actorId: string, requirement: ActorRequirement) {
  const creator = record.createdBy === actorId
  const assignee = record.assignedTo === actorId
  if (requirement === 'CREATOR') return creator
  if (requirement === 'ASSIGNEE') return assignee
  return creator && assignee
}

function requiresComment(action: WorkflowAction) {
  return ['CALISANA_GERI_GONDER', 'BASKAN_YARDIMCISINA_GERI_GONDER', 'REDDET'].includes(action)
}

/** B10/WEB-1: gerçek backend'in available-actions ucunun taşıdığı gösterim adlarıyla aynı fikirde. */
const actionDisplayNames: Record<NonNullable<WorkflowAction>, string> = {
  GONDER: 'İncelemeye Gönder',
  TEKRAR_GONDER: 'Yeniden Gönder',
  BASKANA_ILET: 'Başkana İlet',
  CALISANA_GERI_GONDER: 'Çalışana Geri Gönder',
  BASKAN_YARDIMCISINA_GERI_GONDER: 'Başkan Yardımcısına Geri Gönder',
  ONAYLA: 'Onayla',
  REDDET: 'Reddet',
  DEPARTMANA_GONDER: 'Departmana Gönder',
}

function resolveTarget(record: StoredMockRecord, request: WorkflowActionRequest) {
  if (request.action === 'GONDER' || request.action === 'TEKRAR_GONDER') {
    return getMockUserByRole('BASKAN_YARDIMCISI')
  }
  if (request.action === 'BASKANA_ILET') return getMockUserByRole('BASKAN')
  if (request.action === 'CALISANA_GERI_GONDER') return getMockUserById(record.createdBy)
  if (request.action === 'BASKAN_YARDIMCISINA_GERI_GONDER') {
    return record.lastDeputyId ? getMockUserById(record.lastDeputyId) : undefined
  }
  return undefined
}

export const workflowHandlers = [
  // B10/WEB-1: RecordActionPanel'in düğmelerini buradan türetir - artık
  // rol/systemKey'e göre sabit dallanan bir liste yoktur, gerçek backend'in
  // AvailableActionResolver'ıyla aynı girdiyi (durum + rol + aktör ilişkisi)
  // kullanan bu mock'tan gelir.
  http.get(`${apiBaseUrl}/api/records/:recordId/workflow/available-actions`, ({ params, request }) => {
    const actor = getAuthenticatedMockUser(request)
    if (!actor) return unauthorizedResponse()

    const record = mockApiDb.records.find((item) => item.id === params.recordId)
    if (!record) {
      return apiErrorResponse(404, 'RESOURCE_NOT_FOUND', `Kayıt bulunamadı: ${params.recordId}`)
    }

    const actions: AvailableActionView[] = ['ONAYLANDI', 'REDDEDILDI'].includes(record.status)
      ? []
      : transitionRules
        .filter((rule) => (
          rule.from === record.status &&
          rule.role === actor.role &&
          actorMatches(record, actor.id, rule.actor)
        ))
        .map((rule) => ({
          action: rule.action,
          displayName: actionDisplayNames[rule.action!],
          commentRequired: requiresComment(rule.action),
          targetDepartmentRequired: false,
          targetUserRequired: false,
        }))

    // Parent/Subtask (docs/PARENT_SUBTASK_GOREV_DAGILIMI.md): ALT_GOREVLERE_AYIR
    // henuz gercek workflow_transitions/RecordStatus setinde degil (backend'de
    // hazirlaniyor), bu yuzden `transitionRules`'a eklenemiyor - burada ayrica
    // ekleniyor. Zaten bolunmus bir Parent'ta tekrar gosterilmez.
    const alreadySplit = mockApiDb.subtasks.some((subtask) => subtask.parentRecordId === record.id)
    if (record.status === 'BSK_YRD_INCELEMESINDE' && actor.role === 'BASKAN_YARDIMCISI' &&
      actorMatches(record, actor.id, 'ASSIGNEE') && !alreadySplit) {
      actions.push({
        action: 'ALT_GOREVLERE_AYIR' as AvailableActionView['action'],
        displayName: 'Alt Görevlere Ayır',
        commentRequired: false,
        targetDepartmentRequired: false,
        targetUserRequired: false,
      })
    }

    return HttpResponse.json({
      recordId: record.id,
      status: record.status,
      version: 0,
      actions,
    })
  }),

  http.get(`${apiBaseUrl}/api/records/:recordId/workflow/target-departments`, ({ params, request }) => {
    const actor = getAuthenticatedMockUser(request)
    if (!actor) return unauthorizedResponse()
    if (!mockApiDb.records.some((item) => item.id === params.recordId)) {
      return apiErrorResponse(404, 'RESOURCE_NOT_FOUND', `Kayıt bulunamadı: ${params.recordId}`)
    }
    // V1'de sabit sekiz gecişte departman hedefi yoktur; bu mock departman
    // yönetimi kurulana kadar bilerek boş liste döner.
    return HttpResponse.json({ departments: [] })
  }),

  http.post(`${apiBaseUrl}/api/records/:recordId/workflow/actions`, async ({ params, request }) => {
    const actor = getAuthenticatedMockUser(request)
    if (!actor) return unauthorizedResponse()
    if (actor.role === 'ADMIN') {
      return apiErrorResponse(403, 'WORKFLOW_ROLE_NOT_ALLOWED', 'Rolünüz bu işlemi yapamaz')
    }

    const record = mockApiDb.records.find((item) => item.id === params.recordId)
    if (!record) {
      return apiErrorResponse(404, 'RESOURCE_NOT_FOUND', `Kayıt bulunamadı: ${params.recordId}`)
    }

    const body = await request.json() as WorkflowActionRequest
    if (['ONAYLANDI', 'REDDEDILDI'].includes(record.status)) {
      return apiErrorResponse(409, 'WORKFLOW_RECORD_LOCKED', 'Kayıt kilitli, üzerinde işlem yapılamaz')
    }
    if (requiresComment(body.action) && !body.comment?.trim()) {
      return apiErrorResponse(400, 'WORKFLOW_COMMENT_REQUIRED', 'Bu işlem için açıklama zorunludur')
    }

    if (body.targetUserId) {
      return apiErrorResponse(400, 'WORKFLOW_TARGET_NOT_ALLOWED', 'Bu işlem için hedef kullanıcı gönderilmemelidir')
    }

    const rule = transitionRules.find((candidate) => (
      candidate.from === record.status &&
      candidate.action === body.action &&
      candidate.role === actor.role
    ))
    if (!rule) {
      return apiErrorResponse(400, 'WORKFLOW_INVALID_TRANSITION', 'Bu durumda bu işlem yapılamaz')
    }
    if (!actorMatches(record, actor.id, rule.actor)) {
      return apiErrorResponse(403, 'WORKFLOW_FORBIDDEN', 'Bu kayıt üzerinde işlem yapma yetkiniz yok')
    }

    const target = resolveTarget(record, body)
    if ((body.action === 'GONDER' || body.action === 'TEKRAR_GONDER') && target?.role !== 'BASKAN_YARDIMCISI') {
      return apiErrorResponse(400, 'WORKFLOW_TARGET_ROLE_INVALID', 'Seçilen hedef kullanıcının rolü uygun değil')
    }
    if (body.action === 'BASKAN_YARDIMCISINA_GERI_GONDER' && !target) {
      return apiErrorResponse(400, 'WORKFLOW_TARGET_NOT_ALLOWED', 'Kayıt için önceki Başkan Yardımcısı bulunamadı')
    }

    const performedAt = new Date().toISOString()
    const previousStatus = record.status
    const assignedTo = target?.id ?? null
    const updatedRecord: StoredMockRecord = {
      ...record,
      status: rule.to,
      assignedTo,
      lastDeputyId: target?.role === 'BASKAN_YARDIMCISI' ? target.id : record.lastDeputyId,
      updatedAt: performedAt,
    }
    mockApiDb.records = mockApiDb.records.map((item) => item.id === record.id ? updatedRecord : item)
    mockApiDb.auditLogs = [{
      id: crypto.randomUUID(),
      recordId: record.id,
      userId: actor.id,
      userFullName: `${actor.firstName} ${actor.lastName}`,
      roleId: roleIds[actor.role],
      roleName: actor.role,
      action: body.action,
      previousStatus,
      newStatus: rule.to,
      comment: body.comment,
      createdAt: performedAt,
    }, ...mockApiDb.auditLogs]

    const response: WorkflowActionResponse = {
      recordId: record.id,
      action: body.action,
      previousStatus,
      newStatus: rule.to,
      ...(assignedTo ? { assignedTo } : {}),
      performedBy: actor.id,
      performedAt,
    }
    return HttpResponse.json(response)
  }),
]
