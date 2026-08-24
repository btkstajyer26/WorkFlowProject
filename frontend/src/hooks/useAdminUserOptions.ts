import { useQuery } from '@tanstack/react-query'
import { listAllAdminUsers } from '../api/admin'
import { queryKeys } from '../query/queryKeys'

export function useAdminUserOptions(enabled: boolean) {
  const query = useQuery({
    queryKey: queryKeys.admin.users.options,
    queryFn: () => listAllAdminUsers(),
    enabled,
  })

  return {
    users: query.data ?? [],
    isPending: query.isPending,
    isError: query.isError,
  }
}
