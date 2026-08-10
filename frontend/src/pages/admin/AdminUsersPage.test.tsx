import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it } from 'vitest'
import App from '../../App'
import {
  createMockRegistrationRequest,
  readMockRegistrationRequests,
} from '../../mocks/registrationRequests'
import { getDemoUserByRole } from '../../mocks/users'

const mockSessionKey = 'ebys:mock-session:v1'

function renderAdminUsers() {
  window.sessionStorage.setItem(mockSessionKey, JSON.stringify(getDemoUserByRole('ADMIN')))
  return render(
    <MemoryRouter initialEntries={['/admin/kullanicilar']}>
      <App />
    </MemoryRouter>,
  )
}

describe('Admin kullanıcı yönetimi', () => {
  beforeEach(() => window.sessionStorage.clear())

  it('adminin manuel hesap oluşturmasını göstermez ve kayıt talebini seçilen rolle onaylar', async () => {
    const browser = userEvent.setup()
    createMockRegistrationRequest({
      firstName: 'Deniz',
      lastName: 'Kaya',
      email: 'deniz.kaya@kurum.gov.tr',
      password: 'guvenli123',
    })
    renderAdminUsers()
    expect(screen.queryByRole('button', { name: 'Yeni Hesap' })).not.toBeInTheDocument()

    const requestCard = await screen.findByRole('article', { name: 'Deniz Kaya kayıt talebi' })
    await browser.click(within(requestCard).getByRole('button', { name: 'Başvuruyu İncele' }))

    const dialog = screen.getByRole('dialog')
    expect(dialog).toHaveTextContent('Deniz')
    expect(dialog).toHaveTextContent('Kaya')
    expect(dialog).toHaveTextContent('deniz.kaya@kurum.gov.tr')
    expect(dialog).toHaveTextContent('Şifre korumalıdır')
    expect(within(dialog).queryByRole('option', { name: 'Sistem Yöneticisi' })).not.toBeInTheDocument()

    await browser.selectOptions(within(dialog).getByRole('combobox', { name: 'Onaylanacak rol' }), 'BASKAN')
    await browser.click(within(dialog).getByRole('button', { name: 'Onayla ve Hesabı Aç' }))

    expect(screen.queryByRole('article', { name: 'Deniz Kaya kayıt talebi' })).not.toBeInTheDocument()
    expect(within(screen.getByRole('row', { name: /Deniz Kaya/ })).getByText('Başkan')).toBeInTheDocument()
    expect(readMockRegistrationRequests()).toEqual([
      expect.objectContaining({ email: 'deniz.kaya@kurum.gov.tr', status: 'APPROVED' }),
    ])
  })

  it('kayıt talebini reddeder ve kullanıcı hesabı oluşturmaz', async () => {
    const browser = userEvent.setup()
    createMockRegistrationRequest({
      firstName: 'Selin',
      lastName: 'Demir',
      email: 'selin.demir@kurum.gov.tr',
      password: 'guvenli123',
    })
    renderAdminUsers()

    const requestCard = await screen.findByRole('article', { name: 'Selin Demir kayıt talebi' })
    await browser.click(within(requestCard).getByRole('button', { name: 'Başvuruyu İncele' }))
    await browser.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Talebi Reddet' }))

    expect(screen.queryByRole('article', { name: 'Selin Demir kayıt talebi' })).not.toBeInTheDocument()
    expect(screen.queryByRole('row', { name: /Selin Demir/ })).not.toBeInTheDocument()
    expect(readMockRegistrationRequests()).toEqual([
      expect.objectContaining({ email: 'selin.demir@kurum.gov.tr', status: 'REJECTED' }),
    ])
  })

  it('aktif bir kullanıcıya Admin rolü verir', async () => {
    const browser = userEvent.setup()
    renderAdminUsers()
    const elifRow = await screen.findByRole('row', { name: /Elif Akın/ })
    await browser.click(within(elifRow).getByRole('button', { name: 'Rolü Değiştir' }))
    await browser.selectOptions(screen.getByRole('combobox', { name: 'Yeni rol' }), 'ADMIN')
    await browser.click(screen.getByRole('button', { name: 'Değiştir' }))

    expect(within(screen.getByRole('row', { name: /Elif Akın/ })).getByText('Sistem Yöneticisi')).toBeInTheDocument()
  })

  it('Başkan Yardımcısı rolünü atomik olarak yeni kullanıcıya devreder', async () => {
    const browser = userEvent.setup()
    renderAdminUsers()
    const elifRow = await screen.findByRole('row', { name: /Elif Akın/ })
    await browser.click(within(elifRow).getByRole('button', { name: 'Rolü Değiştir' }))
    await browser.selectOptions(screen.getByRole('combobox', { name: 'Yeni rol' }), 'BASKAN_YARDIMCISI')
    expect(screen.getByRole('dialog')).toHaveTextContent('Ayşe Kaya kullanıcısından devralır')
    expect(screen.getByRole('dialog')).toHaveTextContent('Çalışan rolüne geçirilir')
    await browser.click(screen.getByRole('button', { name: 'Değiştir' }))

    expect(within(screen.getByRole('row', { name: /Elif Akın/ })).getByText('Başkan Yardımcısı')).toBeInTheDocument()
    expect(within(screen.getByRole('row', { name: /Ayşe Kaya/ })).getByText('Çalışan')).toBeInTheDocument()
  })

  it('Aktif Başkan Yardımcısını rol devredilmeden pasifleştirmez', async () => {
    const browser = userEvent.setup()
    renderAdminUsers()
    const deputyRow = await screen.findByRole('row', { name: /Ayşe Kaya/ })
    await browser.click(within(deputyRow).getByRole('button', { name: 'Pasifleştir' }))
    await browser.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Pasifleştir' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Önce Başkan Yardımcısı rolünü')
  })
})
