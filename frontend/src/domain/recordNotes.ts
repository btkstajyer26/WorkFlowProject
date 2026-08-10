import type { AuthUser, WorkflowRole } from '../types/auth'
import type { RecordNote, WorkflowRecord } from '../types/record'

export const recordNoteMaxLength = 1000

function fullName(user: AuthUser) {
  return `${user.firstName} ${user.lastName}`
}

export function canManageRecordNote(record: WorkflowRecord, user: AuthUser) {
  if (user.role === 'BASKAN_YARDIMCISI') {
    return record.status === 'BSK_YRD_INCELEMESINDE' && record.assignedToId === user.id
  }

  if (user.role === 'BASKAN') {
    return record.status === 'BASKAN_INCELEMESINDE' && record.assignedToId === user.id
  }

  return false
}

export function upsertRecordNote(
  record: WorkflowRecord,
  user: AuthUser,
  body: string,
  now = new Date().toISOString(),
): WorkflowRecord {
  const trimmedBody = body.trim()

  if (!canManageRecordNote(record, user)) {
    throw new Error('Yalnızca mevcut aşamadaki atanmış incelemeci not ekleyebilir veya düzenleyebilir.')
  }
  if (!trimmedBody) throw new Error('Not boş bırakılamaz.')
  if (trimmedBody.length > recordNoteMaxLength) {
    throw new Error(`Not en fazla ${recordNoteMaxLength} karakter olabilir.`)
  }

  const existingNote = record.notes.find((note) => note.authorId === user.id)
  const updatedNote: RecordNote = existingNote
    ? {
        ...existingNote,
        author: fullName(user),
        authorRole: user.role as WorkflowRole,
        body: trimmedBody,
        updatedAt: now,
        version: existingNote.version + 1,
      }
    : {
        id: crypto.randomUUID(),
        recordId: record.id,
        authorId: user.id,
        author: fullName(user),
        authorRole: user.role as WorkflowRole,
        body: trimmedBody,
        createdAt: now,
        updatedAt: now,
        version: 0,
      }

  return {
    ...record,
    notes: existingNote
      ? record.notes.map((note) => note.authorId === user.id ? updatedNote : note)
      : [...record.notes, updatedNote],
  }
}
