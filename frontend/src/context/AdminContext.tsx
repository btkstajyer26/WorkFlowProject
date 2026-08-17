import { useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { normalizeManagedUser } from '../api/admin'
import { api } from '../api/client'
import { apiMode } from '../api/config'
import { mockAdminAuditLogs, mockManagedUsers } from '../mocks/admin'
import { queryKeys } from '../query/queryKeys'
import type { AdminAuditLog, CreateManagedUserInput, ManagedUser } from '../types/admin'
import { roleLabels, type AuthUser, type UserRole } from '../types/auth'
import { AdminContext, type AdminContextValue } from './adminState'

function fullName(user: Pick<ManagedUser | AuthUser, 'firstName' | 'lastName'>) {
  return `${user.firstName} ${user.lastName}`
}

function createId(prefix: string) {
  return `${prefix}-${crypto.randomUUID()}`
}

export function AdminProvider({ actor, children }: { actor: AuthUser; children: ReactNode }) {
  const queryClient = useQueryClient()
  const [users, setUsers] = useState<ManagedUser[]>(() => apiMode === 'mock' ? mockManagedUsers : [])
  const [logs, setLogs] = useState<AdminAuditLog[]>(() => apiMode === 'mock'
    ? mockAdminAuditLogs.filter((log) => log.type === 'USER')
    : [])

  const invalidateAdminData = () => queryClient.invalidateQueries({ queryKey: queryKeys.admin.all })
  const addLog = (log: Omit<AdminAuditLog, 'id' | 'createdAt' | 'type'>) => {
    setLogs((current) => [{
      ...log,
      id: createId('user-log'),
      type: 'USER',
      createdAt: new Date().toISOString(),
    }, ...current])
  }

  const createUser = async (input: CreateManagedUserInput) => {
    const response = await api.admin.createUser(input)
    const createdUser = normalizeManagedUser(response)
    if (createdUser.role !== 'CALISAN' || !createdUser.isActive) {
      throw new Error('Sunucu oluşturulan kullanıcıyı aktif Çalışan olarak döndürmedi.')
    }

    if (apiMode === 'backend') {
      await invalidateAdminData()
      return createdUser
    }

    setUsers((current) => [createdUser, ...current])
    addLog({
      action: 'USER_CREATED',
      actionLabel: 'Hesap oluşturuldu',
      actor: fullName(actor),
      target: fullName(createdUser),
      description: 'Çalışan rolüyle yeni kullanıcı hesabı oluşturuldu.',
    })
    return createdUser
  }

  const changeUserRole = async (
    userId: string,
    role: UserRole,
    replacementDeputyId?: string,
    activeDeputyId?: string,
  ) => {
    if (apiMode === 'backend') {
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
      return
    }

    const target = users.find((user) => user.id === userId)
    if (!target) throw new Error('Kullanıcı bulunamadı.')
    if (target.role === 'ADMIN') throw new Error('Admin rolü bu ekrandan değiştirilemez.')
    if (!target.isActive) throw new Error('Pasif hesabın rolünü değiştirmeden önce hesabı etkinleştirin.')
    if (target.role === role) return

    const currentDeputy = role === 'BASKAN_YARDIMCISI'
      ? users.find((user) => user.id !== userId && user.isActive && user.role === 'BASKAN_YARDIMCISI')
      : undefined
    const previousRole = target.role
    setUsers((current) => current.map((user) => {
      if (user.id === userId) return { ...user, role }
      if (currentDeputy && user.id === currentDeputy.id) return { ...user, role: 'CALISAN' }
      return user
    }))

    if (currentDeputy) {
      addLog({
        action: 'DEPUTY_ROLE_TRANSFERRED',
        actionLabel: 'Yardımcılık rolü devredildi',
        actor: fullName(actor),
        target: `${fullName(currentDeputy)} → ${fullName(target)}`,
        description: `${fullName(currentDeputy)} kullanıcısı Çalışan rolüne alındı; Başkan Yardımcısı rolü ${fullName(target)} kullanıcısına verildi.`,
      })
      return
    }

    addLog({
      action: 'ROLE_CHANGED',
      actionLabel: 'Rol değiştirildi',
      actor: fullName(actor),
      target: fullName(target),
      description: `Kullanıcı rolü ${roleLabels[previousRole]} → ${roleLabels[role]} olarak değiştirildi.`,
    })
  }

  const setUserActive = async (userId: string, isActive: boolean) => {
    if (apiMode === 'backend') {
      await api.admin.setActive({ id: userId }, { active: isActive })
      await invalidateAdminData()
      return
    }

    const target = users.find((user) => user.id === userId)
    if (!target) throw new Error('Kullanıcı bulunamadı.')
    if (target.role === 'ADMIN') throw new Error('Admin hesabı bu ekrandan pasifleştirilemez.')
    if (target.isActive === isActive) return
    if (!isActive && target.role === 'BASKAN_YARDIMCISI') {
      throw new Error('Önce Başkan Yardımcısı rolünü başka bir aktif Çalışana devredin.')
    }

    setUsers((current) => current.map((user) => user.id === userId
      ? { ...user, isActive }
      : user))
    addLog({
      action: isActive ? 'ACCOUNT_ACTIVATED' : 'ACCOUNT_DEACTIVATED',
      actionLabel: isActive ? 'Hesap etkinleştirildi' : 'Hesap pasifleştirildi',
      actor: fullName(actor),
      target: fullName(target),
      description: isActive
        ? 'Kullanıcının sisteme erişimi yeniden açıldı.'
        : 'Kullanıcının sisteme erişimi kapatıldı; aktif oturumları backend tarafından sonlandırılmalıdır.',
    })
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
