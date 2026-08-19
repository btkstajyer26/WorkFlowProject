import { z } from 'zod'

export const loginSchema = z.object({
  email: z
    .string()
    .trim()
    .min(1, 'E-posta adresi zorunludur.')
    .email('Geçerli bir e-posta adresi yazın.'),
  password: z.string().min(1, 'Şifre zorunludur.').min(6, 'Şifre en az 6 karakter olmalıdır.'),
})

export type LoginFormValues = z.infer<typeof loginSchema>

export const forgotPasswordSchema = z.object({
  email: z
    .string()
    .trim()
    .min(1, 'E-posta adresi zorunludur.')
    .email('Geçerli bir e-posta adresi yazın.'),
})

export type ForgotPasswordFormValues = z.infer<typeof forgotPasswordSchema>

const newPasswordSchema = z
  .string()
  .min(1, 'Yeni şifrenizi yazın.')
  .regex(
    /^(?=.*[A-Za-z])(?=.*\d).{8,}$/,
    'Yeni şifre en az 8 karakter olmalı, en az bir harf ve bir rakam içermelidir.',
  )

export const changePasswordSchema = z.object({
  currentPassword: z.string().min(1, 'Mevcut şifrenizi yazın.'),
  newPassword: newPasswordSchema,
  newPasswordConfirm: z.string().min(1, 'Yeni şifrenizi tekrar yazın.'),
}).refine((values) => values.newPassword !== values.currentPassword, {
  path: ['newPassword'],
  message: 'Yeni şifreniz mevcut şifrenizle aynı olamaz.',
}).refine((values) => values.newPassword === values.newPasswordConfirm, {
  path: ['newPasswordConfirm'],
  message: 'Yeni şifreler birbiriyle eşleşmiyor.',
})

export type ChangePasswordFormValues = z.infer<typeof changePasswordSchema>

export const resetPasswordSchema = z.object({
  newPassword: newPasswordSchema,
  newPasswordConfirm: z.string().min(1, 'Yeni şifrenizi tekrar yazın.'),
}).refine((values) => values.newPassword === values.newPasswordConfirm, {
  path: ['newPasswordConfirm'],
  message: 'Yeni şifreler birbiriyle eşleşmiyor.',
})

export type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>
