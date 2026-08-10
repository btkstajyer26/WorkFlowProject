import { z } from 'zod'

export const assignableRoleSchema = z.enum(['CALISAN', 'BASKAN_YARDIMCISI', 'BASKAN', 'ADMIN'])

export const createUserSchema = z.object({
  firstName: z.string().trim().min(2, 'Ad en az 2 karakter olmalıdır.').max(100),
  lastName: z.string().trim().min(2, 'Soyad en az 2 karakter olmalıdır.').max(100),
  email: z.string().trim().toLowerCase().email('Geçerli bir e-posta adresi girin.').max(150),
})

export type CreateUserFormValues = z.infer<typeof createUserSchema>

export const changeRoleSchema = z.object({
  role: assignableRoleSchema,
})

export type ChangeRoleFormValues = z.infer<typeof changeRoleSchema>
