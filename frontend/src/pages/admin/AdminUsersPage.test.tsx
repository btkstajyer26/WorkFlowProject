import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it } from 'vitest'
import App from '../../App'
import { seedAuthenticatedUser } from '../../test/auth'

async function renderAdminUsers() {
  await seedAuthenticatedUser('ADMIN')
  return render(
    <MemoryRouter initialEntries={['/admin/kullanicilar']}>
      <App />
    </MemoryRouter>,
  )
}

describe('Admin kullanıcı yönetimi', () => {
  beforeEach(() => window.sessionStorage.clear())

  it('Adminin rol seçmeden Çalışan hesabı oluşturmasını sağlar', async () => {
    const browser = userEvent.setup()
    await renderAdminUsers()

    expect(screen.queryByText('Onay bekleyen kayıtlar')).not.toBeInTheDocument()
    await browser.click(await screen.findByRole('button', { name: 'Yeni kullanıcı' }))

    const dialog = screen.getByRole('dialog')
    expect(within(dialog).queryByRole('combobox')).not.toBeInTheDocument()
    expect(dialog).toHaveTextContent('otomatik olarak Çalışan rolüyle açılır')

    await browser.type(within(dialog).getByLabelText('Ad'), 'Deniz')
    await browser.type(within(dialog).getByLabelText('Soyad'), 'Kaya')
    await browser.type(within(dialog).getByLabelText('E-posta adresi'), 'deniz.kaya@kurum.gov.tr')
    await browser.type(within(dialog).getByLabelText('İlk giriş şifresi'), 'guvenli123')
    await browser.click(within(dialog).getByRole('button', { name: 'Kullanıcı Oluştur' }))

    expect(await screen.findByText('Kullanıcı oluşturuldu')).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(within(screen.getByRole('row', { name: /Deniz Kaya/ })).getByText('Çalışan')).toBeInTheDocument()
  })

  it('daha önce kullanılan e-posta adresini alan bazlı hata olarak gösterir', async () => {
    const browser = userEvent.setup()
    await renderAdminUsers()
    await browser.click(await screen.findByRole('button', { name: 'Yeni kullanıcı' }))

    const dialog = screen.getByRole('dialog')
    await browser.type(within(dialog).getByLabelText('Ad'), 'John')
    await browser.type(within(dialog).getByLabelText('Soyad'), 'Doe')
    await browser.type(within(dialog).getByLabelText('E-posta adresi'), 'john.doe@kurum.gov.tr')
    await browser.type(within(dialog).getByLabelText('İlk giriş şifresi'), 'guvenli123')
    await browser.click(within(dialog).getByRole('button', { name: 'Kullanıcı Oluştur' }))

    expect(await within(dialog).findByRole('alert')).toHaveTextContent('Bu e-posta adresiyle kayıtlı bir kullanıcı zaten var.')
    expect(screen.getByRole('dialog')).toBeInTheDocument()
  })

  it('aktif bir kullanıcıya Admin rolü verir', async () => {
    const browser = userEvent.setup()
    await renderAdminUsers()
    const elifRow = await screen.findByRole('row', { name: /Elif Akın/ })
    await browser.click(within(elifRow).getByRole('button', { name: 'Rolü Değiştir' }))
    await browser.selectOptions(screen.getByRole('combobox', { name: 'Yeni rol' }), 'ADMIN')
    await browser.click(screen.getByRole('button', { name: 'Değiştir' }))

    expect(within(screen.getByRole('row', { name: /Elif Akın/ })).getByText('Sistem Yöneticisi')).toBeInTheDocument()
  })

  it('Başkan Yardımcısı rolünü atomik olarak yeni kullanıcıya devreder', async () => {
    const browser = userEvent.setup()
    await renderAdminUsers()
    const elifRow = await screen.findByRole('row', { name: /Elif Akın/ })
    await browser.click(within(elifRow).getByRole('button', { name: 'Rolü Değiştir' }))
    await browser.selectOptions(screen.getByRole('combobox', { name: 'Yeni rol' }), 'BASKAN_YARDIMCISI')
    expect(screen.getByRole('dialog')).toHaveTextContent('Ayşe Kaya kullanıcısından devralır')
    expect(screen.getByRole('dialog')).toHaveTextContent('Çalışan rolüne geçirilir')
    await browser.click(screen.getByRole('button', { name: 'Değiştir' }))

    expect(within(screen.getByRole('row', { name: /Elif Akın/ })).getByText('Başkan Yardımcısı')).toBeInTheDocument()
    expect(within(screen.getByRole('row', { name: /Ayşe Kaya/ })).getByText('Çalışan')).toBeInTheDocument()
  })

  it('Başkan Yardımcısı rolü devredilmeden hesabın pasifleştirilmesini engeller', async () => {
    const browser = userEvent.setup()
    await renderAdminUsers()
    const deputyRow = await screen.findByRole('row', { name: /Ayşe Kaya/ })
    await browser.click(within(deputyRow).getByRole('button', { name: 'Pasifleştir' }))
    await browser.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Pasifleştir' }))

    expect(await within(screen.getByRole('dialog')).findByRole('alert')).toHaveTextContent(
      'Önce Başkan Yardımcısı rolünü başka bir aktif Çalışana devredin.',
    )
    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(within(screen.getByRole('row', { name: /Ayşe Kaya/ })).getByText('Aktif')).toBeInTheDocument()
  })
})
