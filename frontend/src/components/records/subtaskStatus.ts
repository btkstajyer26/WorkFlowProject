import { CheckCircle2, ClipboardCheck, ClipboardList, CircleX, Wrench, type LucideIcon } from 'lucide-react'
import type { SubtaskStatus } from '../../types/subtask'

type SubtaskStatusMeta = {
  label: string
  className: string
  icon: LucideIcon
}

export const subtaskStatusMeta: Record<SubtaskStatus, SubtaskStatusMeta> = {
  DEGERLENDIRME: {
    label: 'Değerlendirme',
    className: 'bg-amber-50 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300',
    icon: ClipboardList,
  },
  ISLEM: {
    label: 'İşlem',
    className: 'bg-blue-50 text-blue-700 dark:bg-blue-950/40 dark:text-blue-300',
    icon: Wrench,
  },
  ONAY: {
    label: 'Onay',
    className: 'bg-violet-50 text-violet-700 dark:bg-violet-950/40 dark:text-violet-300',
    icon: ClipboardCheck,
  },
  TAMAMLANDI: {
    label: 'Tamamlandı',
    className: 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300',
    icon: CheckCircle2,
  },
  REDDEDILDI: {
    label: 'Reddedildi',
    className: 'bg-rose-50 text-rose-700 dark:bg-rose-950/40 dark:text-rose-300',
    icon: CircleX,
  },
}

export const subtaskTerminalStatuses: SubtaskStatus[] = ['TAMAMLANDI', 'REDDEDILDI']

export function isSubtaskResolved(status: SubtaskStatus) {
  return subtaskTerminalStatuses.includes(status)
}
