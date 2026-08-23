import { apiHttpClient } from './client'

export type ChangePasswordInput = {
  currentPassword: string
  newPassword: string
}

export type ForgotPasswordInput = {
  email: string
}

export type VerifyResetCodeInput = {
  email: string
  code: string
}

export type VerifyResetCodeResult = {
  resetToken: string
  expiresInSeconds: number
}

export type ResetPasswordInput = {
  token: string
  newPassword: string
}

export function changePassword(input: ChangePasswordInput) {
  return apiHttpClient.request<string, unknown>({
    path: '/api/auth/change-password',
    method: 'POST',
    body: input,
    secure: true,
    type: 'application/json',
    format: 'text',
  })
}

// Şifre sıfırlama uçları generated controller'da da mevcut. Bu adapter,
// sayfaların backend yanıt ayrıntılarına bağlanmaması için UI sınırını korur.

/** Hesap varsa e-postaya 6 haneli doğrulama kodu gönderir. */
export function requestPasswordReset(input: ForgotPasswordInput) {
  return apiHttpClient.request<void, unknown>({
    path: '/api/auth/forgot-password',
    method: 'POST',
    body: input,
    type: 'application/json',
  })
}

/** Kodu doğrular; dönen anahtar şifre değiştirme adımında kullanılır. */
export function verifyPasswordResetCode(input: VerifyResetCodeInput) {
  return apiHttpClient.request<VerifyResetCodeResult, unknown>({
    path: '/api/auth/verify-reset-code',
    method: 'POST',
    body: input,
    type: 'application/json',
    format: 'json',
  })
}

export function resetPassword(input: ResetPasswordInput) {
  return apiHttpClient.request<void, unknown>({
    path: '/api/auth/reset-password',
    method: 'POST',
    body: input,
    type: 'application/json',
  })
}
