import { http, HttpResponse } from 'msw'
import type { LoginRequest, LogoutRequest, RefreshTokenRequest } from '../../../api/generated/data-contracts'
import { apiBaseUrl } from '../../../api/config'
import {
  changeMockPassword,
  consumeMockPasswordReset,
  createMockTokenPair,
  findMockUserByCredentials,
  findMockUserByRefreshToken,
  getAuthenticatedMockUser,
  issueMockPasswordReset,
  verifyMockPasswordResetCode,
} from '../auth'
import { apiErrorResponse, unauthorizedResponse } from '../responses'

/** V12 backfill'indeki yerleşik rol kimlikleri. */
const systemRoleIds = { CALISAN: 1, BASKAN_YARDIMCISI: 2, BASKAN: 3, ADMIN: 4 } as const

type ChangePasswordRequest = {
  currentPassword?: string
  newPassword?: string
}

type ForgotPasswordRequest = {
  email?: string
}

type VerifyResetCodeRequest = {
  email?: string
  code?: string
}

type ResetPasswordRequest = {
  token?: string
  newPassword?: string
}

export const authHandlers = [
  http.get(`${apiBaseUrl}/api/users/me`, ({ request }) => {
    const user = getAuthenticatedMockUser(request)
    if (!user) return unauthorizedResponse()
    return HttpResponse.json({
      id: user.id,
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      roleId: systemRoleIds[user.role],
      systemKey: user.role,
      roleName: user.role,
      active: true,
      createdAt: '2026-08-01T09:00:00Z',
    })
  }),

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
    if (user.password === body.newPassword) {
      return apiErrorResponse(400, 'PASSWORD_REUSED', 'Yeni şifreniz mevcut şifrenizle aynı olamaz')
    }

    changeMockPassword(user, body.newPassword!)
    return HttpResponse.text('Şifre değiştirildi')
  }),

  http.post(`${apiBaseUrl}/api/auth/forgot-password`, async ({ request }) => {
    const body = await request.json() as ForgotPasswordRequest
    issueMockPasswordReset(body.email)

    // Hesabın varlığını dışarı sızdırmamak için her e-posta aynı cevabı alır.
    return new HttpResponse(null, { status: 202 })
  }),

  http.post(`${apiBaseUrl}/api/auth/verify-reset-code`, async ({ request }) => {
    const body = await request.json() as VerifyResetCodeRequest
    const resetToken = verifyMockPasswordResetCode(body.email, body.code)
    if (!resetToken) {
      return apiErrorResponse(400, 'INVALID_OR_EXPIRED_RESET_CODE', 'Doğrulama kodu geçersiz veya süresi dolmuş')
    }

    return HttpResponse.json({ resetToken, expiresInSeconds: 900 })
  }),

  http.post(`${apiBaseUrl}/api/auth/reset-password`, async ({ request }) => {
    const body = await request.json() as ResetPasswordRequest
    const fieldErrors = !body.newPassword
      ? [{ field: 'newPassword', message: 'Yeni şifre boş olamaz' }]
      : !/^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(body.newPassword)
        ? [{ field: 'newPassword', message: 'Şifre en az 8 karakter olmalı, en az bir harf ve bir rakam içermeli' }]
        : []

    if (fieldErrors.length) {
      return apiErrorResponse(400, 'VALIDATION_ERROR', 'Girilen veriler geçersiz', fieldErrors)
    }
    const outcome = consumeMockPasswordReset(body.token, body.newPassword)
    if (outcome === 'invalid-token') {
      return apiErrorResponse(400, 'INVALID_OR_EXPIRED_RESET_TOKEN', 'Şifre sıfırlama anahtarı geçersiz veya süresi dolmuş')
    }
    if (outcome === 'password-reused') {
      return apiErrorResponse(400, 'PASSWORD_REUSED', 'Yeni şifreniz mevcut şifrenizle aynı olamaz')
    }

    return new HttpResponse(null, { status: 204 })
  }),
]
