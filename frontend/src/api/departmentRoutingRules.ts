import type { DepartmentRoutingRuleResponse } from './generated/data-contracts'
import { api } from './client'
import { ApiClientError } from './errors'
import type { AdminDepartmentRoutingRule } from '../types/admin'

function invalidRoutingRuleResponse(message: string): never {
  throw new ApiClientError({
    code: 'INVALID_ROUTING_RULE_RESPONSE',
    message,
    status: 0,
  })
}

function normalizeRoutingRule(response: DepartmentRoutingRuleResponse): AdminDepartmentRoutingRule {
  if (
    !Number.isSafeInteger(response.id) ||
    !Number.isSafeInteger(response.departmentId) ||
    !response.fromStatus?.trim() ||
    !response.action?.trim() ||
    !Number.isSafeInteger(response.targetRoleId) ||
    typeof response.active !== 'boolean'
  ) {
    return invalidRoutingRuleResponse('Sunucu geçerli routing kuralı döndürmedi.')
  }

  return {
    id: response.id!,
    departmentId: response.departmentId!,
    fromStatusId: response.fromStatusId!,
    fromStatus: response.fromStatus.trim(),
    fromStatusDisplayName: response.fromStatusDisplayName?.trim() || response.fromStatus.trim(),
    actionId: response.actionId!,
    action: response.action.trim(),
    actionDisplayName: response.actionDisplayName?.trim() || response.action.trim(),
    targetRoleId: response.targetRoleId!,
    targetRoleName: response.targetRoleName?.trim() || '—',
    isActive: response.active === true,
  }
}

export async function listRoutingRules(departmentId: number): Promise<AdminDepartmentRoutingRule[]> {
  const response = await api.departmentRoutingRules.listRules({ departmentId })
  return (response ?? []).map(normalizeRoutingRule)
}

export async function createRoutingRule(
  departmentId: number,
  input: { fromStatusId: number, actionId: number, targetRoleId: number },
): Promise<AdminDepartmentRoutingRule> {
  const response = await api.departmentRoutingRules.createRule({ departmentId }, input)
  return normalizeRoutingRule(response)
}

export async function updateRoutingRule(
  departmentId: number,
  ruleId: number,
  input: { targetRoleId?: number, active?: boolean },
): Promise<AdminDepartmentRoutingRule> {
  const response = await api.departmentRoutingRules.updateRule({ departmentId, ruleId }, input)
  return normalizeRoutingRule(response)
}
