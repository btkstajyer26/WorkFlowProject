import { http, HttpResponse } from 'msw'
import type {
  CreateRoleRequest,
  CreateUserRequest,
  ChangeRoleRequest,
  PagedResponseUserResponse,
  RoleResponse,
  UpdateRoleRequest,
  UserAuditLogResponse,
  UserResponse,
  SetActiveRequest,
} from '../../../api/generated/data-contracts'
import { apiBaseUrl } from '../../../api/config'
import { mockAdminAuditLogs, mockAdminRoles, mockManagedUsers } from '../../admin'
import type { AdminRole } from '../../../types/admin'
import { getAuthenticatedMockUser, mockApiUsers } from '../auth'
import { mockApiDb } from '../db'
import { apiErrorResponse, forbiddenResponse, unauthorizedResponse } from '../responses'

/**
 * Rol adı benzersizliği büyük/küçük harf ayrımı yapmaz. Karşılaştırma Türkçe
 * kurallarıyla yapılır (backend ile aynı): "İdari" ile "idari" aynı, "Isıtma"
 * ile "İsıtma" farklıdır.
 */
function sameRoleName(left: string, right: string) {
  return left.toLocaleUpperCase('tr-TR') === right.toLocaleUpperCase('tr-TR')
}

/** Fixture veritabanı gerçeğini tutar; uç yalnız sözleşmedeki alanları verir. */
function toRoleResponse(role: AdminRole): RoleResponse {
  return {
    id: role.id,
    name: role.name,
    description: role.description ?? undefined,
    systemKey: role.systemKey ?? undefined,
    system: role.isSystem,
    workflowActor: role.isWorkflowActor,
    maxUsers: role.maxUsers ?? undefined,
    active: role.isActive,
  }
}

type PagedUserAuditLogResponse = {
  content: UserAuditLogResponse[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export const adminHandlers = [
  http.get(`${apiBaseUrl}/api/admin/users`, ({ request }) => {
    const actor = getAuthenticatedMockUser(request)
    if (!actor) return unauthorizedResponse()
    if (actor.role !== 'ADMIN') return forbiddenResponse()

    const url = new URL(request.url)
    const q = url.searchParams.get('q')?.trim().toLocaleLowerCase('tr-TR') ?? ''
    const role = url.searchParams.get('role')
    const activeParam = url.searchParams.get('active')
    const page = Math.max(Number(url.searchParams.get('page')) || 0, 0)
    const size = Math.max(Number(url.searchParams.get('size')) || 10, 1)
    const active = activeParam === null ? undefined : activeParam === 'true'
    const createdUsers = mockApiDb.createdUsers.map((user) => ({
      id: user.id,
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      role: user.role,
      isActive: true,
      createdAt: new Date().toISOString(),
    }))
    const filtered = [...mockManagedUsers, ...createdUsers]
      .filter((user) => !q || `${user.firstName} ${user.lastName} ${user.email}`.toLocaleLowerCase('tr-TR').includes(q))
      .filter((user) => !role || user.role === role)
      .filter((user) => active === undefined || user.isActive === active)
      .sort((left, right) => right.createdAt.localeCompare(left.createdAt))
    const content: UserResponse[] = filtered
      .slice(page * size, page * size + size)
      .map((user) => ({
        id: user.id,
        firstName: user.firstName,
        lastName: user.lastName,
        email: user.email,
        roleName: user.role,
        active: user.isActive,
        createdAt: user.createdAt,
      }))
    const response: PagedResponseUserResponse = {
      content,
      page,
      size,
      totalElements: filtered.length,
      totalPages: Math.ceil(filtered.length / size),
    }
    return HttpResponse.json(response)
  }),

  /**
   * Uç sayfalanmamış düz bir dizi döndürür. Varsayılan çağrı yalnız aktif
   * rolleri verir; yönetim ekranı `includeInactive=true` gönderir.
   */
  http.get(`${apiBaseUrl}/api/admin/roles`, ({ request }) => {
    const actor = getAuthenticatedMockUser(request)
    if (!actor) return unauthorizedResponse()
    if (actor.role !== 'ADMIN') return forbiddenResponse()

    const includeInactive = new URL(request.url).searchParams.get('includeInactive') === 'true'
    const content: RoleResponse[] = mockAdminRoles
      .filter((role) => includeInactive || role.isActive)
      .map(toRoleResponse)
    return HttpResponse.json(content)
  }),

  http.post(`${apiBaseUrl}/api/admin/roles`, async ({ request }) => {
    const actor = getAuthenticatedMockUser(request)
    if (!actor) return unauthorizedResponse()
    if (actor.role !== 'ADMIN') return forbiddenResponse()

    const body = await request.json() as CreateRoleRequest
    const name = body.name?.trim() ?? ''
    if (!name) {
      return apiErrorResponse(400, 'VALIDATION_ERROR', 'Girilen veriler geçersiz',
        [{ field: 'name', message: 'Rol adı boş olamaz' }])
    }
    const clash = mockAdminRoles.find((role) => sameRoleName(role.name, name))
    if (clash) {
      return apiErrorResponse(400, 'BUSINESS_RULE_VIOLATION', 'Bu rol adı zaten kullanılıyor: ' + clash.name)
    }

    // Panelden açılan rol daima dinamik ve sınırsız kapasitelidir.
    const created: AdminRole = {
      id: Math.max(...mockAdminRoles.map((role) => role.id)) + 1,
      name,
      description: body.description?.trim() || null,
      systemKey: null,
      isSystem: false,
      isWorkflowActor: Boolean(body.workflowActor),
      maxUsers: null,
      isActive: true,
    }
    mockAdminRoles.push(created)
    return HttpResponse.json(toRoleResponse(created))
  }),

  http.patch(`${apiBaseUrl}/api/admin/roles/:id`, async ({ params, request }) => {
    const actor = getAuthenticatedMockUser(request)
    if (!actor) return unauthorizedResponse()
    if (actor.role !== 'ADMIN') return forbiddenResponse()

    const role = mockAdminRoles.find((item) => String(item.id) === params.id)
    if (!role) return apiErrorResponse(400, 'ROLE_NOT_FOUND', 'Rol bulunamadı: ' + params.id)

    const body = await request.json() as UpdateRoleRequest
    if (body.name !== undefined) {
      const name = body.name.trim()
      if (!name) return apiErrorResponse(400, 'BUSINESS_RULE_VIOLATION', 'Rol adı boş olamaz')
      const clash = mockAdminRoles.find((item) => sameRoleName(item.name, name) && item.id !== role.id)
      if (clash) {
        return apiErrorResponse(400, 'BUSINESS_RULE_VIOLATION', 'Bu rol adı zaten kullanılıyor: ' + clash.name)
      }
      role.name = name
    }
    if (body.description !== undefined) role.description = body.description.trim() || null
    if (body.workflowActor !== undefined && body.workflowActor !== role.isWorkflowActor) {
      if (role.isSystem) {
        return apiErrorResponse(400, 'BUSINESS_RULE_VIOLATION',
          'Sistem rolünün workflow aktörlüğü değiştirilemez: ' + role.name)
      }
      role.isWorkflowActor = body.workflowActor
    }
    if (body.active !== undefined && body.active !== role.isActive) {
      if (!body.active && role.isSystem) {
        return apiErrorResponse(400, 'BUSINESS_RULE_VIOLATION', 'Sistem rolü pasifleştirilemez: ' + role.name)
      }
      const activeUsers = mockManagedUsers.filter(
        (user) => user.isActive && user.role === role.systemKey,
      ).length
      if (!body.active && activeUsers > 0) {
        return apiErrorResponse(400, 'BUSINESS_RULE_VIOLATION',
          `Bu rol ${activeUsers} aktif kullanıcıda; önce onların rolünü değiştirin: ${role.name}`)
      }
      role.isActive = body.active
    }

    return HttpResponse.json(toRoleResponse(role))
  }),

  http.get(`${apiBaseUrl}/api/admin/audit-logs`, ({ request }) => {
    const actor = getAuthenticatedMockUser(request)
    if (!actor) return unauthorizedResponse()
    if (actor.role !== 'ADMIN') return forbiddenResponse()

    const url = new URL(request.url)
    const page = Math.max(Number(url.searchParams.get('page')) || 0, 0)
    const size = Math.max(Number(url.searchParams.get('size')) || 10, 1)
    const userLogs = mockAdminAuditLogs.filter((log) => log.type === 'USER')
    const content: UserAuditLogResponse[] = userLogs
      .slice(page * size, page * size + size)
      .map((log) => ({
        id: log.id,
        action: log.action,
        comment: log.description,
        performedByFullName: log.actor,
        targetUserFullName: log.target,
        createdAt: log.createdAt,
      }))
    const response: PagedUserAuditLogResponse = {
      content,
      page,
      size,
      totalElements: userLogs.length,
      totalPages: Math.ceil(userLogs.length / size),
    }
    return HttpResponse.json(response)
  }),

  http.post(`${apiBaseUrl}/api/admin/users`, async ({ request }) => {
    const body = await request.json() as CreateUserRequest
    const fieldErrors = [
      ...(!body.firstName?.trim() ? [{ field: 'firstName', message: 'Ad boş olamaz' }] : []),
      ...(!body.lastName?.trim() ? [{ field: 'lastName', message: 'Soyad boş olamaz' }] : []),
      ...(!body.email?.trim()
        ? [{ field: 'email', message: 'Email boş olamaz' }]
        : !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(body.email.trim())
          ? [{ field: 'email', message: 'Geçerli bir email adresi girin' }]
          : []),
      ...(!body.password
        ? [{ field: 'password', message: 'Şifre boş olamaz' }]
        : body.password.length < 6
          ? [{ field: 'password', message: 'Şifre en az 6 karakter olmalı' }]
          : []),
    ]

    if (fieldErrors.length) {
      return apiErrorResponse(
        400,
        'VALIDATION_ERROR',
        'Girilen veriler geçersiz',
        fieldErrors,
      )
    }

    const actor = getAuthenticatedMockUser(request)
    if (!actor) return unauthorizedResponse()
    if (actor.role !== 'ADMIN') return forbiddenResponse()

    const firstName = body.firstName!.trim()
    const lastName = body.lastName!.trim()
    const normalizedEmail = body.email!.trim().toLowerCase()
    const password = body.password!
    if (
      mockApiUsers.some((user) => user.email === normalizedEmail) ||
      mockApiDb.createdUsers.some((user) => user.email === normalizedEmail)
    ) {
      return apiErrorResponse(409, 'CONFLICT', 'Bu e-posta adresiyle kayıtlı bir kullanıcı zaten var')
    }

    const createdAt = new Date().toISOString()
    const response: UserResponse = {
      id: crypto.randomUUID(),
      firstName,
      lastName,
      email: normalizedEmail,
      roleName: 'CALISAN',
      active: true,
      createdAt,
    }
    mockApiDb.createdUsers.push({
      id: response.id!,
      firstName: response.firstName!,
      lastName: response.lastName!,
      email: response.email!,
      password,
      role: 'CALISAN',
      mustChangePassword: true,
    })
    return HttpResponse.json(response)
  }),

  http.patch(`${apiBaseUrl}/api/admin/users/:id/role`, async ({ params, request }) => {
    const actor = getAuthenticatedMockUser(request)
    if (!actor) return unauthorizedResponse()
    if (actor.role !== 'ADMIN') return forbiddenResponse()

    const body = await request.json() as ChangeRoleRequest
    const user = mockManagedUsers.find((item) => item.id === params.id)
    if (!user) return apiErrorResponse(404, 'NOT_FOUND', 'Kullanıcı bulunamadı')

    if (body.replacementBaskanYardimcisiId) {
      const replacement = mockManagedUsers.find((item) => item.id === body.replacementBaskanYardimcisiId)
      if (!replacement || !replacement.isActive || replacement.role !== 'CALISAN') {
        return apiErrorResponse(400, 'INVALID_REPLACEMENT', 'Yerine atanacak aktif Çalışan bulunamadı')
      }
      replacement.role = 'BASKAN_YARDIMCISI'
    }
    user.role = body.roleName as typeof user.role

    return HttpResponse.json({
      id: user.id,
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      roleName: user.role,
      active: user.isActive,
      createdAt: user.createdAt,
    } satisfies UserResponse)
  }),

  http.patch(`${apiBaseUrl}/api/admin/users/:id/active`, async ({ params, request }) => {
    const actor = getAuthenticatedMockUser(request)
    if (!actor) return unauthorizedResponse()
    if (actor.role !== 'ADMIN') return forbiddenResponse()

    const body = await request.json() as SetActiveRequest
    const user = mockManagedUsers.find((item) => item.id === params.id)
    if (!user) return apiErrorResponse(404, 'NOT_FOUND', 'Kullanıcı bulunamadı')
    if (!body.active && user.role === 'BASKAN_YARDIMCISI') {
      return apiErrorResponse(400, 'ACTIVE_DEPUTY_REQUIRED', 'Önce Başkan Yardımcısı rolünü başka bir aktif Çalışana devredin.')
    }
    user.isActive = body.active

    return HttpResponse.json({
      id: user.id,
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      roleName: user.role,
      active: user.isActive,
      createdAt: user.createdAt,
    } satisfies UserResponse)
  }),
]
