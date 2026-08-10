import { useState, type ReactNode } from 'react'
import { mockAdminAuditLogs, mockManagedUsers } from '../mocks/admin'
import {
  readMockRegistrationRequests,
  updateMockRegistrationRequestStatus,
} from '../mocks/registrationRequests'
import { roleLabels, type AuthUser, type UserRole, type WorkflowRole } from '../types/auth'
import type { AdminAuditLog, ManagedUser } from '../types/admin'
import type { RegistrationRequest } from '../types/registration'
import { AdminContext, type AdminContextValue } from './adminState'

function fullName(user: Pick<ManagedUser | AuthUser | RegistrationRequest, 'firstName' | 'lastName'>) {
  return `${user.firstName} ${user.lastName}`
}

function createId(prefix: string) {
  return `${prefix}-${crypto.randomUUID()}`
}

export function AdminProvider({ actor, children }: { actor: AuthUser; children: ReactNode }) {
  const [users, setUsers] = useState<ManagedUser[]>(mockManagedUsers)
  const [logs, setLogs] = useState<AdminAuditLog[]>(mockAdminAuditLogs)
  const [registrationRequests, setRegistrationRequests] = useState<RegistrationRequest[]>(
    () => readMockRegistrationRequests().filter((request) => request.status === 'PENDING'),
  )

  const addLog = (log: Omit<AdminAuditLog, 'id' | 'createdAt' | 'type'>) => {
    setLogs((current) => [{
      ...log,
      id: createId('user-log'),
      type: 'USER',
      createdAt: new Date().toISOString(),
    }, ...current])
  }

  const resolveRegistration = (
    requestId: string,
    status: 'APPROVED' | 'REJECTED',
  ) => {
    updateMockRegistrationRequestStatus(requestId, status)
    setRegistrationRequests((current) => current.filter((request) => request.id !== requestId))
  }

  const approveRegistration = (requestId: string, role: WorkflowRole) => {
    const request = registrationRequests.find((item) => item.id === requestId)
    if (!request) throw new Error('Kayıt talebi bulunamadı.')
    if (users.some((user) => user.email.toLowerCase() === request.email.toLowerCase())) {
      throw new Error('Bu e-posta adresiyle kayıtlı bir kullanıcı zaten var.')
    }

    const now = new Date().toISOString()
    const approvedUser: ManagedUser = {
      id: createId('user'),
      firstName: request.firstName,
      lastName: request.lastName,
      email: request.email,
      role,
      isActive: true,
      mustChangePassword: false,
      createdAt: now,
      updatedAt: now,
      version: 1,
    }

    const currentDeputy = role === 'BASKAN_YARDIMCISI'
      ? users.find((user) => user.isActive && user.role === 'BASKAN_YARDIMCISI')
      : undefined

    setUsers((current) => [approvedUser, ...current.map((user) => currentDeputy?.id === user.id
      ? { ...user, role: 'CALISAN' as const, updatedAt: now, version: user.version + 1 }
      : user)])
    resolveRegistration(requestId, 'APPROVED')
    addLog({
      action: 'REGISTRATION_APPROVED',
      actionLabel: 'Kayıt talebi onaylandı',
      actor: fullName(actor),
      target: fullName(approvedUser),
      description: `${roleLabels[approvedUser.role]} rolüyle kullanıcı hesabı etkinleştirildi${currentDeputy ? `; ${fullName(currentDeputy)} Çalışan rolüne alındı` : ''}.`,
    })
  }

  const rejectRegistration = (requestId: string) => {
    const request = registrationRequests.find((item) => item.id === requestId)
    if (!request) throw new Error('Kayıt talebi bulunamadı.')
    resolveRegistration(requestId, 'REJECTED')
    addLog({
      action: 'REGISTRATION_REJECTED',
      actionLabel: 'Kayıt talebi reddedildi',
      actor: fullName(actor),
      target: fullName(request),
      description: `${request.email} adresiyle gönderilen hesap talebi reddedildi.`,
    })
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
    registrationRequests,
    approveRegistration,
    rejectRegistration,
    changeUserRole,
    setUserActive,
  }

  return <AdminContext.Provider value={value}>{children}</AdminContext.Provider>
}
