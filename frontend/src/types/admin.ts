import type { UserRole } from './auth'

export type ManagedUser = {
  id: string
  firstName: string
  lastName: string
  email: string
  role: UserRole
  isActive: boolean
  createdAt: string
}

export type CreateManagedUserInput = {
  firstName: string
  lastName: string
  email: string
  password: string
}

/**
 * Panelin rol kataloğu görünümü. `types/auth.ts`'teki `UserRole` union'ından
 * bilerek bağımsızdır: roller panelden dinamik olarak açılacağı için ad,
 * sabit bir rol listesine değil backend'in döndürdüğü metne dayanır.
 */
export type AdminRole = {
  id: number
  name: string
  systemKey: string | null
  description: string | null
}

export type AdminLogType = 'USER' | 'RECORD'

export type AdminAuditLog = {
  id: string
  type: AdminLogType
  action: string
  actionLabel: string
  actor: string
  target: string
  description: string
  createdAt: string
  recordId?: string
  recordNumber?: string
  httpMethod?: string
  requestPath?: string
  httpStatus?: number
  errorCode?: string
}
