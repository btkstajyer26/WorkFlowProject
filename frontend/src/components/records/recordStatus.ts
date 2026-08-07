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
    className: 'bg-app-surface-strong text-app-text-secondary',
    icon: FilePenLine,
  },
  BSK_YRD_INCELEMESINDE: {
    label: 'Bşk. Yrd. İncelemesinde',
    className: 'bg-amber-50 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300',
    icon: Clock3,
  },
  BASKAN_INCELEMESINDE: {
    label: 'Başkan İncelemesinde',
    className: 'bg-blue-50 text-blue-700 dark:bg-blue-950/40 dark:text-blue-300',
    icon: Send,
  },
  DUZENLEME_BEKLIYOR: {
    label: 'Düzenleme Bekliyor',
    className: 'bg-orange-50 text-orange-700 dark:bg-orange-950/40 dark:text-orange-300',
    icon: FilePenLine,
  },
  ONAYLANDI: {
    label: 'Onaylandı',
    className: 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300',
    icon: CheckCircle2,
  },
  REDDEDILDI: {
    label: 'Reddedildi',
    className: 'bg-rose-50 text-rose-700 dark:bg-rose-950/40 dark:text-rose-300',
    icon: CircleX,
  },
}
