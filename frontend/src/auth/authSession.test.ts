import { HttpResponse, http } from 'msw'
import { describe, expect, it } from 'vitest'
import { api } from '../api/client'
import { apiBaseUrl } from '../api/config'
import { mockApiCategories } from '../mocks/api/db'
import { apiMockServer } from '../mocks/api/server'
import {
  endAuthSession,
  refreshAuthSession,
  restoreAuthSession,
  startAuthSession,
} from './authSession'

describe('authSession', () => {
  it.each([
    { roleId: 101, systemKey: null, roleName: 'Satın Alma Uzmanı' },
    { roleId: 1, systemKey: 'CALISAN', roleName: 'Kurum Personeli' },
  ])('değiştirilebilir rol adıyla girişe izin verir: $roleName', async (role) => {
    apiMockServer.use(http.get(`${apiBaseUrl}/api/users/me`, () => HttpResponse.json({
      id: 'aaaaaaaa-1111-4111-8111-aaaaaaaaaaaa',
      firstName: 'Test', lastName: 'Kullanıcı', email: 'john.doe@kurum.gov.tr',
      active: true, ...role,
    })))
    await expect(startAuthSession('john.doe@kurum.gov.tr', 'demo123')).resolves.toMatchObject({ user: role })
  })

  it('giriş tokenını korumalı isteklere ekler ve çıkışta temizler', async () => {
    await startAuthSession('john.doe@kurum.gov.tr', 'demo123')

    await expect(api.categories.getAllCategories()).resolves.toHaveLength(5)

    await endAuthSession()

    await expect(api.categories.getAllCategories()).rejects.toMatchObject({
      code: 'UNAUTHORIZED',
      status: 401,
    })
  })

  it('refresh token ile erişim tokenını merkezi olarak yeniler', async () => {
    await startAuthSession('john.doe@kurum.gov.tr', 'demo123')

    await expect(refreshAuthSession()).resolves.toMatchObject({
      accessToken: expect.any(String),
      refreshToken: expect.any(String),
    })
  })

  it('korumalı istek 401 döndüğünde tokenı bir kez yenileyip isteği tekrarlar', async () => {
    await startAuthSession('john.doe@kurum.gov.tr', 'demo123')
    let categoryRequestCount = 0
    let refreshRequestCount = 0

    apiMockServer.use(
      http.get(`${apiBaseUrl}/api/categories`, () => {
        categoryRequestCount += 1
        return categoryRequestCount === 1
          ? HttpResponse.json({
              timestamp: new Date().toISOString(),
              status: 401,
              code: 'UNAUTHORIZED',
              message: 'Access token süresi doldu',
            }, { status: 401 })
          : HttpResponse.json(mockApiCategories)
      }),
      http.post(`${apiBaseUrl}/api/auth/refresh`, async ({ request }) => {
        refreshRequestCount += 1
        const body = await request.json() as { refreshToken?: string }
        return HttpResponse.json({
          accessToken: 'msw-access-user-demo-001',
          refreshToken: body.refreshToken,
          mustChangePassword: false,
        })
      }),
    )

    await expect(api.categories.getAllCategories()).resolves.toEqual(mockApiCategories)
    expect(categoryRequestCount).toBe(2)
    expect(refreshRequestCount).toBe(1)
  })

  it('refresh tokenı localStorage üzerinden yeniler ve kullanıcıyı /api/users/me ile geri yükler', async () => {
    const session = await startAuthSession('john.doe@kurum.gov.tr', 'demo123')

    expect(window.localStorage.length).toBe(1)
    await expect(restoreAuthSession()).resolves.toEqual(session.user)
    await expect(api.categories.getAllCategories()).resolves.toHaveLength(5)
  })

  it('çıkışta kalıcı oturum verilerini de temizler', async () => {
    await startAuthSession('john.doe@kurum.gov.tr', 'demo123')

    await endAuthSession()

    expect(window.localStorage.length).toBe(0)
  })
})
