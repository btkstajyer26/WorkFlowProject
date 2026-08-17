import { useQuery } from '@tanstack/react-query'
import { listAllAdminUsers } from '../api/admin'
import { apiMode } from '../api/config'
import { queryKeys } from '../query/queryKeys'
import type { ManagedUser } from '../types/admin'

export function useAdminUserOptions(mockUsers: ManagedUser[], enabled: boolean) {
  const backendMode = apiMode === 'backend'
  const query = useQuery({
    queryKey: queryKeys.admin.users.options,
    queryFn: () => listAllAdminUsers(),
    enabled: backendMode && enabled,
  })

  return {
    users: backendMode ? query.data ?? [] : mockUsers,
    isPending: backendMode && query.isPending,
    isError: backendMode && query.isError,
  }
}
