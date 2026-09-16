import type { WorkflowActorBindingView } from './generated/data-contracts'
import { api } from './client'
import { ApiClientError } from './errors'
import type { AdminActorBinding } from '../types/admin'

function invalidBindingResponse(message: string): never {
  throw new ApiClientError({
    code: 'INVALID_ACTOR_BINDING_RESPONSE',
    message,
    status: 0,
  })
}

function normalizeBinding(response: WorkflowActorBindingView): AdminActorBinding {
  if (
    !Number.isSafeInteger(response.bindingId) ||
    !response.fromStatus?.trim() ||
    !response.action?.trim() ||
    !response.toStatus?.trim() ||
    !Number.isSafeInteger(response.actorRoleId) ||
    typeof response.active !== 'boolean'
  ) {
    return invalidBindingResponse('Sunucu geçerli aktör-rol bağı döndürmedi.')
  }

  return {
    bindingId: response.bindingId!,
    fromStatusId: response.fromStatusId!,
    fromStatus: response.fromStatus.trim(),
    fromStatusDisplayName: response.fromStatusDisplayName?.trim() || response.fromStatus.trim(),
    actionId: response.actionId!,
    action: response.action.trim(),
    actionDisplayName: response.actionDisplayName?.trim() || response.action.trim(),
    toStatusId: response.toStatusId!,
    toStatus: response.toStatus.trim(),
    toStatusDisplayName: response.toStatusDisplayName?.trim() || response.toStatus.trim(),
    actorRoleId: response.actorRoleId!,
    actorRoleName: response.actorRoleName?.trim() || '—',
    actorRequirement: response.actorRequirement as AdminActorBinding['actorRequirement'],
    targetStrategy: response.targetStrategy ?? '',
    expectedTargetRoleId: response.expectedTargetRoleId ?? null,
    requiredPermissionId: response.requiredPermissionId ?? null,
    requiredPermissionCode: response.requiredPermissionCode?.trim() || null,
    isActive: response.active === true,
    isProtected: response.protectedBinding === true,
  }
}

/** Aktif ve pasif tüm bağları döndürür; boş liste geçerlidir. */
export async function listActorBindings(): Promise<AdminActorBinding[]> {
  const response = await api.workflowActorBindings.list()
  return (response ?? []).map(normalizeBinding)
}

/**
 * `templateTransitionId`, kopyalanacak sabit alanları taşıyan mevcut bir
 * bağın kimliğidir - genelde aynı grup içindeki aktif bir satır. Rol daha
 * önce bu geçişte pasif bir bağa sahipse yeniden etkinleştirilir.
 */
export async function bindActor(templateTransitionId: number, actorRoleId: number): Promise<AdminActorBinding> {
  const response = await api.workflowActorBindings.bind({ templateTransitionId, actorRoleId })
  return normalizeBinding(response)
}

/** Fiziksel silme değildir: bağ pasifleştirilir. */
export async function unbindActor(bindingId: number): Promise<AdminActorBinding> {
  const response = await api.workflowActorBindings.unbind({ bindingId })
  return normalizeBinding(response)
}
