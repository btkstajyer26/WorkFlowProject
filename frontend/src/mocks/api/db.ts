import type {
  AuditLogResponse,
  CategoryResponse,
  NotificationResponse,
  RecordResponse,
} from '../../api/generated/data-contracts'
import { getMockUserByRole, type MockApiUser } from './auth'

export type StoredMockRecord = Required<Pick<
  RecordResponse,
  'id' | 'title' | 'description' | 'categoryId' | 'status' | 'createdAt'
>> & {
  updatedAt: string
  createdBy: string
  assignedTo: string | null
  lastDeputyId: string | null
}

export type StoredMockNotification = Required<Pick<
  NotificationResponse,
  'id' | 'recordId' | 'message' | 'notificationType' | 'read' | 'createdAt'
>> & {
  userId: string
}

export const mockApiCategories: Required<CategoryResponse>[] = [
  { id: 1, name: 'İdari' },
  { id: 2, name: 'Mali' },
  { id: 3, name: 'İnsan Kaynakları' },
  { id: 4, name: 'Bilgi İşlem' },
  { id: 5, name: 'Teknik' },
]

function initialRecords(): StoredMockRecord[] {
  const employee = getMockUserByRole('CALISAN')
  const deputy = getMockUserByRole('BASKAN_YARDIMCISI')
  const chair = getMockUserByRole('BASKAN')

  return [
    {
      id: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1',
      title: 'Sunucu alım talebi',
      description: 'İki yeni sunucu temin edilmesi talep edilmektedir.',
      categoryId: 4,
      status: 'TASLAK',
      createdAt: '2026-08-01T09:15:00Z',
      updatedAt: '2026-08-01T09:15:00Z',
      createdBy: employee.id,
      assignedTo: null,
      lastDeputyId: null,
    },
    {
      id: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa2',
      title: 'Birim içi eğitim planı',
      description: 'Bilgi güvenliği eğitimi planıdır.',
      categoryId: 3,
      status: 'BSK_YRD_INCELEMESINDE',
      createdAt: '2026-08-02T10:00:00Z',
      updatedAt: '2026-08-04T14:20:00Z',
      createdBy: employee.id,
      assignedTo: deputy.id,
      lastDeputyId: deputy.id,
    },
    {
      id: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa3',
      title: 'Bakım sözleşmesi yenileme',
      description: 'Yıllık bakım sözleşmesinin yenilenmesi talebidir.',
      categoryId: 1,
      status: 'BASKAN_INCELEMESINDE',
      createdAt: '2026-08-03T11:30:00Z',
      updatedAt: '2026-08-05T09:45:00Z',
      createdBy: employee.id,
      assignedTo: chair.id,
      lastDeputyId: deputy.id,
    },
  ]
}

function initialNotifications(): StoredMockNotification[] {
  const employee = getMockUserByRole('CALISAN')
  const deputy = getMockUserByRole('BASKAN_YARDIMCISI')
  const chair = getMockUserByRole('BASKAN')

  return [
    {
      id: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb1',
      userId: employee.id,
      recordId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa3',
      message: 'Evrağınız onaylandı',
      notificationType: 'RECORD_APPROVED',
      read: false,
      createdAt: '2026-08-05T10:18:00Z',
    },
    {
      id: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb2',
      userId: deputy.id,
      recordId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa2',
      message: 'Bir evrak incelemenize sunuldu',
      notificationType: 'RECORD_SUBMITTED',
      read: false,
      createdAt: '2026-08-05T09:42:00Z',
    },
    {
      id: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb3',
      userId: chair.id,
      recordId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa3',
      message: 'Bir evrak onayınıza iletildi',
      notificationType: 'RECORD_FORWARDED',
      read: false,
      createdAt: '2026-08-04T16:24:00Z',
    },
    {
      id: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb4',
      userId: employee.id,
      recordId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1',
      message: 'Evrağınız düzeltme için geri gönderildi',
      notificationType: 'RECORD_RETURNED',
      read: true,
      createdAt: '2026-08-03T08:30:00Z',
    },
  ]
}

export type StoredMockFile = {
  id: string
  recordId: string
  originalName: string
  mimeType: string
  fileSize: number
  uploadedBy: string
  uploadedAt: string
}

type MockApiState = {
  records: StoredMockRecord[]
  auditLogs: AuditLogResponse[]
  notifications: StoredMockNotification[]
  createdUsers: MockApiUser[]
  files: StoredMockFile[]
}

const state: MockApiState = {
  records: [],
  auditLogs: [],
  notifications: [],
  createdUsers: [],
  files: [],
}

export function resetMockApiDb() {
  state.records = initialRecords()
  state.auditLogs = []
  state.notifications = initialNotifications()
  state.createdUsers = []
  state.files = []
}

resetMockApiDb()

export const mockApiDb = {
  get records() {
    return state.records
  },
  set records(records: StoredMockRecord[]) {
    state.records = records
  },
  get auditLogs() {
    return state.auditLogs
  },
  set auditLogs(logs: AuditLogResponse[]) {
    state.auditLogs = logs
  },
  get notifications() {
    return state.notifications
  },
  set notifications(notifications: StoredMockNotification[]) {
    state.notifications = notifications
  },
  get createdUsers() {
    return state.createdUsers
  },
  get files() {
    return state.files
  },
  set files(files: StoredMockFile[]) {
    state.files = files
  },
}

export function toRecordResponse(record: StoredMockRecord): RecordResponse {
  return {
    id: record.id,
    title: record.title,
    description: record.description,
    categoryId: record.categoryId,
    status: record.status,
    createdAt: record.createdAt,
  }
}
