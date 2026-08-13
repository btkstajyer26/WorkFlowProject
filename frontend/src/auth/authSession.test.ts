import { describe, expect, it } from 'vitest'
import { api } from '../api/client'
import {
  endAuthSession,
  persistAuthenticatedUser,
  readPersistedAuthenticatedUser,
  refreshAuthSession,
  restoreAuthSession,
  startAuthSession,
} from './authSession'
import { getDemoUserByRole } from '../mocks/users'

describe('authSession', () => {
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

  it('refresh token ve kullanıcıyı localStorage üzerinden geri yükler', async () => {
    const user = getDemoUserByRole('CALISAN')
    await startAuthSession('john.doe@kurum.gov.tr', 'demo123')
    persistAuthenticatedUser(user)

    expect(window.localStorage.length).toBe(2)
    expect(readPersistedAuthenticatedUser()).toEqual(user)
    await expect(restoreAuthSession()).resolves.toEqual(user)
    await expect(api.categories.getAllCategories()).resolves.toHaveLength(5)
  })

  it('çıkışta kalıcı oturum verilerini de temizler', async () => {
    await startAuthSession('john.doe@kurum.gov.tr', 'demo123')
    persistAuthenticatedUser(getDemoUserByRole('CALISAN'))

    await endAuthSession()

    expect(window.localStorage.length).toBe(0)
    expect(readPersistedAuthenticatedUser()).toBeNull()
  })
})
