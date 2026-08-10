import { createContext, useContext } from 'react'
import type { UserRole, WorkflowRole } from '../types/auth'
import type { AdminAuditLog, ManagedUser } from '../types/admin'
import type { RegistrationRequest } from '../types/registration'

export type AdminContextValue = {
  users: ManagedUser[]
  logs: AdminAuditLog[]
  registrationRequests: RegistrationRequest[]
  approveRegistration: (requestId: string, role: WorkflowRole) => void
  rejectRegistration: (requestId: string) => void
  changeUserRole: (userId: string, role: UserRole) => void
  setUserActive: (userId: string, isActive: boolean) => void
}

export const AdminContext = createContext<AdminContextValue | null>(null)

export function useAdmin() {
  const context = useContext(AdminContext)
  if (!context) throw new Error('useAdmin must be used inside AdminProvider.')
  return context
}
