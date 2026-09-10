import type { SystemRoleKey } from './auth'

export type ManagedUser = {
  id: string
  firstName: string
  lastName: string
  email: string
  roleId: number
  /** Yerleşik rolün değişmez anahtarı; dinamik rolde `null`. Kararlar buna bakar. */
  systemKey: SystemRoleKey | null
  /** Yalnız gösterim adı; panelden değiştirilebilir. */
  roleName: string
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
  /** Yerleşik rol: yeniden adlandırılabilir ama pasifleştirilemez. */
  isSystem: boolean
  /** Rolün mevcut geçişlere aktör olarak bağlanabilmesi (WF-8 şartı). */
  isWorkflowActor: boolean
  /** null = sınırsız. Panelden açılan roller daima sınırsızdır. */
  maxUsers: number | null
  isActive: boolean
}

export type CreateAdminRoleInput = {
  name: string
  description?: string
  workflowActor: boolean
}

export type UpdateAdminRoleInput = {
  name?: string
  description?: string
  workflowActor?: boolean
  active?: boolean
}

/**
 * AP-3 kapalı capability katalogu. Admin yeni kod üretemez; bu tip yalnız
 * backend'in Flyway seed'iyle gelen kodları taşır.
 */
export type AdminPermission = {
  id: number
  code: string
  displayName: string
  description: string | null
  isActive: boolean
}

/** Bir rolün o an taşıdığı permission kodları (matrisin tek satırı). */
export type AdminRolePermissions = {
  roleId: number
  roleName: string
  permissionCodes: string[]
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
