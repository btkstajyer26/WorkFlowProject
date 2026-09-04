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

describe('Admin rol yönetimi', () => {
  beforeEach(() => window.sessionStorage.clear())

  it('varsayılan görünümde yalnız aktif rolleri listeler', async () => {
    await renderRoles()

    expect(await screen.findByRole('heading', { name: 'Roller' })).toBeInTheDocument()
    const dynamicRow = await screen.findByRole('row', { name: /Mali İşler Uzmanı/ })
    expect(within(dynamicRow).getByText('Dinamik rol')).toBeInTheDocument()
    expect(screen.getByRole('row', { name: /BASKAN_YARDIMCISI/ })).toBeInTheDocument()
    // Pasif rol varsayılan çağrıda gelmez.
    expect(screen.queryByRole('row', { name: /Arşiv Sorumlusu/ })).not.toBeInTheDocument()
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

  it('pasif rolleri göster seçilince pasifleştirilmiş rol listeye gelir', async () => {
    const browser = userEvent.setup()
    await renderRoles()
    await screen.findByRole('row', { name: /Mali İşler Uzmanı/ })

    await browser.click(screen.getByRole('checkbox', { name: /Pasif rolleri de göster/ }))

    const passiveRow = await screen.findByRole('row', { name: /Arşiv Sorumlusu/ })
    expect(within(passiveRow).getByText('Pasif')).toBeInTheDocument()
  })

  it('panelden yeni dinamik rol açar', async () => {
    const browser = userEvent.setup()
    await renderRoles()
    await screen.findByRole('row', { name: /Mali İşler Uzmanı/ })

    await browser.click(screen.getByRole('button', { name: 'Yeni rol' }))
    const dialog = screen.getByRole('dialog')
    await browser.type(within(dialog).getByLabelText('Rol adı'), 'Evrak Kayıt Memuru')
    await browser.type(within(dialog).getByLabelText('Açıklama'), 'Gelen evrakı kaydeder')
    await browser.click(within(dialog).getByRole('checkbox', { name: /İş akışı aktörü/ }))
    await browser.click(within(dialog).getByRole('button', { name: 'Rol Oluştur' }))

    expect(await screen.findByText('Rol oluşturuldu')).toBeInTheDocument()
    const createdRow = await screen.findByRole('row', { name: /Evrak Kayıt Memuru/ })
    expect(within(createdRow).getByText('Dinamik rol')).toBeInTheDocument()
    expect(within(createdRow).getByText('İş akışı aktörü')).toBeInTheDocument()
  })

  it('aynı adla ikinci rol açılmasını sunucu reddeder ve hata formda görünür', async () => {
    const browser = userEvent.setup()
    await renderRoles()
    await screen.findByRole('row', { name: /Mali İşler Uzmanı/ })

    await browser.click(screen.getByRole('button', { name: 'Yeni rol' }))
    const dialog = screen.getByRole('dialog')
    await browser.type(within(dialog).getByLabelText('Rol adı'), 'Mali İşler Uzmanı')
    await browser.click(within(dialog).getByRole('button', { name: 'Rol Oluştur' }))

    expect(await within(dialog).findByRole('alert')).toHaveTextContent('zaten kullanılıyor')
    expect(screen.getByRole('dialog')).toBeInTheDocument()
  })

  it('yalnız harf büyüklüğü farklı adla rol açılmasını engeller', async () => {
    const browser = userEvent.setup()
    await renderRoles()
    await screen.findByRole('row', { name: /Mali İşler Uzmanı/ })

    await browser.click(screen.getByRole('button', { name: 'Yeni rol' }))
    const dialog = screen.getByRole('dialog')
    await browser.type(within(dialog).getByLabelText('Rol adı'), 'mali işler uzmanı')
    await browser.click(within(dialog).getByRole('button', { name: 'Rol Oluştur' }))

    expect(await within(dialog).findByRole('alert')).toHaveTextContent('Mali İşler Uzmanı')
    expect(screen.getByRole('dialog')).toBeInTheDocument()
  })

  it('sistem rolü pasifleştirilemez, dinamik rol pasifleştirilebilir', async () => {
    const browser = userEvent.setup()
    await renderRoles()

    const systemRow = await screen.findByRole('row', { name: /CALISAN/ })
    expect(within(systemRow).getByRole('button', { name: 'Pasifleştir' })).toBeDisabled()

    const dynamicRow = screen.getByRole('row', { name: /Mali İşler Uzmanı/ })
    await browser.click(within(dynamicRow).getByRole('button', { name: 'Pasifleştir' }))

    expect(await screen.findByText('Rol pasifleştirildi')).toBeInTheDocument()
    // Varsayılan görünüm yalnız aktifleri gösterdiği için satır listeden düşer.
    expect(screen.queryByRole('row', { name: /Mali İşler Uzmanı/ })).not.toBeInTheDocument()
  })

  it('rolün adını ve açıklamasını günceller', async () => {
    const browser = userEvent.setup()
    await renderRoles()
    const dynamicRow = await screen.findByRole('row', { name: /Mali İşler Uzmanı/ })

    await browser.click(within(dynamicRow).getByRole('button', { name: 'Düzenle' }))
    const dialog = screen.getByRole('dialog')
    const nameInput = within(dialog).getByLabelText('Rol adı')
    await browser.clear(nameInput)
    await browser.type(nameInput, 'Bütçe Uzmanı')
    await browser.click(within(dialog).getByRole('button', { name: 'Kaydet' }))

    expect(await screen.findByText('Rol güncellendi')).toBeInTheDocument()
    expect(await screen.findByRole('row', { name: /Bütçe Uzmanı/ })).toBeInTheDocument()
  })

  it('sistem rolü düzenlenirken iş akışı aktörü kutusu kilitlidir', async () => {
    const browser = userEvent.setup()
    await renderRoles()
    const systemRow = await screen.findByRole('row', { name: /CALISAN/ })

    await browser.click(within(systemRow).getByRole('button', { name: 'Düzenle' }))

    const dialog = screen.getByRole('dialog')
    expect(within(dialog).getByRole('checkbox', { name: /İş akışı aktörü/ })).toBeDisabled()
    expect(dialog).toHaveTextContent('Sistem rolünün aktörlüğü değiştirilemez')
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
