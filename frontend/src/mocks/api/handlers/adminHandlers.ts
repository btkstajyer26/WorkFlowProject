import { http, HttpResponse } from 'msw'
import type { CreateUserRequest, UserResponse } from '../../../api/generated/data-contracts'
import { apiBaseUrl } from '../../../api/config'
import { getAuthenticatedMockUser, mockApiUsers } from '../auth'
import { mockApiDb } from '../db'
import { apiErrorResponse, forbiddenResponse, unauthorizedResponse } from '../responses'

export const adminHandlers = [
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
      createdAt,
    }
    mockApiDb.createdUsers.push({
      id: response.id!,
      firstName: response.firstName!,
      lastName: response.lastName!,
      email: response.email!,
      password,
      role: 'CALISAN',
    })
    return HttpResponse.json(response)
  }),
]
