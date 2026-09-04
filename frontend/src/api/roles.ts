import type {
  CreateRoleData,
  CreateRoleRequest,
  ListRolesData,
  RoleResponse,
  UpdateRoleData,
  UpdateRoleRequest,
} from './generated/data-contracts'
import { apiHttpClient } from './client'
import { ApiClientError } from './errors'
import type { AdminRole, CreateAdminRoleInput, UpdateAdminRoleInput } from '../types/admin'

function invalidRoleResponse(message: string): never {
  throw new ApiClientError({
    code: 'INVALID_ROLE_RESPONSE',
    message,
    status: 0,
  })
}

/**
 * Rol adı sabit bir listeye karşı doğrulanmaz: panelden dinamik rol
 * açılabildiği için sunucudan gelen ad olduğu gibi taşınır.
 */
export function normalizeAdminRole(response: RoleResponse): AdminRole {
  if (
    !Number.isSafeInteger(response.id) ||
    !response.name?.trim() ||
    typeof response.system !== 'boolean' ||
    typeof response.workflowActor !== 'boolean' ||
    typeof response.active !== 'boolean'
  ) {
    return invalidRoleResponse('Sunucu geçerli rol bilgisi döndürmedi.')
  }

  return {
    id: response.id!,
    name: response.name.trim(),
    // Yerleşik rolün değişmez anahtarı; dinamik rollerde yoktur.
    systemKey: response.systemKey?.trim() || null,
    description: response.description?.trim() || null,
    isSystem: response.system,
    isWorkflowActor: response.workflowActor,
    maxUsers: response.maxUsers ?? null,
    isActive: response.active,
  }
}

/**
 * `/api/admin/roles` sayfalanmamış düz bir dizi döndürür (`/api/admin/users`'tan
 * farklı olarak `PagedResponse` değildir). Varsayılan çağrı yalnız atanabilir
 * rolleri getirir; yönetim ekranı pasifleri de görmek için `includeInactive`
 * gönderir, aksi halde pasifleştirdiği rol listeden düşer ve geri açılamaz.
 */
export async function listRoles(includeInactive = false): Promise<AdminRole[]> {
  const response = await apiHttpClient.request<ListRolesData>({
    path: '/api/admin/roles',
    method: 'GET',
    query: { includeInactive },
    secure: true,
  })
  return (response ?? []).map(normalizeAdminRole)
}

export async function createRole(input: CreateAdminRoleInput): Promise<AdminRole> {
  const body: CreateRoleRequest = {
    name: input.name.trim(),
    description: input.description?.trim() || undefined,
    workflowActor: input.workflowActor,
  }
  const response = await apiHttpClient.request<CreateRoleData>({
    path: '/api/admin/roles',
    method: 'POST',
    body,
    type: 'application/json',
    secure: true,
  })
  return normalizeAdminRole(response)
}

/** Kısmi güncelleme: yalnız verilen alanlar gönderilir. */
export async function updateRole(id: number, input: UpdateAdminRoleInput): Promise<AdminRole> {
  const body: UpdateRoleRequest = {
    ...(input.name === undefined ? {} : { name: input.name.trim() }),
    ...(input.description === undefined ? {} : { description: input.description.trim() }),
    ...(input.workflowActor === undefined ? {} : { workflowActor: input.workflowActor }),
    ...(input.active === undefined ? {} : { active: input.active }),
  }
  const response = await apiHttpClient.request<UpdateRoleData>({
    path: `/api/admin/roles/${id}`,
    method: 'PATCH',
    body,
    type: 'application/json',
    secure: true,
  })
  return normalizeAdminRole(response)
}
