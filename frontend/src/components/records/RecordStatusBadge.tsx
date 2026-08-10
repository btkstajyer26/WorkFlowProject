import type { RecordStatus } from '../../types/record'
import { recordStatusMeta } from './recordStatus'

export function RecordStatusBadge({ status }: { status: RecordStatus }) {
  const meta = recordStatusMeta[status]
  const Icon = meta.icon

  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] font-bold ${meta.className}`}>
      <Icon className="size-3.5" aria-hidden="true" />
      {meta.label}
    </span>
  )
}
