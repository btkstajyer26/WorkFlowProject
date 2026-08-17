import { mockRecords } from './records'
import { demoAccounts } from './users'
import type { AdminAuditLog, ManagedUser } from '../types/admin'

const demoDates = [
  '2026-07-11T09:15:00',
  '2026-07-14T10:30:00',
  '2026-07-18T13:40:00',
  '2026-07-20T08:50:00',
]

export const mockManagedUsers: ManagedUser[] = [
  ...demoAccounts.map((account, index) => ({
    id: account.id,
    firstName: account.firstName,
    lastName: account.lastName,
    email: account.email,
    role: account.role,
    isActive: true,
    createdAt: demoDates[index] ?? demoDates[0],
  })),
  {
    id: 'user-managed-004',
    firstName: 'Elif',
    lastName: 'Akın',
    email: 'elif.akin@kurum.gov.tr',
    role: 'CALISAN',
    isActive: true,
    createdAt: '2026-08-02T11:25:00',
  },
  {
    id: 'user-managed-005',
    firstName: 'Mert',
    lastName: 'Yılmaz',
    email: 'mert.yilmaz@kurum.gov.tr',
    role: 'CALISAN',
    isActive: false,
    createdAt: '2026-06-12T09:10:00',
  },
]

const recordLogs: AdminAuditLog[] = mockRecords.flatMap((record) =>
  record.history.map((history) => ({
    id: `record-${history.id}`,
    type: 'RECORD' as const,
    action: history.action.toLocaleUpperCase('tr-TR').replaceAll(' ', '_'),
    actionLabel: history.action,
    actor: history.actor,
    target: `${record.recordNumber} · ${record.title}`,
    description: history.note ?? `${record.recordNumber} numaralı evrakta işlem yapıldı.`,
    createdAt: history.date,
    recordId: record.id,
    recordNumber: record.recordNumber,
  })),
)

const userLogs: AdminAuditLog[] = [
  {
    id: 'user-log-001',
    type: 'USER',
    action: 'USER_CREATED',
    actionLabel: 'Hesap oluşturuldu',
    actor: 'Zeynep Yönetici',
    target: 'Elif Akın',
    description: 'Çalışan rolüyle yeni kullanıcı hesabı oluşturuldu.',
    createdAt: '2026-08-02T11:25:00',
  },
  {
    id: 'user-log-002',
    type: 'USER',
    action: 'ACCOUNT_DEACTIVATED',
    actionLabel: 'Hesap pasifleştirildi',
    actor: 'Zeynep Yönetici',
    target: 'Mert Yılmaz',
    description: 'Kullanıcının sisteme erişimi kapatıldı.',
    createdAt: '2026-07-28T15:45:00',
  },
  {
    id: 'user-log-003',
    type: 'USER',
    action: 'ROLE_CHANGED',
    actionLabel: 'Rol değiştirildi',
    actor: 'Zeynep Yönetici',
    target: 'Ayşe Kaya',
    description: 'Kullanıcı rolü Çalışan → Başkan Yardımcısı olarak değiştirildi.',
    createdAt: '2026-07-14T10:30:00',
  },
]

export const mockAdminAuditLogs = [...userLogs, ...recordLogs]
  .sort((left, right) => right.createdAt.localeCompare(left.createdAt))
