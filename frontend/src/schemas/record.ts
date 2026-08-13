import { z } from 'zod'
import { maxRecordTitleLength } from '../config/records'

export const recordFormSchema = z.object({
  title: z
    .string()
    .trim()
    .min(1, 'Başlık zorunludur.')
    .max(maxRecordTitleLength, `Başlık en fazla ${maxRecordTitleLength} karakter olabilir.`),
  categoryId: z.number().int().positive('Geçerli bir kategori seçin.'),
  description: z.string().trim().min(1, 'Açıklama zorunludur.'),
})

export type RecordFormValues = z.infer<typeof recordFormSchema>
