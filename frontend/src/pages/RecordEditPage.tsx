import type { SystemRoleKey } from '../types/auth'
import { BackendRecordEditPage } from './BackendRecordEditPage'

export function RecordEditPage({ systemKey }: { systemKey: SystemRoleKey | null }) {
  return <BackendRecordEditPage systemKey={systemKey} />
}
