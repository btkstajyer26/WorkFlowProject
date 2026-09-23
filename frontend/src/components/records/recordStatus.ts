import { CheckCircle2, CircleX, Clock3, FilePenLine, Hourglass, Send, ShieldCheck, type LucideIcon } from 'lucide-react'
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
  // Parent/Subtask alt akışı (docs/PARENT_SUBTASK_GOREV_DAGILIMI.md).
  ALT_GOREV_BEKLIYOR: {
    label: 'Alt Görevler Bekleniyor',
    className: 'bg-violet-50 text-violet-700 dark:bg-violet-950/40 dark:text-violet-300',
    icon: Hourglass,
  },
  KONTROL: {
    label: 'Kontrol',
    className: 'bg-teal-50 text-teal-700 dark:bg-teal-950/40 dark:text-teal-300',
    icon: ShieldCheck,
  },
}
