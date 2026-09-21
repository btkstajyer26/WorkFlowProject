import type { SubtaskStatus } from '../../types/subtask'
import { subtaskStatusMeta } from './subtaskStatus'

export function SubtaskStatusBadge({ status }: { status: SubtaskStatus }) {
  const meta = subtaskStatusMeta[status]
  const Icon = meta.icon

  return (
    <span className={`inline-flex items-center gap-1.5 rounded-md px-2.5 py-1 text-xs font-bold ${meta.className}`}>
      <Icon className="size-3.5" aria-hidden="true" />
      {meta.label}
    </span>
  )
}
