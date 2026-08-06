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
