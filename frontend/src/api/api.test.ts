import { describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { api, clearApiAccessToken, setApiAccessToken } from './client'
import {
  getUnreadNotificationCount,
  listNotifications,
  listUnreadNotifications,
  markNotificationAsRead,
} from './notifications'
import { searchRecords } from './recordSearch'
import { listRecords } from './records'
import { listCategories } from './categories'
import { getRecordDetail, listRecordAuditLogs } from './recordDetails'
import { listAdminAuditLogs, listAdminUsers } from './admin'
import { deleteRecordFile, listRecordFiles, uploadRecordFile } from './files'
import { apiMockServer } from '../mocks/api/server'
import { apiBaseUrl } from './config'
import { performWorkflowAction } from './workflow'

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
  it('departmana gönderim isteğini kişi hedefi olmadan sözleşmeye uygun taşır', async () => {
    await loginAs(employeeCredentials.email)
    const recordId = '11111111-1111-4111-8111-111111111111'
    apiMockServer.use(http.post(`${apiBaseUrl}/api/records/:recordId/workflow/actions`, async ({ request }) => {
      expect(await request.json()).toEqual({ action: 'DEPARTMANA_GONDER', targetDepartmentId: 12 })
      return HttpResponse.json({ recordId, action: 'DEPARTMANA_GONDER', previousStatus: 'TASLAK',
        newStatus: 'BSK_YRD_INCELEMESINDE', assignedTo: null,
        performedBy: 'employee-id', performedAt: '2026-09-04T12:00:00Z' })
    }))
    await expect(performWorkflowAction(recordId, { action: 'DEPARTMANA_GONDER', targetDepartmentId: 12 }))
      .resolves.toMatchObject({ action: 'DEPARTMANA_GONDER', assignedTo: null, newStatus: 'BSK_YRD_INCELEMESINDE' })
  })

  it('kayıt eklerini listeler, multipart olarak yükler ve siler', async () => {
    await loginAs(employeeCredentials.email)
    const recordId = '11111111-1111-4111-8111-111111111111'
    const fileId = '22222222-2222-4222-8222-222222222222'
    let deletedFileId: string | undefined

    apiMockServer.use(
      http.get(`${apiBaseUrl}/api/records/:recordId/files`, ({ params }) => HttpResponse.json([{
        id: fileId,
        recordId: params.recordId,
        originalName: 'belge.pdf',
        mimeType: 'application/pdf',
        fileSize: 2048,
        uploadedBy: '33333333-3333-4333-8333-333333333333',
        uploadedAt: '2026-08-17T09:00:00Z',
      }])),
      http.post(`${apiBaseUrl}/api/records/:recordId/files`, ({ params, request }) => {
        expect(request.headers.get('content-type')).toContain('multipart/form-data')
        return HttpResponse.json([{
          id: fileId,
          recordId: params.recordId,
          originalName: 'yeni.pdf',
          mimeType: 'application/pdf',
          fileSize: 3,
          uploadedBy: '33333333-3333-4333-8333-333333333333',
          uploadedAt: '2026-08-17T09:00:00Z',
        }])
      }),
      http.delete(`${apiBaseUrl}/api/files/:fileId`, ({ params }) => {
        deletedFileId = String(params.fileId)
        return new HttpResponse(null, { status: 204 })
      }),
    )

    await expect(listRecordFiles(recordId)).resolves.toEqual([
      expect.objectContaining({ id: fileId, originalName: 'belge.pdf', fileSize: 2048 }),
    ])
    await expect(uploadRecordFile(recordId, new File(['pdf'], 'yeni.pdf', { type: 'application/pdf' }))).resolves.toEqual(
      expect.objectContaining({ recordId, originalName: 'yeni.pdf' }),
    )
    await deleteRecordFile(fileId)
    expect(deletedFileId).toBe(fileId)
  })

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
      q: 'sunucu',
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
    const categories = await listCategories()

    const result = await searchRecords({
      q: 'sunucu',
      creator: 'John Doe',
      categoryId: 4,
      page: 0,
      size: 5,
    }, categories)

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
    const deputyId = 'user-demo-002'

    await loginAs(employeeCredentials.email)
    const submitted = await api.workflow.performAction({ recordId }, {
      action: 'GONDER',
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
    const history = await listRecordAuditLogs(recordId)
    expect(history.map((item) => item.action)).toEqual([
      'GONDER',
      'BASKANA_ILET',
      'ONAYLA',
    ])

    const detail = await getRecordDetail(recordId, await listCategories())
    expect(detail).toMatchObject({
      id: recordId,
      status: 'ONAYLANDI',
      category: 'Bilgi İşlem',
      lastAction: 'Kayıt onaylandı',
    })
    expect(detail.history.map((item) => item.action)).toEqual([
      'Başkan Yardımcısına gönderildi',
      'Başkana iletildi',
      'Kayıt onaylandı',
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

  it('tüm bildirimleri Spring Pageable adapterıyla okundu durumlarıyla listeler', async () => {
    await loginAs(employeeCredentials.email)

    const result = await listNotifications({ page: 0, size: 10 })

    expect(result).toMatchObject({
      page: 0,
      size: 10,
      totalElements: 2,
      totalPages: 1,
    })
    expect(result.content).toEqual([
      expect.objectContaining({
        id: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb1',
        read: false,
      }),
      expect.objectContaining({
        id: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb4',
        read: true,
      }),
    ])
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
    })).rejects.toMatchObject({ status: 403 })

    await loginAs('admin@kurum.gov.tr')
    const created = await api.admin.createUser({
      firstName: 'Yeni',
      lastName: 'Kullanıcı',
      email: 'yeni.kullanici@kurum.gov.tr',
      password: 'demo123',
    })
    expect(created).toMatchObject({
      email: 'yeni.kullanici@kurum.gov.tr',
      roleName: 'CALISAN',
    })
  })

  it('admin kullanıcı listesini sunucu filtreleri ve sayfalamasıyla normalize eder', async () => {
    await loginAs('admin@kurum.gov.tr')

    const result = await listAdminUsers({
      q: 'elif',
      role: 'CALISAN',
      active: true,
      page: 0,
      size: 1,
    })

    expect(result).toMatchObject({
      page: 0,
      size: 1,
      totalElements: 1,
      totalPages: 1,
    })
    expect(result.content).toEqual([
      expect.objectContaining({
        email: 'elif.akin@kurum.gov.tr',
        systemKey: 'CALISAN',
        roleName: 'CALISAN',
        isActive: true,
      }),
    ])
  })

  it('admin denetim kayıtlarını sunucu tarafında sayfalayıp ekran modeline dönüştürür', async () => {
    await loginAs('admin@kurum.gov.tr')

    const result = await listAdminAuditLogs({ page: 0, size: 2 })

    expect(result).toMatchObject({
      page: 0,
      size: 2,
      totalElements: 3,
      totalPages: 2,
    })
    expect(result.content).toHaveLength(2)
    expect(result.content[0]).toEqual(expect.objectContaining({
      type: 'USER',
      actor: 'Zeynep Yönetici',
    }))
  })

  it('teknik sunucu ve HTTP metot hatalarını kullanıcı dostu Türkçe mesaja dönüştürür', async () => {
    const { toApiClientError } = await import('./errors')
    const { AxiosError } = await import('axios')

    const raw405Error = new AxiosError('Request failed with status code 405')
    raw405Error.response = {
      status: 405,
      statusText: 'Method Not Allowed',
      headers: {},
      config: {} as any,
      data: {
        timestamp: '2026-08-19T11:35:00.000Z',
        status: 405,
        code: 'METHOD_NOT_ALLOWED',
        message: "Request method 'GET' is not supported",
      },
    }

    const clientError = toApiClientError(raw405Error)
    expect(clientError.message).toBe('İstenen işlem bu kaynak için geçerli değil veya desteklenmiyor.')
    expect(clientError.status).toBe(405)
    expect(clientError.code).toBe('METHOD_NOT_ALLOWED')
  })
})

