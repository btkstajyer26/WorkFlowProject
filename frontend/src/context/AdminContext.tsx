import { useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { normalizeManagedUser } from '../api/admin'
import { api } from '../api/client'
import { queryKeys } from '../query/queryKeys'
import type { AdminAuditLog, CreateManagedUserInput, ManagedUser } from '../types/admin'
import type { AuthUser, UserRole } from '../types/auth'
import { AdminContext, type AdminContextValue } from './adminState'

export function AdminProvider({ actor: _actor, children }: { actor: AuthUser; children: ReactNode }) {
  const queryClient = useQueryClient()
  const [users] = useState<ManagedUser[]>([])
  const [logs] = useState<AdminAuditLog[]>([])

  const invalidateAdminData = () => queryClient.invalidateQueries({ queryKey: queryKeys.admin.all })
  const createUser = async (input: CreateManagedUserInput) => {
    const response = await api.admin.createUser(input)
    const createdUser = normalizeManagedUser(response)
    if (createdUser.systemKey !== 'CALISAN' || !createdUser.isActive) {
      throw new Error('Sunucu oluşturulan kullanıcıyı aktif Çalışan olarak döndürmedi.')
    }

    await invalidateAdminData()
    return createdUser
  }

  const changeUserRole = async (
    userId: string,
    role: UserRole,
    replacementDeputyId?: string,
    activeDeputyId?: string,
  ) => {
    if (role === 'BASKAN_YARDIMCISI' && activeDeputyId && activeDeputyId !== userId) {
      await api.admin.changeRole({ id: activeDeputyId }, {
        roleName: 'CALISAN',
        replacementBaskanYardimcisiId: userId,
      })
    } else {
      await api.admin.changeRole({ id: userId }, {
        roleName: role,
        ...(replacementDeputyId ? { replacementBaskanYardimcisiId: replacementDeputyId } : {}),
      })
    }
    await invalidateAdminData()
  }

  const setUserActive = async (userId: string, isActive: boolean) => {
    await api.admin.setActive({ id: userId }, { active: isActive })
    await invalidateAdminData()
  }

  const value: AdminContextValue = {
    users,
    logs,
    createUser,
    changeUserRole,
    setUserActive,
  }

  return <AdminContext.Provider value={value}>{children}</AdminContext.Provider>
}
