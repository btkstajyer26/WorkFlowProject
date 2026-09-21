import { http, HttpResponse } from 'msw'
import { apiBaseUrl } from '../../../api/config'
import { getAuthenticatedMockUser, mockApiUsers } from '../auth'
import { mockApiDb, type StoredMockSubtask } from '../db'
import { apiErrorResponse, unauthorizedResponse } from '../responses'

type SplitRequestBody = {
  approvalPolicy?: 'UNANIMOUS' | 'MAJORITY'
  subtasks?: { title?: string; description?: string; assigneeUserId?: string }[]
}

function requiredApprovalsFor(policy: 'UNANIMOUS' | 'MAJORITY', count: number) {
  return policy === 'UNANIMOUS' ? count : Math.floor(count / 2) + 1
}

/**
 * Parent/Subtask dinamik alt akışı (docs/PARENT_SUBTASK_GOREV_DAGILIMI.md).
 * Backend hazır olana kadar frontend'in tek doğrulama zemini bu mock -
 * gerçek uçlar gelince bu dosya değişmeden generated istemciye geçilir,
 * yalnız `api/subtasks.ts` içindeki çağrılar güncellenir.
 */
export const subtaskHandlers = [
  http.get(`${apiBaseUrl}/api/records/:recordId/subtasks`, ({ params, request }) => {
    const actor = getAuthenticatedMockUser(request)
    if (!actor) return unauthorizedResponse()

    const record = mockApiDb.records.find((item) => item.id === params.recordId)
    if (!record) return apiErrorResponse(404, 'RESOURCE_NOT_FOUND', `Kayıt bulunamadı: ${params.recordId}`)

    const subtasks = mockApiDb.subtasks
      .filter((subtask) => subtask.parentRecordId === record.id)
      .map((subtask) => toSubtaskView(subtask))

    return HttpResponse.json({
      approvalPolicy: record.subtaskApprovalPolicy,
      requiredApprovals: record.subtaskRequiredApprovals,
      subtasks,
    })
  }),

  http.get(`${apiBaseUrl}/api/records/:recordId/subtasks/assignable-users`, ({ params, request }) => {
    const actor = getAuthenticatedMockUser(request)
    if (!actor) return unauthorizedResponse()
    if (!mockApiDb.records.some((item) => item.id === params.recordId)) {
      return apiErrorResponse(404, 'RESOURCE_NOT_FOUND', `Kayıt bulunamadı: ${params.recordId}`)
    }

    return HttpResponse.json({
      users: mockApiUsers
        .filter((user) => user.role !== 'ADMIN')
        .map((user) => ({ id: user.id, fullName: `${user.firstName} ${user.lastName}` })),
    })
  }),

  http.post(`${apiBaseUrl}/api/records/:recordId/subtasks/split`, async ({ params, request }) => {
    const actor = getAuthenticatedMockUser(request)
    if (!actor) return unauthorizedResponse()

    const record = mockApiDb.records.find((item) => item.id === params.recordId)
    if (!record) return apiErrorResponse(404, 'RESOURCE_NOT_FOUND', `Kayıt bulunamadı: ${params.recordId}`)
    if (record.status !== 'BSK_YRD_INCELEMESINDE' || record.assignedTo !== actor.id) {
      return apiErrorResponse(409, 'SUBTASK_PARENT_NOT_WAITING', 'Kayıt şu anda bölünmeye uygun durumda değil')
    }
    if (mockApiDb.subtasks.some((subtask) => subtask.parentRecordId === record.id)) {
      return apiErrorResponse(409, 'SUBTASK_PARENT_NOT_WAITING', 'Kayıt zaten alt görevlere bölünmüş')
    }

    const body = await request.json() as SplitRequestBody
    const items = body.subtasks ?? []
    if (!body.approvalPolicy || items.length < 2) {
      return apiErrorResponse(400, 'VALIDATION_ERROR', 'En az iki alt görev ve bir onay politikası gerekir')
    }
    if (items.some((item) => !item.title?.trim() || !item.assigneeUserId)) {
      return apiErrorResponse(400, 'VALIDATION_ERROR', 'Her alt görev için başlık ve atanan kişi zorunludur')
    }
    const assigneeIds = items.map((item) => item.assigneeUserId)
    if (new Set(assigneeIds).size !== assigneeIds.length) {
      return apiErrorResponse(400, 'VALIDATION_ERROR', 'Aynı kişi birden fazla alt göreve atanamaz')
    }

    const now = new Date().toISOString()
    const created: StoredMockSubtask[] = items.map((item) => ({
      id: crypto.randomUUID(),
      parentRecordId: record.id,
      title: item.title!.trim(),
      description: item.description?.trim() ?? '',
      assignedTo: item.assigneeUserId!,
      status: 'DEGERLENDIRME',
      resolutionComment: null,
      createdAt: now,
      completedAt: null,
    }))
    mockApiDb.subtasks = [...mockApiDb.subtasks, ...created]

    const requiredApprovals = requiredApprovalsFor(body.approvalPolicy, created.length)
    mockApiDb.records = mockApiDb.records.map((item) => item.id === record.id
      ? { ...item, subtaskApprovalPolicy: body.approvalPolicy!, subtaskRequiredApprovals: requiredApprovals }
      : item)

    return HttpResponse.json({
      parentRecordId: record.id,
      approvalPolicy: body.approvalPolicy,
      requiredApprovals,
      subtasks: created.map((subtask) => toSubtaskView(subtask)),
    }, { status: 201 })
  }),

  http.post(`${apiBaseUrl}/api/subtasks/:subtaskId/actions`, async ({ params, request }) => {
    const actor = getAuthenticatedMockUser(request)
    if (!actor) return unauthorizedResponse()

    const subtask = mockApiDb.subtasks.find((item) => item.id === params.subtaskId)
    if (!subtask) return apiErrorResponse(404, 'SUBTASK_NOT_FOUND', `Alt görev bulunamadı: ${params.subtaskId}`)
    if (subtask.assignedTo !== actor.id) {
      return apiErrorResponse(403, 'FORBIDDEN', 'Bu alt görev üzerinde işlem yapma yetkiniz yok')
    }
    if (subtask.status === 'TAMAMLANDI' || subtask.status === 'REDDEDILDI') {
      return apiErrorResponse(409, 'SUBTASK_ALREADY_RESOLVED', 'Bu alt görev zaten sonuçlanmış')
    }

    const body = await request.json() as { action?: string; comment?: string }
    const transitions: Record<string, { from: StoredMockSubtask['status']; to: StoredMockSubtask['status'] }> = {
      DEGERLENDIRMEYI_TAMAMLA: { from: 'DEGERLENDIRME', to: 'ISLEM' },
      ISLEMI_TAMAMLA: { from: 'ISLEM', to: 'ONAY' },
      ONAYLA: { from: 'ONAY', to: 'TAMAMLANDI' },
      REDDET: { from: 'ONAY', to: 'REDDEDILDI' },
    }
    const transition = body.action ? transitions[body.action] : undefined
    if (!transition || transition.from !== subtask.status) {
      return apiErrorResponse(400, 'VALIDATION_ERROR', 'Bu adımda bu işlem yapılamaz')
    }
    if (body.action === 'REDDET' && !body.comment?.trim()) {
      return apiErrorResponse(400, 'VALIDATION_ERROR', 'Reddetmek için açıklama zorunludur')
    }

    const now = new Date().toISOString()
    const resolved = transition.to === 'TAMAMLANDI' || transition.to === 'REDDEDILDI'
    const updated: StoredMockSubtask = {
      ...subtask,
      status: transition.to,
      resolutionComment: body.comment?.trim() || subtask.resolutionComment,
      completedAt: resolved ? now : null,
    }
    mockApiDb.subtasks = mockApiDb.subtasks.map((item) => item.id === subtask.id ? updated : item)

    return HttpResponse.json(toSubtaskView(updated))
  }),
]

function toSubtaskView(subtask: StoredMockSubtask) {
  const assignee = mockApiUsers.find((user) => user.id === subtask.assignedTo)
  return {
    id: subtask.id,
    parentRecordId: subtask.parentRecordId,
    title: subtask.title,
    description: subtask.description,
    assignedTo: subtask.assignedTo,
    assignedToName: assignee ? `${assignee.firstName} ${assignee.lastName}` : 'Bilinmeyen kullanıcı',
    status: subtask.status,
    resolutionComment: subtask.resolutionComment,
    createdAt: subtask.createdAt,
    completedAt: subtask.completedAt,
  }
}
