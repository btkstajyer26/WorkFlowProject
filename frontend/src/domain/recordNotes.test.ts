import { describe, expect, it } from 'vitest'
import { getDemoUserByRole } from '../mocks/users'
import type { WorkflowRecord } from '../types/record'
import { recordNoteMaxLength, upsertRecordNote } from './recordNotes'

const employee = getDemoUserByRole('CALISAN')
const deputy = getDemoUserByRole('BASKAN_YARDIMCISI')

function makeRecord(status: WorkflowRecord['status'] = 'BSK_YRD_INCELEMESINDE'): WorkflowRecord {
  return {
    id: 'record-note-test',
    recordNumber: 'EBYS-2026-000099',
    title: 'Not testi',
    description: 'Kayıt notu davranışları',
    category: 'İdari',
    status,
    createdBy: 'John Doe',
    createdById: employee.id,
    assignedTo: 'Ayşe Kaya',
    assignedToId: deputy.id,
    lastDeputyId: deputy.id,
    lastAction: 'İncelemeye gönderildi',
    createdAt: '2026-08-01T08:00:00.000Z',
    updatedAt: '2026-08-01T08:30:00.000Z',
    attachments: [],
    notes: [],
    history: [],
  }
}

describe('record notes', () => {
  it('kullanıcı için ilk notu oluşturur', () => {
    const updated = upsertRecordNote(makeRecord(), employee, '  İlk değerlendirmem.  ', '2026-08-01T09:00:00.000Z')

    expect(updated.notes).toHaveLength(1)
    expect(updated.notes[0]).toMatchObject({
      recordId: 'record-note-test',
      authorId: employee.id,
      authorRole: 'CALISAN',
      body: 'İlk değerlendirmem.',
      version: 0,
    })
    expect(updated.lastAction).toBe('İncelemeye gönderildi')
    expect(updated.updatedAt).toBe('2026-08-01T08:30:00.000Z')
    expect(updated.history).toHaveLength(0)
  })

  it('aynı kullanıcının notunu yeni satır oluşturmadan günceller', () => {
    const created = upsertRecordNote(makeRecord(), employee, 'İlk metin', '2026-08-01T09:00:00.000Z')
    const updated = upsertRecordNote(created, employee, 'Güncel metin', '2026-08-01T10:00:00.000Z')

    expect(updated.notes).toHaveLength(1)
    expect(updated.notes[0]).toMatchObject({
      id: created.notes[0].id,
      body: 'Güncel metin',
      createdAt: '2026-08-01T09:00:00.000Z',
      updatedAt: '2026-08-01T10:00:00.000Z',
      version: 1,
    })
  })

  it('farklı kullanıcıların tekil notlarını birlikte korur', () => {
    const employeeNote = upsertRecordNote(makeRecord(), employee, 'Çalışan notu')
    const deputyNote = upsertRecordNote(employeeNote, deputy, 'Başkan Yardımcısı notu')

    expect(deputyNote.notes).toHaveLength(2)
    expect(deputyNote.notes.map((note) => note.authorId)).toEqual([employee.id, deputy.id])
  })

  it('sonuçlanmış kaydı ve karakter sınırını korur', () => {
    expect(() => upsertRecordNote(makeRecord('ONAYLANDI'), employee, 'Yeni not')).toThrow('değiştirilemez')
    expect(() => upsertRecordNote(makeRecord(), employee, 'a'.repeat(recordNoteMaxLength + 1))).toThrow('1000 karakter')
  })
})
