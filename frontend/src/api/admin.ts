import type { ListAuditLogsData, ListUsersData } from './generated/data-contracts'
import { apiHttpClient } from './client'

type AdminUserListQuery = {
  q?: string
  role?: string
  active?: boolean
  page?: number
  size?: number
  sort?: string[]
}

export function listAdminUsers({
  page = 0,
  size = 100,
  sort = ['createdAt,desc'],
  ...filters
}: AdminUserListQuery = {}) {
  return apiHttpClient.request<ListUsersData>({
    path: '/api/admin/users',
    method: 'GET',
    query: { ...filters, page, size, sort },
    secure: true,
  })
}

export function listAdminAuditLogs({
  page = 0,
  size = 100,
}: {
  page?: number
  size?: number
} = {}) {
  return apiHttpClient.request<ListAuditLogsData>({
    path: '/api/admin/audit-logs',
    method: 'GET',
    query: { page, size },
    secure: true,
  })
}
