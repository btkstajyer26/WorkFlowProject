import { apiHttpClient } from './client'

export type ChangePasswordInput = {
  currentPassword: string
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
