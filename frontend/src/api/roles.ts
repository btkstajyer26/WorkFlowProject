import type { ListRolesData, RoleResponse } from './generated/data-contracts'
import { apiHttpClient } from './client'
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
 * açılabildiği için sunucudan gelen ad olduğu gibi taşınır.
 */
export function normalizeAdminRole(response: RoleResponse): AdminRole {
  if (!Number.isSafeInteger(response.id) || !response.name?.trim()) {
    return invalidRoleResponse('Sunucu geçerli rol bilgisi döndürmedi.')
  }

  return {
    id: response.id!,
    name: response.name.trim(),
    // TODO(AP-2): RoleResponse bugün systemKey taşımıyor; alan tipte duruyor
    // ama uçtan gelmediği için null'a sabitleniyor. AP-2'de sistem rollerinin
    // silinmesini engellemek için gerekli olacak; bu backend DTO değişikliği
    // Alperen'in modülünde.
    systemKey: null,
    description: response.description?.trim() || null,
  }
}

/**
 * `/api/admin/roles` sayfalanmamış düz bir dizi döndürür (`/api/admin/users`'tan
 * farklı olarak `PagedResponse` değildir) ve pasif rolleri hiç göndermez.
 */
export async function listRoles(): Promise<AdminRole[]> {
  const response = await apiHttpClient.request<ListRolesData>({
    path: '/api/admin/roles',
    method: 'GET',
    secure: true,
  })
  return (response ?? []).map(normalizeAdminRole)
}
