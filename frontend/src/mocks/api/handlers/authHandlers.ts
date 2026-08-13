import { http, HttpResponse } from 'msw'
import type { LoginRequest, LogoutRequest, RefreshTokenRequest } from '../../../api/generated/data-contracts'
import { apiBaseUrl } from '../../../api/config'
import {
  changeMockPassword,
  createMockTokenPair,
  findMockUserByCredentials,
  findMockUserByRefreshToken,
  getAuthenticatedMockUser,
} from '../auth'
import { apiErrorResponse, unauthorizedResponse } from '../responses'

type ChangePasswordRequest = {
  currentPassword?: string
  newPassword?: string
}

export const authHandlers = [
  http.post(`${apiBaseUrl}/api/auth/login`, async ({ request }) => {
    const body = await request.json() as LoginRequest
    const user = findMockUserByCredentials(body.email, body.password)
    if (!user) {
      return apiErrorResponse(401, 'UNAUTHORIZED', 'E-posta adresi veya şifre hatalı')
    }

    return HttpResponse.json(createMockTokenPair(user))
  }),

  http.post(`${apiBaseUrl}/api/auth/refresh`, async ({ request }) => {
    const body = await request.json() as RefreshTokenRequest
    const user = findMockUserByRefreshToken(body.refreshToken)
    if (!user) return apiErrorResponse(401, 'UNAUTHORIZED', 'Refresh token geçersiz')
    return HttpResponse.json(createMockTokenPair(user))
  }),

  http.post(`${apiBaseUrl}/api/auth/logout`, async ({ request }) => {
    const body = await request.json() as LogoutRequest
    if (!findMockUserByRefreshToken(body.refreshToken)) {
      return apiErrorResponse(401, 'UNAUTHORIZED', 'Refresh token geçersiz')
    }
    return new HttpResponse('Çıkış yapıldı', { status: 200 })
  }),

  http.post(`${apiBaseUrl}/api/auth/change-password`, async ({ request }): Promise<Response> => {
    const user = getAuthenticatedMockUser(request)
    if (!user) return unauthorizedResponse()

    const body = await request.json() as ChangePasswordRequest
    const fieldErrors = [
      ...(!body.currentPassword
        ? [{ field: 'currentPassword', message: 'Mevcut şifre boş olamaz' }]
        : []),
      ...(!body.newPassword
        ? [{ field: 'newPassword', message: 'Yeni şifre boş olamaz' }]
        : !/^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(body.newPassword)
          ? [{ field: 'newPassword', message: 'Şifre en az 8 karakter olmalı, en az bir harf ve bir rakam içermeli' }]
          : []),
    ]

    if (fieldErrors.length) {
      return apiErrorResponse(400, 'VALIDATION_ERROR', 'Girilen veriler geçersiz', fieldErrors)
    }
    if (user.password !== body.currentPassword) {
      return apiErrorResponse(401, 'INVALID_CREDENTIALS', 'Mevcut şifre yanlış')
    }

    changeMockPassword(user, body.newPassword!)
    return HttpResponse.text('Şifre değiştirildi')
  }),
]
