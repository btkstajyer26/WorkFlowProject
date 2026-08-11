import { roleLabels, type AuthUser } from '../types/auth'
import type { NotificationItem } from '../types/notification'
import type { WorkflowRecord } from '../types/record'

export type WorkflowAction =
  | 'GONDER'
  | 'TEKRAR_GONDER'
  | 'BASKANA_ILET'
  | 'CALISANA_GERI_GONDER'
  | 'BASKAN_YARDIMCISINA_GERI_GONDER'
  | 'ONAYLA'
  | 'REDDET'

export type WorkflowActionInput = {
  action: WorkflowAction
  actor: AuthUser
  comment?: string
  targetUser?: AuthUser
}

export type WorkflowTransition = {
  record: WorkflowRecord
  notification?: NotificationItem
}

const actionLabels: Record<WorkflowAction, string> = {
  GONDER: 'Başkan Yardımcısına gönderildi',
  TEKRAR_GONDER: 'Düzeltme tamamlanarak yeniden gönderildi',
  BASKANA_ILET: 'Başkana iletildi',
  CALISANA_GERI_GONDER: 'Çalışana geri gönderildi',
  BASKAN_YARDIMCISINA_GERI_GONDER: 'Başkan Yardımcısına geri gönderildi',
  ONAYLA: 'Başkan tarafından onaylandı',
  REDDET: 'Başkan tarafından reddedildi',
}

const terminalStatuses = new Set<WorkflowRecord['status']>(['ONAYLANDI', 'REDDEDILDI'])

function fullName(user: AuthUser) {
  return `${user.firstName} ${user.lastName}`
}

function requireComment(comment: string | undefined) {
  if (!comment?.trim()) throw new Error('Bu işlem için açıklama zorunludur.')
}

function requireTarget(targetUser: AuthUser | undefined, role: AuthUser['role']) {
  if (!targetUser || targetUser.role !== role) throw new Error('İşlem için geçerli bir hedef kullanıcı seçilmelidir.')
  return targetUser
}

function assertTransition(record: WorkflowRecord, input: WorkflowActionInput) {
  if (terminalStatuses.has(record.status)) throw new Error('Sonuçlanmış kayıtlar üzerinde işlem yapılamaz.')

  switch (input.action) {
    case 'GONDER':
      if (input.actor.role !== 'CALISAN' || record.status !== 'TASLAK') throw new Error('Bu kayıt incelemeye gönderilemez.')
      requireTarget(input.targetUser, 'BASKAN_YARDIMCISI')
      return
    case 'TEKRAR_GONDER':
      if (input.actor.role !== 'CALISAN' || record.status !== 'DUZENLEME_BEKLIYOR') throw new Error('Bu kayıt yeniden gönderilemez.')
      requireTarget(input.targetUser, 'BASKAN_YARDIMCISI')
      return
    case 'BASKANA_ILET':
      if (input.actor.role !== 'BASKAN_YARDIMCISI' || record.status !== 'BSK_YRD_INCELEMESINDE') throw new Error('Bu kayıt Başkana iletilemez.')
      requireTarget(input.targetUser, 'BASKAN')
      return
    case 'CALISANA_GERI_GONDER':
      if (!(
        (input.actor.role === 'BASKAN_YARDIMCISI' && record.status === 'BSK_YRD_INCELEMESINDE') ||
        (input.actor.role === 'BASKAN' && record.status === 'BASKAN_INCELEMESINDE')
      )) {
        throw new Error('Bu kayıt çalışana geri gönderilemez.')
      }
      requireComment(input.comment)
      return
    case 'BASKAN_YARDIMCISINA_GERI_GONDER':
      if (input.actor.role !== 'BASKAN' || record.status !== 'BASKAN_INCELEMESINDE') throw new Error('Bu kayıt Başkan Yardımcısına geri gönderilemez.')
      requireComment(input.comment)
      requireTarget(input.targetUser, 'BASKAN_YARDIMCISI')
      return
    case 'ONAYLA':
      if (input.actor.role !== 'BASKAN' || record.status !== 'BASKAN_INCELEMESINDE') throw new Error('Bu kayıt onaylanamaz.')
      return
    case 'REDDET':
      if (input.actor.role !== 'BASKAN' || record.status !== 'BASKAN_INCELEMESINDE') throw new Error('Bu kayıt reddedilemez.')
      requireComment(input.comment)
  }
}

function notificationMessage(record: WorkflowRecord, action: WorkflowAction) {
  const messages: Record<WorkflowAction, string> = {
    GONDER: `${record.title} kaydı incelemeniz için gönderildi.`,
    TEKRAR_GONDER: `${record.title} kaydındaki düzeltmeler tamamlanarak yeniden gönderildi.`,
    BASKANA_ILET: `${record.title} kaydı nihai incelemeniz için iletildi.`,
    CALISANA_GERI_GONDER: `${record.title} kaydınız düzeltme için geri gönderildi.`,
    BASKAN_YARDIMCISINA_GERI_GONDER: `${record.title} kaydı yeniden incelemeniz için geri gönderildi.`,
    ONAYLA: `${record.title} kaydınız Başkan tarafından onaylandı.`,
    REDDET: `${record.title} kaydınız Başkan tarafından reddedildi.`,
  }
  return messages[action]
}

export function transitionRecord(record: WorkflowRecord, input: WorkflowActionInput): WorkflowTransition {
  assertTransition(record, input)

  const now = new Date().toISOString()
  const employeeTarget = {
    id: record.createdById ?? 'user-demo-001',
    name: record.createdBy,
  }
  const next = { ...record }
  let notificationTarget = input.targetUser

  switch (input.action) {
    case 'GONDER':
    case 'TEKRAR_GONDER':
      next.status = 'BSK_YRD_INCELEMESINDE'
      next.assignedTo = fullName(input.targetUser!)
      next.assignedToId = input.targetUser!.id
      next.lastDeputyId = input.targetUser!.id
      break
    case 'BASKANA_ILET':
      next.status = 'BASKAN_INCELEMESINDE'
      next.assignedTo = fullName(input.targetUser!)
      next.assignedToId = input.targetUser!.id
      break
    case 'CALISANA_GERI_GONDER':
      next.status = 'DUZENLEME_BEKLIYOR'
      next.assignedTo = employeeTarget.name
      next.assignedToId = employeeTarget.id
      notificationTarget = { ...input.actor, id: employeeTarget.id }
      break
    case 'BASKAN_YARDIMCISINA_GERI_GONDER':
      next.status = 'BSK_YRD_INCELEMESINDE'
      next.assignedTo = fullName(input.targetUser!)
      next.assignedToId = input.targetUser!.id
      break
    case 'ONAYLA':
    case 'REDDET':
      next.status = input.action === 'ONAYLA' ? 'ONAYLANDI' : 'REDDEDILDI'
      next.assignedTo = null
      next.assignedToId = null
      notificationTarget = { ...input.actor, id: employeeTarget.id }
      break
  }

  next.updatedAt = now
  next.lastAction = actionLabels[input.action]
  next.history = [
    ...record.history,
    {
      id: crypto.randomUUID(),
      action: actionLabels[input.action],
      actor: fullName(input.actor),
      actorId: input.actor.id,
      role: roleLabels[input.actor.role],
      ...(input.comment?.trim() ? { note: input.comment.trim() } : {}),
      date: now,
    },
  ]

  return {
    record: next,
    ...(notificationTarget
      ? {
          notification: {
            id: crypto.randomUUID(),
            userId: notificationTarget.id,
            recordId: record.id,
            message: notificationMessage(record, input.action),
            isRead: false,
            createdAt: now,
          },
        }
      : {}),
  }
}

export function canUserViewRecord(record: WorkflowRecord, user: AuthUser) {
  if (user.role === 'CALISAN') return (record.createdById ?? 'user-demo-001') === user.id
  if (user.role === 'BASKAN_YARDIMCISI') return record.status !== 'TASLAK'
  return ['BASKAN_INCELEMESINDE', 'ONAYLANDI', 'REDDEDILDI'].includes(record.status)
}
