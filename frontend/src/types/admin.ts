import type { UserRole } from './auth'

export type ManagedUser = {
  id: string
  firstName: string
  lastName: string
  email: string
  role: UserRole
  isActive: boolean
  mustChangePassword: boolean
  createdAt: string
  updatedAt: string
  version: number
}

export type CreateManagedUserInput = {
  firstName: string
  lastName: string
  email: string
  password: string
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
}
