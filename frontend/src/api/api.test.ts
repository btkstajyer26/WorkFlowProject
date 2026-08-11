import { describe, expect, it } from 'vitest'
import { api, clearApiAccessToken, setApiAccessToken } from './client'
import {
  getUnreadNotificationCount,
  listUnreadNotifications,
  markNotificationAsRead,
} from './notifications'
import { searchRecords } from './recordSearch'
import { listRecords } from './records'

const employeeCredentials = {
  email: 'john.doe@kurum.gov.tr',
  password: 'demo123',
}

async function loginAs(email: string) {
  const tokens = await api.auth.login({ email, password: 'demo123' })
  expect(tokens.accessToken).toBeTruthy()
  setApiAccessToken(tokens.accessToken!)
  return tokens
}

describe('OpenAPI istemcisi ve MSW sözleşmesi', () => {
  it('korumalı isteklerde Authorization header zorunluluğunu uygular', async () => {
    clearApiAccessToken()

    await expect(api.categories.getAllCategories()).rejects.toMatchObject({
      code: 'UNAUTHORIZED',
      status: 401,
    })

    await loginAs(employeeCredentials.email)
    await expect(api.categories.getAllCategories()).resolves.toEqual([
      { id: 1, name: 'İdari' },
      { id: 2, name: 'Mali' },
      { id: 3, name: 'İnsan Kaynakları' },
      { id: 4, name: 'Bilgi İşlem' },
      { id: 5, name: 'Teknik' },
    ])
  })

  it('Spring Pageable adaptörüyle kayıtları filtreleyip sayfalar', async () => {
    await loginAs(employeeCredentials.email)

    const result = await listRecords({
      categoryId: 4,
      keyword: 'sunucu',
      page: 0,
      size: 5,
    })

    expect(result.content).toHaveLength(1)
    expect(result.content?.[0]).toMatchObject({
      title: 'Sunucu alım talebi',
      categoryId: 4,
      status: 'TASLAK',
    })
    expect(result.totalElements).toBe(1)
  })

  it('RBAC kapsamındaki kayıtları arar ve kategori kimliğini merkezi kategori adıyla eşleştirir', async () => {
    await loginAs(employeeCredentials.email)

    const result = await searchRecords({
      text: 'sunucu',
      categoryId: 4,
      page: 0,
      size: 5,
    })

    expect(result).toMatchObject({
      page: 0,
      size: 5,
      totalElements: 1,
      totalPages: 1,
    })
    expect(result.content).toEqual([
      expect.objectContaining({
        title: 'Sunucu alım talebi',
        category: { id: 4, name: 'Bilgi İşlem' },
        status: 'TASLAK',
      }),
    ])
    expect(result.content[0]).not.toHaveProperty('recordNumber')
    expect(result.content[0]).not.toHaveProperty('lastAction')
  })

  it('kayıt oluşturma, güncelleme ve silme cevaplarını OpenAPI modeliyle taşır', async () => {
    await loginAs(employeeCredentials.email)

    const created = await api.records.createRecord({
      title: 'Yeni donanım talebi',
      description: 'Çalışma istasyonu talebidir.',
      categoryId: 4,
    })
    expect(created).toMatchObject({
      title: 'Yeni donanım talebi',
      status: 'TASLAK',
    })

    const updated = await api.records.updateRecord({ id: created.id! }, {
      title: 'Güncellenen donanım talebi',
      description: 'İki çalışma istasyonu talebidir.',
      categoryId: 4,
    })
    expect(updated.title).toBe('Güncellenen donanım talebi')

    await api.records.deleteRecord({ id: created.id! })
    await expect(api.records.getRecordById({ id: created.id! })).rejects.toMatchObject({
      code: 'RESOURCE_NOT_FOUND',
      status: 404,
    })
  })

  it('workflow cevabını ve ayrı audit geçmişini aynı sahte sunucu durumundan üretir', async () => {
    const recordId = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1'
    const deputyId = '22222222-2222-2222-2222-222222222222'

    await loginAs(employeeCredentials.email)
    const submitted = await api.workflow.performAction({ recordId }, {
      action: 'GONDER',
      targetUserId: deputyId,
    })
    expect(submitted).toMatchObject({
      previousStatus: 'TASLAK',
      newStatus: 'BSK_YRD_INCELEMESINDE',
      assignedTo: deputyId,
    })

    await loginAs('ayse.kaya@kurum.gov.tr')
    const forwarded = await api.workflow.performAction({ recordId }, {
      action: 'BASKANA_ILET',
      comment: 'Başkan değerlendirmesine uygundur.',
    })
    expect(forwarded.newStatus).toBe('BASKAN_INCELEMESINDE')

    await loginAs('mehmet.demir@kurum.gov.tr')
    const approved = await api.workflow.performAction({ recordId }, {
      action: 'ONAYLA',
      comment: 'Uygundur.',
    })
    expect(approved.newStatus).toBe('ONAYLANDI')

    await loginAs(employeeCredentials.email)
    const history = await api.auditLogs.getGecmis({ recordId })
    expect(history.map((item) => item.action)).toEqual([
      'ONAYLA',
      'BASKANA_ILET',
      'GONDER',
    ])
  })

  it('okunmamış bildirim listesini ve sayısını aynı MSW durumundan yönetir', async () => {
    await loginAs(employeeCredentials.email)

    await expect(listUnreadNotifications()).resolves.toEqual([
      expect.objectContaining({
        id: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb1',
        notificationType: 'RECORD_APPROVED',
        read: false,
      }),
    ])
    await expect(getUnreadNotificationCount()).resolves.toBe(1)

    await markNotificationAsRead('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb1')

    await expect(listUnreadNotifications()).resolves.toEqual([])
    await expect(getUnreadNotificationCount()).resolves.toBe(0)
  })

  it('başka kullanıcıya ait bildirimin okundu yapılmasını reddeder', async () => {
    await loginAs(employeeCredentials.email)

    await expect(
      markNotificationAsRead('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb2'),
    ).rejects.toMatchObject({ status: 403 })
  })

  it('admin kullanıcı oluşturma endpointini yalnızca Admin tokenıyla çalıştırır', async () => {
    await loginAs(employeeCredentials.email)
    await expect(api.admin.createUser({
      firstName: 'Yeni',
      lastName: 'Kullanıcı',
      email: 'yeni.kullanici@kurum.gov.tr',
      password: 'demo123',
      roleName: 'CALISAN',
    })).rejects.toMatchObject({ status: 403 })

    await loginAs('admin@kurum.gov.tr')
    const created = await api.admin.createUser({
      firstName: 'Yeni',
      lastName: 'Kullanıcı',
      email: 'yeni.kullanici@kurum.gov.tr',
      password: 'demo123',
      roleName: 'CALISAN',
    })
    expect(created).toMatchObject({
      email: 'yeni.kullanici@kurum.gov.tr',
      roleName: 'CALISAN',
    })
  })
})
