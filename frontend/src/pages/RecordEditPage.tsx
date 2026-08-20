import type { UserRole } from '../types/auth'
import { BackendRecordEditPage } from './BackendRecordEditPage'

export function RecordEditPage({ role }: { role: UserRole }) {
  return <BackendRecordEditPage role={role} />
}
