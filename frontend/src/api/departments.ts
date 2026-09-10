import type {
  CreateDepartmentRequest,
  DepartmentResponse,
  DepartmentMembersResponse,
  UpdateDepartmentRequest,
} from './generated/data-contracts'
import { api } from './client'
import { ApiClientError } from './errors'
import { normalizeManagedUser } from './admin'
import type {
  AdminDepartment,
  AdminDepartmentMembers,
  CreateAdminDepartmentInput,
  UpdateAdminDepartmentInput,
} from '../types/admin'

function invalidDepartmentResponse(message: string): never {
  throw new ApiClientError({
    code: 'INVALID_DEPARTMENT_RESPONSE',
    message,
    status: 0,
  })
}

export function normalizeAdminDepartment(response: DepartmentResponse): AdminDepartment {
  if (!Number.isSafeInteger(response.id) || !response.name?.trim() || typeof response.active !== 'boolean') {
    return invalidDepartmentResponse('Sunucu geçerli departman bilgisi döndürmedi.')
  }

  return {
    id: response.id!,
    name: response.name.trim(),
    parentDepartmentId: typeof response.parentDepartmentId === 'number' ? response.parentDepartmentId : null,
    isActive: response.active === true,
  }
}

function normalizeDepartmentMembers(response: DepartmentMembersResponse): AdminDepartmentMembers {
  if (!Number.isSafeInteger(response.departmentId) || !response.departmentName?.trim()) {
    return invalidDepartmentResponse('Sunucu geçerli departman üyelik bilgisi döndürmedi.')
  }

  return {
    departmentId: response.departmentId!,
    departmentName: response.departmentName.trim(),
    members: (response.members ?? []).map(normalizeManagedUser),
  }
}

/** Varsayılan çağrı yalnız aktif departmanları döner. */
export async function listDepartments(includeInactive = false): Promise<AdminDepartment[]> {
  const response = await api.departments.listDepartments({ includeInactive })
  return (response ?? []).map(normalizeAdminDepartment)
}

export async function createDepartment(input: CreateAdminDepartmentInput): Promise<AdminDepartment> {
  const body: CreateDepartmentRequest = {
    name: input.name.trim(),
    parentDepartmentId: input.parentDepartmentId,
  }
  const response = await api.departments.createDepartment(body)
  return normalizeAdminDepartment(response)
}

/** Kısmi güncelleme: yalnız verilen alanlar gönderilir. */
export async function updateDepartment(id: number, input: UpdateAdminDepartmentInput): Promise<AdminDepartment> {
  const body: UpdateDepartmentRequest = {
    ...(input.name === undefined ? {} : { name: input.name.trim() }),
    ...(input.parentDepartmentId === undefined ? {} : { parentDepartmentId: input.parentDepartmentId }),
    ...(input.clearParent === undefined ? {} : { clearParent: input.clearParent }),
    ...(input.active === undefined ? {} : { active: input.active }),
  }
  const response = await api.departments.updateDepartment({ id }, body)
  return normalizeAdminDepartment(response)
}

export async function listDepartmentMembers(departmentId: number): Promise<AdminDepartmentMembers> {
  const response = await api.departments.listMembers({ id: departmentId })
  return normalizeDepartmentMembers(response)
}

export async function addDepartmentMember(departmentId: number, userId: string): Promise<AdminDepartmentMembers> {
  const response = await api.departments.addMember({ id: departmentId }, { userId })
  return normalizeDepartmentMembers(response)
}

export async function removeDepartmentMember(departmentId: number, userId: string): Promise<AdminDepartmentMembers> {
  const response = await api.departments.removeMember({ id: departmentId, userId })
  return normalizeDepartmentMembers(response)
}
