import { http, HttpResponse } from 'msw'
import type {
  CreateUserRequest,
  ChangeRoleRequest,
  PagedResponseUserResponse,
  RoleResponse,
  UserAuditLogResponse,
  UserResponse,
  SetActiveRequest,
} from '../../../api/generated/data-contracts'
import { apiBaseUrl } from '../../../api/config'
import { mockAdminAuditLogs, mockAdminRoles, mockManagedUsers } from '../../admin'
import { getAuthenticatedMockUser, mockApiUsers } from '../auth'
import { mockApiDb } from '../db'
import { apiErrorResponse, forbiddenResponse, unauthorizedResponse } from '../responses'

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
   * Uç sayfalanmamış düz bir dizi döndürür ve pasif rolleri hiç göndermez.
   * `RoleResponse` id/name/description'dan ibaret olduğu için fixture'daki
   * `systemKey` dışarı verilmez.
   */
  http.get(`${apiBaseUrl}/api/admin/roles`, ({ request }) => {
    const actor = getAuthenticatedMockUser(request)
    if (!actor) return unauthorizedResponse()
    if (actor.role !== 'ADMIN') return forbiddenResponse()

    const content: RoleResponse[] = mockAdminRoles.map((role) => ({
      id: role.id,
      name: role.name,
      description: role.description ?? undefined,
    }))
    return HttpResponse.json(content)
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
