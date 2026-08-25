import type {
  AuditLogResponse,
  ListAuditLogsData,
  ListUsersData,
  UserAuditLogResponse,
  UserResponse,
} from './generated/data-contracts'
import { apiHttpClient } from './client'
import { ApiClientError } from './errors'
import type { AdminAuditLog, AdminLogType, ManagedUser } from '../types/admin'
import type { UserRole } from '../types/auth'

export type AdminUserListQuery = {
  q?: string
  role?: UserRole
  active?: boolean
  page?: number
  size?: number
  sort?: string[]
}

export type AdminUserListResult = {
  content: ManagedUser[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type AdminAuditLogListResult = {
  content: AdminAuditLog[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

const userRoles: UserRole[] = ['CALISAN', 'BASKAN_YARDIMCISI', 'BASKAN', 'ADMIN']
const userActionLabels: Record<string, string> = {
  USER_CREATED: 'Hesap oluşturuldu',
  ROLE_CHANGED: 'Rol değiştirildi',
  ACCOUNT_ACTIVATED: 'Hesap etkinleştirildi',
  ACCOUNT_DEACTIVATED: 'Hesap pasifleştirildi',
  BOOTSTRAP_ADMIN_CREATED: 'İlk Admin oluşturuldu',
  LOGIN: 'Giriş yapıldı',
  LOGIN_FAILED: 'Giriş başarısız',
  LOGOUT: 'Çıkış yapıldı',
  TOKEN_REFRESH: 'Oturum yenilendi',
  TOKEN_REFRESH_FAILED: 'Oturum yenileme başarısız',
  PASSWORD_CHANGED: 'Şifre değiştirildi',
  HTTP_REQUEST: 'API isteği',
}

function invalidAdminResponse(message: string): never {
  throw new ApiClientError({
    code: 'INVALID_ADMIN_RESPONSE',
    message,
    status: 0,
  })
}

function requirePageNumber(value: number | undefined, field: string, minimum = 0) {
  if (!Number.isSafeInteger(value) || value! < minimum) {
    return invalidAdminResponse(`Sunucu geçerli ${field} bilgisi döndürmedi.`)
  }
  return value!
}

export function normalizeManagedUser(response: UserResponse): ManagedUser {
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
    return invalidAdminResponse('Sunucu geçerli kullanıcı bilgisi döndürmedi.')
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

function normalizeAdminLog(response: UserAuditLogResponse): AdminAuditLog {
  if (!response.id || !response.action || !response.createdAt) {
    return invalidAdminResponse('Sunucu geçerli kullanıcı işlem kaydı döndürmedi.')
  }

  return {
    id: response.id,
    type: 'USER',
    action: response.action,
    actionLabel: userActionLabels[response.action] ?? response.action,
    actor: response.performedByFullName?.trim() || 'Sistem',
    target: response.targetUserFullName?.trim() || response.requestPath?.trim() || 'Bilinmeyen kullanıcı',
    description: response.comment?.trim() || 'Kullanıcı hesabında işlem yapıldı.',
    createdAt: response.createdAt,
    httpMethod: response.httpMethod,
    requestPath: response.requestPath,
    httpStatus: response.httpStatus,
    errorCode: response.errorCode,
  }
}

function normalizeRecordAuditLog(response: AuditLogResponse): AdminAuditLog {
  if (!response.id || !response.action || !response.createdAt) {
    return invalidAdminResponse('Sunucu geçerli evrak/admin işlem kaydı döndürmedi.')
  }

  return {
    id: response.id,
    type: 'RECORD',
    action: response.action,
    actionLabel: userActionLabels[response.action] ?? response.action,
    actor: response.userFullName?.trim() || 'Sistem',
    target: response.requestPath?.trim() || response.recordId || 'Evrak / admin işlemi',
    description: response.comment?.trim() || 'İşlem kaydedildi.',
    createdAt: response.createdAt,
    recordId: response.recordId,
    httpMethod: response.httpMethod,
    requestPath: response.requestPath,
    httpStatus: response.httpStatus,
    errorCode: response.errorCode,
  }
}

function normalizePage<TResponse, TResult>(
  response: {
    content?: TResponse[]
    page?: number
    size?: number
    totalElements?: number
    totalPages?: number
  },
  normalizeItem: (item: TResponse) => TResult,
) {
  return {
    content: (response.content ?? []).map(normalizeItem),
    page: requirePageNumber(response.page, 'sayfa'),
    size: requirePageNumber(response.size, 'sayfa boyutu', 1),
    totalElements: requirePageNumber(response.totalElements, 'toplam öğe sayısı'),
    totalPages: requirePageNumber(response.totalPages, 'toplam sayfa sayısı'),
  }
}

export async function listAdminUsers({
  page = 0,
  size = 10,
  sort = ['createdAt,desc'],
  ...filters
}: AdminUserListQuery = {}): Promise<AdminUserListResult> {
  const response = await apiHttpClient.request<ListUsersData>({
    path: '/api/admin/users',
    method: 'GET',
    query: { ...filters, page, size, sort },
    secure: true,
  })
  return normalizePage(response, normalizeManagedUser)
}

export async function listAllAdminUsers(
  filters: Omit<AdminUserListQuery, 'page' | 'size' | 'sort'> = {},
) {
  const firstPage = await listAdminUsers({ ...filters, page: 0, size: 100 })
  if (firstPage.totalPages <= 1) return firstPage.content

  const remainingPages = await Promise.all(
    Array.from({ length: firstPage.totalPages - 1 }, (_, index) => (
      listAdminUsers({ ...filters, page: index + 1, size: 100 })
    )),
  )
  return [firstPage, ...remainingPages].flatMap((page) => page.content)
}

export async function listAdminAuditLogs({
  page = 0,
  size = 10,
  type = 'USER',
}: {
  page?: number
  size?: number
  type?: AdminLogType
} = {}): Promise<AdminAuditLogListResult> {
  const response = await apiHttpClient.request<ListAuditLogsData>({
    path: '/api/admin/audit-logs',
    method: 'GET',
    query: { page, size, sort: 'createdAt,desc', type },
    secure: true,
  })
  if (type === 'RECORD') {
    return normalizePage(response as { content?: AuditLogResponse[], page?: number, size?: number, totalElements?: number, totalPages?: number }, normalizeRecordAuditLog)
  }
  return normalizePage(response, normalizeAdminLog)
}
