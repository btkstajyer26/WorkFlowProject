import { describe, expect, it } from 'vitest'
import { transitionRecord } from './workflow'
import { getDemoUserByRole } from '../mocks/users'
import type { WorkflowRecord } from '../types/record'

const employee = getDemoUserByRole('CALISAN')
const deputy = getDemoUserByRole('BASKAN_YARDIMCISI')
const chair = getDemoUserByRole('BASKAN')

function makeRecord(status: WorkflowRecord['status']): WorkflowRecord {
  return {
    id: 'record-test-001',
    recordNumber: 'EBYS-2026-000001',
    title: 'Test Kaydı',
    description: 'İş akışı testi',
    categoryId: 1,
    category: 'İdari',
    status,
    createdBy: 'John Doe',
    createdById: employee.id,
    assignedTo: status === 'TASLAK' ? null : 'Atanan kullanıcı',
    assignedToId: status === 'TASLAK' ? null : deputy.id,
    lastDeputyId: deputy.id,
    lastAction: 'Kayıt oluşturuldu',
    createdAt: '2026-08-01T08:00:00.000Z',
    updatedAt: '2026-08-01T08:00:00.000Z',
    attachments: [],
    history: [],
  }
}

describe('workflow transitions', () => {
  it('çalışanın taslağını Başkan Yardımcısına gönderir', () => {
    const result = transitionRecord(makeRecord('TASLAK'), {
      action: 'GONDER',
      actor: employee,
      targetUser: deputy,
    })

    expect(result.record.status).toBe('BSK_YRD_INCELEMESINDE')
    expect(result.record.assignedToId).toBe(deputy.id)
    expect(result.notification?.userId).toBe(deputy.id)
    expect(result.record.history.at(-1)?.action).toBe('Başkan Yardımcısına gönderildi')
  })

  it('Başkan Yardımcısının kaydı Başkana iletmesine izin verir', () => {
    const record = makeRecord('BSK_YRD_INCELEMESINDE')

    const result = transitionRecord(record, {
      action: 'BASKANA_ILET',
      actor: deputy,
      targetUser: chair,
      comment: 'Uygundur.',
    })

    expect(result.record.status).toBe('BASKAN_INCELEMESINDE')
    expect(result.record.assignedToId).toBe(chair.id)
    expect(result.notification?.userId).toBe(chair.id)
    expect(result.record.history.at(-1)?.note).toBe('Uygundur.')
  })

  it('Başkan onayladığında kaydı kilitler ve çalışanı bilgilendirir', () => {
    const result = transitionRecord(makeRecord('BASKAN_INCELEMESINDE'), {
      action: 'ONAYLA',
      actor: chair,
      comment: 'Onaylandı.',
    })

    expect(result.record.status).toBe('ONAYLANDI')
    expect(result.record.assignedToId).toBeNull()
    expect(result.notification?.userId).toBe(employee.id)
  })

  it.each([
    ['CALISANA_GERI_GONDER', deputy, 'BSK_YRD_INCELEMESINDE'],
    ['BASKAN_YARDIMCISINA_GERI_GONDER', chair, 'BASKAN_INCELEMESINDE'],
    ['REDDET', chair, 'BASKAN_INCELEMESINDE'],
  ] as const)('%s aksiyonunda açıklamayı zorunlu tutar', (action, actor, status) => {
    expect(() => transitionRecord(makeRecord(status), {
      action,
      actor,
      ...(action === 'BASKAN_YARDIMCISINA_GERI_GONDER' ? { targetUser: deputy } : {}),
    })).toThrow('açıklama zorunludur')
  })

  it('Başkanın kaydı Başkan Yardımcısına geri göndermesini destekler', () => {
    const result = transitionRecord(makeRecord('BASKAN_INCELEMESINDE'), {
      action: 'BASKAN_YARDIMCISINA_GERI_GONDER',
      actor: chair,
      targetUser: deputy,
      comment: 'Tekrar kontrol edilmeli.',
    })

    expect(result.record.status).toBe('BSK_YRD_INCELEMESINDE')
    expect(result.record.assignedToId).toBe(deputy.id)
  })

  it('yanlış rolün aksiyonunu reddeder', () => {
    expect(() => transitionRecord(makeRecord('BSK_YRD_INCELEMESINDE'), {
      action: 'BASKANA_ILET',
      actor: employee,
      targetUser: chair,
    })).toThrow('Başkana iletilemez')
  })

  it.each(['ONAYLANDI', 'REDDEDILDI'] as const)('%s durumundaki kaydı değiştirmez', (status) => {
    expect(() => transitionRecord(makeRecord(status), {
      action: 'ONAYLA',
      actor: chair,
    })).toThrow('Sonuçlanmış kayıtlar')
  })
})
