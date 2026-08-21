import { queryOptions, useQuery } from '@tanstack/react-query';

import { getCurrentUser } from '@/api/users';

export const currentUserQueryKey = ['users', 'me'] as const;

export const currentUserQueryOptions = queryOptions({
  queryFn: getCurrentUser,
  queryKey: currentUserQueryKey,
  staleTime: 60 * 1000,
});

export function useCurrentUser() {
  return useQuery(currentUserQueryOptions);
}
