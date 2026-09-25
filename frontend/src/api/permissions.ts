import type {
  PermissionResponse,
  RolePermissionsResponse,
  UpdateRolePermissionsRequest,
} from './generated/data-contracts'
import { api } from './client'
import { ApiClientError } from './errors'
import type { AdminPermission, AdminRolePermissions } from '../types/admin'

function invalidPermissionResponse(message: string): never {
  throw new ApiClientError({
    code: 'INVALID_PERMISSION_RESPONSE',
    message,
    status: 0,
  })
}

function normalizePermission(response: PermissionResponse): AdminPermission {
  if (
    !Number.isSafeInteger(response.id) ||
    !response.code?.trim() ||
    !response.displayName?.trim() ||
    typeof response.active !== 'boolean'
  ) {
    return invalidPermissionResponse('Sunucu geçerli permission bilgisi döndürmedi.')
  }

  return {
    id: response.id!,
    code: response.code.trim(),
    displayName: response.displayName.trim(),
    description: response.description?.trim() || null,
    isActive: response.active === true,
  }
}

function normalizeRolePermissions(response: RolePermissionsResponse): AdminRolePermissions {
  if (!Number.isSafeInteger(response.roleId) || !response.roleName?.trim()) {
    return invalidPermissionResponse('Sunucu geçerli rol-permission bilgisi döndürmedi.')
  }

  return {
    roleId: response.roleId!,
    roleName: response.roleName.trim(),
    permissionCodes: [...(response.permissionCodes ?? [])].sort(),
  }
}

/** Kapalı capability katalogunun tamamı (aktif + pasif). */
export async function listPermissions(): Promise<AdminPermission[]> {
  const response = await api.permissions.listPermissions()
  return (response ?? []).map(normalizePermission)
}

export async function getRolePermissions(roleId: number): Promise<AdminRolePermissions> {
  const response = await api.permissions.getRolePermissions({ id: roleId })
  return normalizeRolePermissions(response)
}

/**
 * Tam değişim (replace): `permissionCodes` rolün yeni hali olur, kısmi
 * PATCH değildir. Boş dizi rolün bütün yetkilerini kaldırır.
 */
export async function updateRolePermissions(
  roleId: number,
  permissionCodes: string[],
): Promise<AdminRolePermissions> {
  const body: UpdateRolePermissionsRequest = { permissionCodes }
  const response = await api.permissions.updateRolePermissions({ id: roleId }, body)
  return normalizeRolePermissions(response)
}
