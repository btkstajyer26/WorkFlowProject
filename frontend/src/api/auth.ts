import { apiHttpClient } from './client'

export type ChangePasswordInput = {
  currentPassword: string
  newPassword: string
}

export type ForgotPasswordInput = {
  email: string
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

// Bu iki uç backend OpenAPI şemasına eklendiğinde generated controller üzerinden
// çağrılabilir. O zamana kadar sözleşme sınırı bu adapterda tutulur.
export function requestPasswordReset(input: ForgotPasswordInput) {
  return apiHttpClient.request<void, unknown>({
    path: '/api/auth/forgot-password',
    method: 'POST',
    body: input,
    type: 'application/json',
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
