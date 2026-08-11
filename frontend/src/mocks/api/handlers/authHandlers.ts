import { http, HttpResponse } from 'msw'
import type { LoginRequest, LogoutRequest, RefreshTokenRequest } from '../../../api/generated/data-contracts'
import { apiBaseUrl } from '../../../api/config'
import {
  createMockTokenPair,
  findMockUserByCredentials,
  findMockUserByRefreshToken,
} from '../auth'
import { apiErrorResponse } from '../responses'

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
]
