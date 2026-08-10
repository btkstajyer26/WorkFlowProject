import { useState, type ReactNode } from 'react'
import { upsertRecordNote } from '../domain/recordNotes'
import { canUserViewRecord, transitionRecord, type WorkflowActionInput } from '../domain/workflow'
import { mockNotifications } from '../mocks/notifications'
import { mockRecords } from '../mocks/records'
import { getDemoUserByRole } from '../mocks/users'
import { roleLabels, type AuthUser } from '../types/auth'
import type { NotificationItem } from '../types/notification'
import type { WorkflowRecord } from '../types/record'
import { WorkflowContext, type RecordDraftInput, type WorkflowContextValue } from './workflowState'

function nameOf(user: AuthUser) {
  return `${user.firstName} ${user.lastName}`
}

function hydrateRecord(record: WorkflowRecord): WorkflowRecord {
  const employee = getDemoUserByRole('CALISAN')
  const deputy = getDemoUserByRole('BASKAN_YARDIMCISI')
  const chair = getDemoUserByRole('BASKAN')
  const assignedToId = record.status === 'BSK_YRD_INCELEMESINDE'
    ? deputy.id
    : record.status === 'BASKAN_INCELEMESINDE'
      ? chair.id
      : record.status === 'DUZENLEME_BEKLIYOR'
        ? employee.id
        : null

  return {
    ...record,
    createdById: record.createdById ?? employee.id,
    assignedToId: record.assignedToId ?? assignedToId,
    lastDeputyId: record.lastDeputyId ?? deputy.id,
    notes: record.notes ?? [],
  }
}

function nextRecordNumber(records: WorkflowRecord[]) {
  const sequence = records.reduce((highest, record) => {
    const parsed = Number(record.recordNumber.split('-').at(-1))
    return Number.isFinite(parsed) ? Math.max(highest, parsed) : highest
  }, 0) + 1

  return `EBYS-${new Date().getFullYear()}-${String(sequence).padStart(6, '0')}`
}

export function WorkflowProvider({ user, children }: { user: AuthUser; children: ReactNode }) {
  const [records, setRecords] = useState<WorkflowRecord[]>(() => mockRecords.map(hydrateRecord))
  const [allNotifications, setAllNotifications] = useState<NotificationItem[]>(mockNotifications)

  const buildDraft = (input: RecordDraftInput): WorkflowRecord => {
    if (user.role !== 'CALISAN') throw new Error('Yalnızca çalışanlar kayıt oluşturabilir.')
    const now = new Date().toISOString()
    return {
      id: crypto.randomUUID(),
      recordNumber: nextRecordNumber(records),
      title: input.title.trim(),
      category: input.category,
      description: input.description.trim(),
      status: 'TASLAK',
      createdBy: nameOf(user),
      createdById: user.id,
      assignedTo: null,
      assignedToId: null,
      lastDeputyId: null,
      lastAction: 'Taslak kaydedildi',
      createdAt: now,
      updatedAt: now,
      attachments: input.attachments,
      notes: [],
      history: [{
        id: crypto.randomUUID(),
        action: 'Taslak kaydedildi',
        actor: nameOf(user),
        actorId: user.id,
        role: roleLabels[user.role],
        date: now,
      }],
    }
  }

  const createDraft = (input: RecordDraftInput) => {
    const record = buildDraft(input)
    setRecords((current) => [record, ...current])
    return record
  }

  const createAndSubmit = (input: RecordDraftInput) => {
    const draft = buildDraft(input)
    const deputy = getDemoUserByRole('BASKAN_YARDIMCISI')
    const transition = transitionRecord(draft, { action: 'GONDER', actor: user, targetUser: deputy })
    setRecords((current) => [transition.record, ...current])
    if (transition.notification) setAllNotifications((current) => [transition.notification!, ...current])
    return transition.record
  }

  const updateEditableRecord = (recordId: string, input: RecordDraftInput) => {
    const existing = records.find((record) => record.id === recordId)
    if (!existing) throw new Error('Kayıt bulunamadı.')
    if (user.role !== 'CALISAN' || existing.createdById !== user.id || !['TASLAK', 'DUZENLEME_BEKLIYOR'].includes(existing.status)) {
      throw new Error('Bu kayıt düzenlenemez.')
    }

    const now = new Date().toISOString()
    const updated: WorkflowRecord = {
      ...existing,
      title: input.title.trim(),
      category: input.category,
      description: input.description.trim(),
      attachments: input.attachments,
      updatedAt: now,
      lastAction: existing.status === 'TASLAK' ? 'Taslak güncellendi' : 'Düzeltmeler kaydedildi',
    }
    setRecords((current) => current.map((record) => record.id === recordId ? updated : record))
    return updated
  }

  const updateAndSubmit = (recordId: string, input: RecordDraftInput) => {
    const existing = records.find((record) => record.id === recordId)
    if (!existing) throw new Error('Kayıt bulunamadı.')
    if (user.role !== 'CALISAN' || existing.createdById !== user.id || !['TASLAK', 'DUZENLEME_BEKLIYOR'].includes(existing.status)) {
      throw new Error('Bu kayıt gönderilemez.')
    }

    const prepared: WorkflowRecord = {
      ...existing,
      title: input.title.trim(),
      category: input.category,
      description: input.description.trim(),
      attachments: input.attachments,
    }
    const deputy = getDemoUserByRole('BASKAN_YARDIMCISI')
    const transition = transitionRecord(prepared, {
      action: existing.status === 'TASLAK' ? 'GONDER' : 'TEKRAR_GONDER',
      actor: user,
      targetUser: deputy,
    })
    setRecords((current) => current.map((record) => record.id === recordId ? transition.record : record))
    if (transition.notification) setAllNotifications((current) => [transition.notification!, ...current])
    return transition.record
  }

  const deleteDraft = (recordId: string) => {
    const existing = records.find((record) => record.id === recordId)
    if (!existing) throw new Error('Kayıt bulunamadı.')
    if (user.role !== 'CALISAN' || existing.createdById !== user.id || existing.status !== 'TASLAK') {
      throw new Error('Yalnızca kendi taslağınızı silebilirsiniz.')
    }
    setRecords((current) => current.filter((record) => record.id !== recordId))
    setAllNotifications((current) => current.filter((notification) => notification.recordId !== recordId))
  }

  const applyAction = (recordId: string, input: Omit<WorkflowActionInput, 'actor'>) => {
    const existing = records.find((record) => record.id === recordId)
    if (!existing) throw new Error('Kayıt bulunamadı.')
    const transition = transitionRecord(existing, { ...input, actor: user })
    setRecords((current) => current.map((record) => record.id === recordId ? transition.record : record))
    if (transition.notification) setAllNotifications((current) => [transition.notification!, ...current])
    return transition.record
  }

  const saveNote = (recordId: string, body: string) => {
    const existing = records.find((record) => record.id === recordId)
    if (!existing) throw new Error('Kayıt bulunamadı.')
    if (!canUserViewRecord(existing, user)) throw new Error('Bu kayda not ekleme yetkiniz yok.')

    const updated = upsertRecordNote(existing, user, body)
    setRecords((current) => current.map((record) => record.id === recordId ? updated : record))
    return updated
  }

  const notifications = allNotifications.filter((notification) => notification.userId === user.id)
  const value: WorkflowContextValue = {
    user,
    records,
    visibleRecords: records.filter((record) => canUserViewRecord(record, user)),
    notifications,
    unreadNotificationCount: notifications.reduce((count, notification) => count + Number(!notification.isRead), 0),
    createDraft,
    createAndSubmit,
    updateEditableRecord,
    updateAndSubmit,
    deleteDraft,
    applyAction,
    saveNote,
    markNotificationRead: (notificationId) => {
      setAllNotifications((current) => current.map((notification) =>
        notification.id === notificationId && notification.userId === user.id
          ? { ...notification, isRead: true }
          : notification,
      ))
    },
    markAllNotificationsRead: () => {
      setAllNotifications((current) => current.map((notification) =>
        notification.userId === user.id && !notification.isRead
          ? { ...notification, isRead: true }
          : notification,
      ))
    },
  }

  return <WorkflowContext.Provider value={value}>{children}</WorkflowContext.Provider>
}
