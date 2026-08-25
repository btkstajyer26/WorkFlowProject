import type { RecordStatus } from '../../types/record'
import { recordStatusMeta } from './recordStatus'

export function RecordStatusBadge({ status }: { status: RecordStatus }) {
  const meta = recordStatusMeta[status]
  const Icon = meta.icon

  return (
    <span className={`inline-flex items-center gap-2 rounded-md px-3 py-1.5 text-xs font-bold ${meta.className}`}>
      <Icon className="size-4" aria-hidden="true" />
      {meta.label}
    </span>
  )
}
