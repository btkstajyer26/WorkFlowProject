import type { MockApiUser } from './auth'
import type { StoredMockRecord } from './db'

export function canViewMockRecord(user: MockApiUser, record: StoredMockRecord) {
  if (user.role === 'CALISAN') return record.createdBy === user.id
  if (user.role === 'BASKAN_YARDIMCISI') return record.assignedTo === user.id
  if (user.role === 'BASKAN') {
    return record.status === 'BASKAN_INCELEMESINDE' || record.assignedTo === user.id
  }
  return false
}
