import { useState, type ReactNode } from 'react'
import { api } from '../api/client'
import { ApiClientError } from '../api/errors'
import { mockAdminAuditLogs, mockManagedUsers } from '../mocks/admin'
import { roleLabels, type AuthUser, type UserRole } from '../types/auth'
import type { AdminAuditLog, CreateManagedUserInput, ManagedUser } from '../types/admin'
import { AdminContext, type AdminContextValue } from './adminState'

function fullName(user: Pick<ManagedUser | AuthUser, 'firstName' | 'lastName'>) {
  return `${user.firstName} ${user.lastName}`
}

function createId(prefix: string) {
  return `${prefix}-${crypto.randomUUID()}`
}

export function AdminProvider({ actor, children }: { actor: AuthUser; children: ReactNode }) {
  const [users, setUsers] = useState<ManagedUser[]>(mockManagedUsers)
  const [logs, setLogs] = useState<AdminAuditLog[]>(mockAdminAuditLogs)

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
    if (
      !response.id ||
      !response.firstName ||
      !response.lastName ||
      !response.email ||
      response.roleName !== 'CALISAN' ||
      !response.createdAt
    ) {
      throw new ApiClientError({
        code: 'INVALID_USER_RESPONSE',
        message: 'Sunucu oluşturulan kullanıcı için geçerli bilgiler döndürmedi.',
        status: 0,
      })
    }

    const createdUser: ManagedUser = {
      id: response.id,
      firstName: response.firstName,
      lastName: response.lastName,
      email: response.email,
      role: response.roleName,
      isActive: true,
      mustChangePassword: false,
      createdAt: response.createdAt,
      updatedAt: response.createdAt,
      version: 1,
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

  const changeUserRole = (userId: string, role: UserRole) => {
    const target = users.find((user) => user.id === userId)
    if (!target) throw new Error('Kullanıcı bulunamadı.')
    if (target.role === 'ADMIN') throw new Error('Admin rolü bu ekrandan değiştirilemez.')
    if (!target.isActive) throw new Error('Pasif hesabın rolünü değiştirmeden önce hesabı etkinleştirin.')
    if (target.role === role) return

    const previousRole = target.role
    const currentDeputy = role === 'BASKAN_YARDIMCISI'
      ? users.find((user) => user.id !== userId && user.isActive && user.role === 'BASKAN_YARDIMCISI')
      : undefined
    const now = new Date().toISOString()

    setUsers((current) => current.map((user) => {
      if (user.id === userId) {
        return { ...user, role, updatedAt: now, version: user.version + 1 }
      }
      if (currentDeputy && user.id === currentDeputy.id) {
        return { ...user, role: 'CALISAN', updatedAt: now, version: user.version + 1 }
      }
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

  const setUserActive = (userId: string, isActive: boolean) => {
    const target = users.find((user) => user.id === userId)
    if (!target) throw new Error('Kullanıcı bulunamadı.')
    if (target.role === 'ADMIN') throw new Error('Admin hesabı bu ekrandan pasifleştirilemez.')
    if (target.isActive === isActive) return
    if (!isActive && target.role === 'BASKAN_YARDIMCISI') {
      throw new Error('Önce Başkan Yardımcısı rolünü başka bir aktif kullanıcıya devredin.')
    }

    const now = new Date().toISOString()
    setUsers((current) => current.map((user) => user.id === userId
      ? { ...user, isActive, updatedAt: now, version: user.version + 1 }
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
