import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it } from 'vitest'
import App from '../../App'
import { apiBaseUrl } from '../../api/config'
import { apiMockServer } from '../../mocks/api/server'
import { seedAuthenticatedUser } from '../../test/auth'

async function renderRoles() {
  await seedAuthenticatedUser('ADMIN')
  return render(
    <MemoryRouter initialEntries={['/admin/roller']}>
      <App />
    </MemoryRouter>,
  )
}

describe('Admin rol listesi', () => {
  beforeEach(() => window.sessionStorage.clear())

  it('yerleşik ve panelden açılmış rolleri yalnız-okur tabloda listeler', async () => {
    await renderRoles()

    expect(await screen.findByRole('heading', { name: 'Roller' })).toBeInTheDocument()
    const dynamicRow = await screen.findByRole('row', { name: /Mali İşler Uzmanı/ })
    expect(within(dynamicRow).getByText('Panelden açılmış dinamik rol')).toBeInTheDocument()
    expect(screen.getByRole('row', { name: /BASKAN_YARDIMCISI/ })).toBeInTheDocument()
    expect(screen.getByText('5')).toBeInTheDocument()

    // Yalnız-okur ekran: hiçbir yazma eylemi sunulmaz.
    expect(screen.queryByRole('button', { name: /Yeni rol|Düzenle|Sil/ })).not.toBeInTheDocument()
  })

  it('rol adlarını sabit rol listesine göre çevirmeden, sunucudan geldiği gibi gösterir', async () => {
    await renderRoles()

    // Kısıt: AdminRole, types/auth.ts'teki UserRole union'ından bağımsızdır.
    // `roleLabels` uygulansaydı CALISAN "Çalışan", ADMIN "Sistem Yöneticisi"
    // olurdu; ham adların görünmesi bu bağın kurulmadığını kanıtlar.
    const roleTable = await screen.findByRole('table')
    expect(within(roleTable).getByText('CALISAN')).toBeInTheDocument()
    expect(within(roleTable).getByText('ADMIN')).toBeInTheDocument()
    expect(within(roleTable).getByText('Mali İşler Uzmanı')).toBeInTheDocument()
    expect(within(roleTable).queryByText('Çalışan')).not.toBeInTheDocument()
    expect(within(roleTable).queryByText('Başkan Yardımcısı')).not.toBeInTheDocument()
    expect(within(roleTable).queryByText('Sistem Yöneticisi')).not.toBeInTheDocument()
  })

  it('rol listesi alınamazsa hata bloğunu gösterip yeniden denemeye izin verir', async () => {
    const browser = userEvent.setup()
    // 403 seçildi: sorgu istemcisi yalnızca 5xx ve ağ hatalarını yeniden dener,
    // böylece test geri çekilme beklemeden hata durumuna ulaşır.
    apiMockServer.use(
      http.get(`${apiBaseUrl}/api/admin/roles`, () => HttpResponse.json({
        timestamp: new Date().toISOString(),
        status: 403,
        code: 'FORBIDDEN',
        message: 'Bu işlem için yetkiniz yok',
      }, { status: 403 })),
    )
    await renderRoles()

    expect(await screen.findByRole('heading', { name: 'Roller yüklenemedi' })).toBeInTheDocument()
    expect(screen.getByRole('alert')).toBeInTheDocument()

    apiMockServer.resetHandlers()
    await browser.click(screen.getByRole('button', { name: 'Tekrar dene' }))

    expect(await screen.findByRole('row', { name: /Mali İşler Uzmanı/ })).toBeInTheDocument()
  })

  it('sunucu boş liste döndürürse boş durum metnini gösterir', async () => {
    apiMockServer.use(
      http.get(`${apiBaseUrl}/api/admin/roles`, () => HttpResponse.json([])),
    )
    await renderRoles()

    expect(await screen.findByRole('heading', { name: 'Tanımlı rol bulunamadı' })).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })
})
