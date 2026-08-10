import { z } from 'zod'

export const assignableRoleSchema = z.enum(['CALISAN', 'BASKAN_YARDIMCISI', 'BASKAN', 'ADMIN'])

export const changeRoleSchema = z.object({
  role: assignableRoleSchema,
})

export type ChangeRoleFormValues = z.infer<typeof changeRoleSchema>
