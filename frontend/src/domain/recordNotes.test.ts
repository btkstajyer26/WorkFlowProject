import { describe, expect, it } from 'vitest'
import { getDemoUserByRole } from '../mocks/users'
import type { WorkflowRecord } from '../types/record'
import { recordNoteMaxLength, upsertRecordNote } from './recordNotes'

const employee = getDemoUserByRole('CALISAN')
const deputy = getDemoUserByRole('BASKAN_YARDIMCISI')
const chair = getDemoUserByRole('BASKAN')

function makeRecord(
  status: WorkflowRecord['status'] = 'BSK_YRD_INCELEMESINDE',
  assignedToId: string | null = deputy.id,
): WorkflowRecord {
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
    assignedToId,
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
  it('atanmış incelemecinin tek notunu yeni geçmiş olayı üretmeden oluşturur ve günceller', () => {
    const first = upsertRecordNote(makeRecord(), deputy, '  İlk değerlendirmem.  ', '2026-08-01T09:00:00.000Z')
    const second = upsertRecordNote(first, deputy, 'Düzeltilmiş değerlendirmem.', '2026-08-01T10:00:00.000Z')

    expect(second.notes).toHaveLength(1)
    expect(second.notes[0]).toMatchObject({
      id: first.notes[0].id,
      authorId: deputy.id,
      body: 'Düzeltilmiş değerlendirmem.',
      createdAt: '2026-08-01T09:00:00.000Z',
      updatedAt: '2026-08-01T10:00:00.000Z',
      version: 1,
    })
    expect(second.history).toEqual([])
  })

  it('Başkanın yalnız kendisine atanmış Başkan incelemesindeki kaydın notunu yönetmesine izin verir', () => {
    const record = makeRecord('BASKAN_INCELEMESINDE', chair.id)

    expect(() => upsertRecordNote(record, chair, 'Başkan değerlendirmesi')).not.toThrow()
    expect(() => upsertRecordNote(record, deputy, 'Yetkisiz not')).toThrow('atanmış incelemeci')
  })

  it('Çalışanın, yanlış atanan kullanıcının ve sonuçlanmış kaydın not yönetmesini engeller', () => {
    expect(() => upsertRecordNote(makeRecord(), employee, 'Çalışan notu')).toThrow('atanmış incelemeci')
    expect(() => upsertRecordNote(makeRecord('BSK_YRD_INCELEMESINDE', chair.id), deputy, 'Yanlış atama')).toThrow('atanmış incelemeci')
    expect(() => upsertRecordNote(makeRecord('ONAYLANDI', null), chair, 'Yeni not')).toThrow('atanmış incelemeci')
  })

  it('boş notu ve karakter sınırını aşan notu reddeder', () => {
    expect(() => upsertRecordNote(makeRecord(), deputy, '   ')).toThrow('boş bırakılamaz')
    expect(() => upsertRecordNote(makeRecord(), deputy, 'a'.repeat(recordNoteMaxLength + 1))).toThrow('1000 karakter')
  })
})
