export const queryKeys = {
  currentUser: ['current-user'] as const,
  categories: ['categories'] as const,
  records: {
    all: ['records'] as const,
    lists: () => ['records', 'list'] as const,
    list: (query: object) => ['records', 'list', query] as const,
    details: () => ['records', 'detail'] as const,
    detail: (recordId: string, categoryRevision?: string) => (
      categoryRevision
        ? ['records', 'detail', recordId, categoryRevision] as const
        : ['records', 'detail', recordId] as const
    ),
    files: (recordId: string) => ['records', 'files', recordId] as const,
  },
  notifications: {
    all: ['notifications'] as const,
    lists: () => ['notifications', 'list'] as const,
    list: () => ['notifications', 'list', 'all'] as const,
    unread: ['notifications', 'unread'] as const,
    unreadCount: ['notifications', 'unread-count'] as const,
  },
  admin: {
    all: ['admin'] as const,
    users: {
      all: ['admin', 'users'] as const,
      list: (query: object) => ['admin', 'users', 'list', query] as const,
      options: ['admin', 'users', 'options'] as const,
    },
    roles: {
      all: ['admin', 'roles'] as const,
      list: (includeInactive: boolean) => ['admin', 'roles', 'list', includeInactive] as const,
      permissions: (roleId: number) => ['admin', 'roles', roleId, 'permissions'] as const,
    },
    permissions: {
      all: ['admin', 'permissions'] as const,
      list: ['admin', 'permissions', 'list'] as const,
    },
    actorBindings: {
      all: ['admin', 'actor-bindings'] as const,
      list: ['admin', 'actor-bindings', 'list'] as const,
    },
    departments: {
      all: ['admin', 'departments'] as const,
      list: (includeInactive: boolean) => ['admin', 'departments', 'list', includeInactive] as const,
      members: (departmentId: number) => ['admin', 'departments', departmentId, 'members'] as const,
      routingRules: (departmentId: number) => ['admin', 'departments', departmentId, 'routing-rules'] as const,
    },
    auditLogs: {
      all: ['admin', 'audit-logs'] as const,
      list: (query: object) => ['admin', 'audit-logs', 'list', query] as const,
    },
  },
}
