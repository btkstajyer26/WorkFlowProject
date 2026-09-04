import type { RoleResponse } from './generated/data-contracts'
import { api } from './client'
import { ApiClientError } from './errors'
import type { AdminRole } from '../types/admin'

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
  if (!Number.isSafeInteger(response.id) || !response.name?.trim()) {
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
