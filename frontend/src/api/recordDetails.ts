import type {
  AuditLogResponse,
  RecordCreateRequest,
  RecordResponse,
  RecordUpdateRequest,
} from './generated/data-contracts'
import { api } from './client'
import { apiHttpClient } from './client'
import type { RecordCategoryOption } from './categories'
import { ApiClientError } from './errors'
import type { RecordHistoryItem, RecordStatus, WorkflowRecord } from '../types/record'

const recordStatuses: RecordStatus[] = [
  'TASLAK',
  'BSK_YRD_INCELEMESINDE',
  'BASKAN_INCELEMESINDE',
  'DUZENLEME_BEKLIYOR',
  'ONAYLANDI',
  'REDDEDILDI',
]

const actionLabels: Record<string, string> = {
  GONDER: 'Başkan Yardımcısına gönderildi',
  TEKRAR_GONDER: 'Yeniden incelemeye gönderildi',
  BASKANA_ILET: 'Başkana iletildi',
  CALISANA_GERI_GONDER: 'Çalışana geri gönderildi',
  BASKAN_YARDIMCISINA_GERI_GONDER: 'Başkan Yardımcısına geri gönderildi',
  ONAYLA: 'Kayıt onaylandı',
  REDDET: 'Kayıt reddedildi',
  RECORD_CREATED: 'Kayıt oluşturuldu',
  RECORD_UPDATED: 'Kayıt düzenlendi',
  RECORD_DELETED: 'Kayıt silindi',
}

function invalidRecordResponse(message: string): never {
  throw new ApiClientError({
    code: 'INVALID_RECORD_RESPONSE',
    message,
    status: 0,
  })
}

function isRecordStatus(value: string | undefined): value is RecordStatus {
  return Boolean(value && recordStatuses.includes(value as RecordStatus))
}

function normalizeHistoryItem(item: AuditLogResponse): RecordHistoryItem {
  if (!item.id || !item.action || !item.createdAt || !item.userFullName?.trim() || !item.roleName?.trim()) {
    return invalidRecordResponse('Sunucu geçerli kayıt geçmişi bilgisi döndürmedi.')
  }

  return {
    id: item.id,
    action: actionLabels[item.action] ?? item.action,
    actor: item.userFullName.trim(),
    actorId: item.userId,
    role: item.roleName.trim(),
    note: item.comment?.trim() || undefined,
    date: item.createdAt,
  }
}

export async function getRecordDetail(recordId: string, categories: RecordCategoryOption[]): Promise<WorkflowRecord> {
  const [record, auditLogs] = await Promise.all([
    api.records.getRecordById({ id: recordId }),
    listRecordAuditLogs(recordId),
  ])
  const categoryName = record.categoryId
    ? categories.find((category) => category.id === record.categoryId)?.name
    : undefined

  if (
    !record.id ||
    !record.title?.trim() ||
    !record.description?.trim() ||
    !record.categoryId ||
    !categoryName ||
    !isRecordStatus(record.status) ||
    !record.createdAt
  ) {
    return invalidRecordResponse('Sunucu geçerli kayıt detay bilgisi döndürmedi.')
  }

  const history = auditLogs
    .map(normalizeHistoryItem)
    .toSorted((left, right) => left.date.localeCompare(right.date))
  // Olusturan bilgisi kaydin kendisinden okunur, gecmisten turetilmez:
  // Baskanin gecmisi evrak kendisine iletildigi anda basladigi icin
  // "Kayıt oluşturuldu" satirini hic gormez ve geri dusulen history[0]
  // ona Baskan Yardimcisini olusturan gibi gosterirdi.
  const detail = record as RecordResponse & { createdBy?: string; createdByFullName?: string }
  const creatorItem = history.find((item) => item.action === 'Kayıt oluşturuldu')
  const createdById = detail.createdBy ?? creatorItem?.actorId
  const createdByName = detail.createdByFullName?.trim() || creatorItem?.actor || ''
  return {
    id: record.id,
    recordNumber: '',
    title: record.title.trim(),
    description: record.description.trim(),
    categoryId: record.categoryId,
    category: categoryName,
    status: record.status,
    createdBy: createdByName,
    createdById,
    assignedTo: null,
    assignedToId: null,
    lastDeputyId: null,
    lastAction: history.at(-1)?.action ?? '',
    createdAt: record.createdAt,
    updatedAt: record.createdAt,
    attachments: [],
    history,
  }
}

/** OperationId çakışmalarından etkilenmemek için kayıt geçmişi yolu adapterda sabitlenir. */
export function listRecordAuditLogs(recordId: string) {
  return apiHttpClient.request<AuditLogResponse[]>({
    path: `/api/audit-logs/record/${recordId}`,
    method: 'GET',
    secure: true,
  })
}

function requireRecordId(record: RecordResponse) {
  if (!record.id) return invalidRecordResponse('Sunucu kaydedilen kayıt için id döndürmedi.')
  return record.id
}

export async function createRecordDraft(input: RecordCreateRequest) {
  const record = await api.records.createRecord(input)
  return requireRecordId(record)
}

export async function updateRecordDraft(recordId: string, input: RecordUpdateRequest) {
  const record = await api.records.updateRecord({ id: recordId }, input)
  return requireRecordId(record)
}

export function deleteRecordDraft(recordId: string) {
  return api.records.deleteRecord({ id: recordId })
}
