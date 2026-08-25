import { z } from 'zod'

export const assignableRoleSchema = z.enum(['CALISAN', 'BASKAN_YARDIMCISI', 'BASKAN', 'ADMIN'])

export const changeRoleSchema = z.object({
  role: assignableRoleSchema,
})

export const createUserSchema = z.object({
  firstName: z
    .string()
    .trim()
    .min(1, 'Ad zorunludur.')
    .max(100, 'Ad en fazla 100 karakter olabilir.'),
  lastName: z
    .string()
    .trim()
    .min(1, 'Soyad zorunludur.')
    .max(100, 'Soyad en fazla 100 karakter olabilir.'),
  email: z
    .string()
    .trim()
    .min(1, 'E-posta adresi zorunludur.')
    .max(150, 'E-posta adresi en fazla 150 karakter olabilir.')
    .email('Geçerli bir e-posta adresi yazın.'),
  password: z
    .string()
    .min(1, 'Şifre zorunludur.')
    .min(6, 'Şifre en az 6 karakter olmalıdır.')
    .max(72, 'Şifre en fazla 72 karakter olabilir.'),
})

export type CreateUserFormValues = z.infer<typeof createUserSchema>
export type ChangeRoleFormValues = z.infer<typeof changeRoleSchema>
