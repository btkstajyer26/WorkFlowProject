import { createContext, useContext } from 'react'
import type { UserRole } from '../types/auth'
import type { AdminAuditLog, CreateManagedUserInput, ManagedUser } from '../types/admin'

export type AdminContextValue = {
  users: ManagedUser[]
  logs: AdminAuditLog[]
  loadStatus: 'loading' | 'ready' | 'error'
  retryLoad: () => Promise<void>
  createUser: (input: CreateManagedUserInput) => Promise<ManagedUser>
  changeUserRole: (userId: string, role: UserRole, replacementDeputyId?: string) => Promise<void>
  setUserActive: (userId: string, isActive: boolean) => Promise<void>
}

export const AdminContext = createContext<AdminContextValue | null>(null)

export function useAdmin() {
  const context = useContext(AdminContext)
  if (!context) throw new Error('useAdmin must be used inside AdminProvider.')
  return context
}
