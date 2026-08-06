import { CheckCircle2, CircleX, Clock3, FilePenLine, Send, type LucideIcon } from 'lucide-react'
import type { RecordStatus } from '../../types/record'

type StatusMeta = {
  label: string
  className: string
  icon: LucideIcon
}

export const recordStatusMeta: Record<RecordStatus, StatusMeta> = {
  TASLAK: {
    label: 'Taslak',
    className: 'bg-slate-100 text-slate-700',
    icon: FilePenLine,
  },
  BSK_YRD_INCELEMESINDE: {
    label: 'Bşk. Yrd. İncelemesinde',
    className: 'bg-amber-50 text-amber-700',
    icon: Clock3,
  },
  BASKAN_INCELEMESINDE: {
    label: 'Başkan İncelemesinde',
    className: 'bg-blue-50 text-blue-700',
    icon: Send,
  },
  DUZENLEME_BEKLIYOR: {
    label: 'Düzenleme Bekliyor',
    className: 'bg-orange-50 text-orange-700',
    icon: FilePenLine,
  },
  ONAYLANDI: {
    label: 'Onaylandı',
    className: 'bg-emerald-50 text-emerald-700',
    icon: CheckCircle2,
  },
  REDDEDILDI: {
    label: 'Reddedildi',
    className: 'bg-rose-50 text-rose-700',
    icon: CircleX,
  },
}
