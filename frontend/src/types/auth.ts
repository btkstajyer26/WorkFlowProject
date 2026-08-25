export type WorkflowRole = 'CALISAN' | 'BASKAN_YARDIMCISI' | 'BASKAN'
export type UserRole = WorkflowRole | 'ADMIN'

export type AuthUser = {
  id: string
  firstName: string
  lastName: string
  email: string
  role: UserRole
  mustChangePassword: boolean
}

export const roleLabels: Record<UserRole, string> = {
  CALISAN: 'Çalışan',
  BASKAN_YARDIMCISI: 'Başkan Yardımcısı',
  BASKAN: 'Başkan',
  ADMIN: 'Sistem Yöneticisi',
}
