import { http, HttpResponse } from 'msw'
import type { CreateUserRequest, UserResponse } from '../../../api/generated/data-contracts'
import { apiBaseUrl } from '../../../api/config'
import { getAuthenticatedMockUser } from '../auth'
import { mockApiDb } from '../db'
import { apiErrorResponse, forbiddenResponse, unauthorizedResponse } from '../responses'

export const adminHandlers = [
  http.post(`${apiBaseUrl}/api/admin/users`, async ({ request }) => {
    const actor = getAuthenticatedMockUser(request)
    if (!actor) return unauthorizedResponse()
    if (actor.role !== 'ADMIN') return forbiddenResponse()

    const body = await request.json() as CreateUserRequest
    if (!body.firstName?.trim() || !body.lastName?.trim() || !body.email?.trim() || !body.password) {
      return apiErrorResponse(400, 'BAD_REQUEST', 'Kullanıcı bilgileri eksik')
    }

    const createdAt = new Date().toISOString()
    const response: UserResponse = {
      id: crypto.randomUUID(),
      firstName: body.firstName.trim(),
      lastName: body.lastName.trim(),
      email: body.email.trim().toLowerCase(),
      roleName: body.roleName,
      createdAt,
    }
    mockApiDb.createdUsers.push({
      id: response.id!,
      firstName: response.firstName!,
      lastName: response.lastName!,
      email: response.email!,
      password: body.password,
      role: body.roleName === 'ADMIN' ? 'ADMIN' : 'CALISAN',
    })
    return HttpResponse.json(response)
  }),
]
