import { useCallback, useEffect, useState, type ReactNode } from 'react'
import { listAdminAuditLogs, listAdminUsers } from '../api/admin'
import { api } from '../api/client'
import { apiMode } from '../api/config'
import { ApiClientError } from '../api/errors'
import type { UserAuditLogResponse, UserResponse } from '../api/generated/data-contracts'
import { mockAdminAuditLogs, mockManagedUsers } from '../mocks/admin'
import type { AdminAuditLog, CreateManagedUserInput, ManagedUser } from '../types/admin'
import { roleLabels, type AuthUser, type UserRole } from '../types/auth'
import { AdminContext, type AdminContextValue } from './adminState'

function fullName(user: Pick<ManagedUser | AuthUser, 'firstName' | 'lastName'>) {
  return `${user.firstName} ${user.lastName}`
}

function createId(prefix: string) {
  return `${prefix}-${crypto.randomUUID()}`
}

const userRoles: UserRole[] = ['CALISAN', 'BASKAN_YARDIMCISI', 'BASKAN', 'ADMIN']

function normalizeManagedUser(response: UserResponse): ManagedUser {
  const role = response.roleName as UserRole | undefined
  if (
    !response.id ||
    !response.firstName?.trim() ||
    !response.lastName?.trim() ||
    !response.email?.trim() ||
    !role ||
    !userRoles.includes(role) ||
    typeof response.active !== 'boolean' ||
    !response.createdAt
  ) {
    throw new ApiClientError({
      code: 'INVALID_USER_RESPONSE',
      message: 'Sunucu geçerli kullanıcı bilgisi döndürmedi.',
      status: 0,
    })
  }

  return {
    id: response.id,
    firstName: response.firstName.trim(),
    lastName: response.lastName.trim(),
    email: response.email.trim().toLowerCase(),
    role,
    isActive: response.active,
    createdAt: response.createdAt,
  }
}

const userActionLabels: Record<string, string> = {
  USER_CREATED: 'Hesap oluşturuldu',
  ROLE_CHANGED: 'Rol değiştirildi',
  ACCOUNT_ACTIVATED: 'Hesap etkinleştirildi',
  ACCOUNT_DEACTIVATED: 'Hesap pasifleştirildi',
  BOOTSTRAP_ADMIN_CREATED: 'İlk Admin oluşturuldu',
}

function normalizeAdminLog(response: UserAuditLogResponse): AdminAuditLog {
  if (!response.id || !response.action || !response.targetUserFullName?.trim() || !response.createdAt) {
    throw new ApiClientError({
      code: 'INVALID_ADMIN_AUDIT_RESPONSE',
      message: 'Sunucu geçerli kullanıcı işlem kaydı döndürmedi.',
      status: 0,
    })
  }

  return {
    id: response.id,
    type: 'USER',
    action: response.action,
    actionLabel: userActionLabels[response.action] ?? response.action,
    actor: response.performedByFullName?.trim() || 'Sistem',
    target: response.targetUserFullName.trim(),
    description: response.comment?.trim() || 'Kullanıcı hesabında işlem yapıldı.',
    createdAt: response.createdAt,
  }
}

async function fetchAdminData() {
  const [userPage, logPage] = await Promise.all([
    listAdminUsers(),
    listAdminAuditLogs(),
  ])
  return {
    users: (userPage.content ?? []).map(normalizeManagedUser),
    logs: (logPage.content ?? []).map(normalizeAdminLog),
  }
}

export function AdminProvider({ actor, children }: { actor: AuthUser; children: ReactNode }) {
  const [users, setUsers] = useState<ManagedUser[]>(() => apiMode === 'mock' ? mockManagedUsers : [])
  const [logs, setLogs] = useState<AdminAuditLog[]>(() => apiMode === 'mock'
    ? mockAdminAuditLogs.filter((log) => log.type === 'USER')
    : [])
  const [loadStatus, setLoadStatus] = useState<'loading' | 'ready' | 'error'>(
    apiMode === 'mock' ? 'ready' : 'loading',
  )

  const reloadAdminData = useCallback(async () => {
    if (apiMode === 'mock') return
    const data = await fetchAdminData()
    setUsers(data.users)
    setLogs(data.logs)
  }, [])

  const retryLoad = useCallback(async () => {
    setLoadStatus('loading')
    try {
      await reloadAdminData()
      setLoadStatus('ready')
    } catch (error) {
      setLoadStatus('error')
      throw error
    }
  }, [reloadAdminData])

  useEffect(() => {
    if (apiMode === 'mock') return
    let active = true

    void fetchAdminData()
      .then((data) => {
        if (!active) return
        setUsers(data.users)
        setLogs(data.logs)
        setLoadStatus('ready')
      })
      .catch(() => {
        if (active) setLoadStatus('error')
      })

    return () => {
      active = false
    }
  }, [])

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
    if (response.roleName !== 'CALISAN' || response.active !== true) {
      throw new ApiClientError({
        code: 'INVALID_USER_RESPONSE',
        message: 'Sunucu oluşturulan kullanıcıyı aktif Çalışan olarak döndürmedi.',
        status: 0,
      })
    }
    const createdUser = normalizeManagedUser(response)

    if (apiMode === 'mock') {
      setUsers((current) => [createdUser, ...current])
      addLog({
        action: 'USER_CREATED',
        actionLabel: 'Hesap oluşturuldu',
        actor: fullName(actor),
        target: fullName(createdUser),
        description: 'Çalışan rolüyle yeni kullanıcı hesabı oluşturuldu.',
      })
    } else {
      await reloadAdminData()
    }
    return createdUser
  }

  const changeUserRole = async (userId: string, role: UserRole, replacementDeputyId?: string) => {
    const target = users.find((user) => user.id === userId)
    if (!target) throw new Error('Kullanıcı bulunamadı.')
    if (target.role === 'ADMIN') throw new Error('Admin rolü bu ekrandan değiştirilemez.')
    if (!target.isActive) throw new Error('Pasif hesabın rolünü değiştirmeden önce hesabı etkinleştirin.')
    if (target.role === role) return

    const currentDeputy = role === 'BASKAN_YARDIMCISI'
      ? users.find((user) => user.id !== userId && user.isActive && user.role === 'BASKAN_YARDIMCISI')
      : undefined

    if (apiMode === 'backend') {
      if (currentDeputy) {
        await api.admin.changeRole({ id: currentDeputy.id }, {
          roleName: 'CALISAN',
          replacementBaskanYardimcisiId: userId,
        })
      } else {
        await api.admin.changeRole({ id: userId }, {
          roleName: role,
          ...(replacementDeputyId ? { replacementBaskanYardimcisiId: replacementDeputyId } : {}),
        })
      }
      await reloadAdminData()
      return
    }

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
    const target = users.find((user) => user.id === userId)
    if (!target) throw new Error('Kullanıcı bulunamadı.')
    if (target.role === 'ADMIN') throw new Error('Admin hesabı bu ekrandan pasifleştirilemez.')
    if (target.isActive === isActive) return
    if (!isActive && target.role === 'BASKAN_YARDIMCISI') {
      throw new Error('Önce Başkan Yardımcısı rolünü başka bir aktif Çalışana devredin.')
    }

    if (apiMode === 'backend') {
      await api.admin.setActive({ id: userId }, { active: isActive })
      await reloadAdminData()
      return
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
    loadStatus,
    retryLoad,
    createUser,
    changeUserRole,
    setUserActive,
  }

  return <AdminContext.Provider value={value}>{children}</AdminContext.Provider>
}
