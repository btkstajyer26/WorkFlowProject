import type { AuditLogResponse, CategoryResponse, RecordResponse } from '../../api/generated/data-contracts'
import { getMockUserByRole, type MockApiUser } from './auth'

export type StoredMockRecord = Required<Pick<
  RecordResponse,
  'id' | 'title' | 'description' | 'categoryId' | 'status' | 'createdAt'
>> & {
  createdBy: string
  assignedTo: string | null
  lastDeputyId: string | null
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
      createdBy: employee.id,
      assignedTo: chair.id,
      lastDeputyId: deputy.id,
    },
  ]
}

type MockApiState = {
  records: StoredMockRecord[]
  auditLogs: AuditLogResponse[]
  createdUsers: MockApiUser[]
}

const state: MockApiState = {
  records: [],
  auditLogs: [],
  createdUsers: [],
}

export function resetMockApiDb() {
  state.records = initialRecords()
  state.auditLogs = []
  state.createdUsers = []
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
  get createdUsers() {
    return state.createdUsers
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
