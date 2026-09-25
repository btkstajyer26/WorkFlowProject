import type { CreateRoleRequest, RoleResponse, UpdateRoleRequest } from './generated/data-contracts'
import { api } from './client'
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
 * açılabildiği için sunucudan gelen ad olduğu gibi taşınır. Yerleşik rolün
 * değişmez anlamı `systemKey` ile gelir; `null` ise rol dinamiktir.
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
    systemKey: response.systemKey?.trim() || null,
    description: response.description?.trim() || null,
    isSystem: response.system === true,
    isWorkflowActor: response.workflowActor === true,
    maxUsers: typeof response.maxUsers === 'number' ? response.maxUsers : null,
    isActive: response.active === true,
  }
}

/**
 * `/api/admin/roles` sayfalanmamış düz bir dizi döndürür (`/api/admin/users`'tan
 * farklı olarak `PagedResponse` değildir). Varsayılan çağrı yalnız atanabilir
 * (aktif) rolleri döner; yönetim ekranı pasifleri de görmek için
 * `includeInactive` gönderir.
 */
export async function listRoles(includeInactive = false): Promise<AdminRole[]> {
  const response = await api.roles.listRoles({ includeInactive })
  return (response ?? []).map(normalizeAdminRole)
}

export async function createRole(input: CreateAdminRoleInput): Promise<AdminRole> {
  const body: CreateRoleRequest = {
    name: input.name.trim(),
    description: input.description?.trim() || undefined,
    workflowActor: input.workflowActor,
  }
  const response = await api.roles.createRole(body)
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
  const response = await api.roles.updateRole({ id }, body)
  return normalizeAdminRole(response)
}
