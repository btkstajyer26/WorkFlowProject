import { createContext, useContext } from 'react'
import type { UserRole } from '../types/auth'
import type {
  AdminAuditLog,
  AdminOperationResult,
  CreateManagedUserInput,
  ManagedUser,
} from '../types/admin'

export type AdminContextValue = {
  users: ManagedUser[]
  logs: AdminAuditLog[]
  createUser: (input: CreateManagedUserInput) => AdminOperationResult
  changeUserRole: (userId: string, role: UserRole) => void
  setUserActive: (userId: string, isActive: boolean) => void
}

export const AdminContext = createContext<AdminContextValue | null>(null)

export function useAdmin() {
  const context = useContext(AdminContext)
  if (!context) throw new Error('useAdmin must be used inside AdminProvider.')
  return context
}
