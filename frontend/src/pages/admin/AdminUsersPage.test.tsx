import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it } from 'vitest'
import App from '../../App'
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

  it('yeni hesabı rol seçtirmeden Çalışan olarak açar', async () => {
    const browser = userEvent.setup()
    renderAdminUsers()
    await browser.click(await screen.findByRole('button', { name: 'Yeni Hesap' }))
    expect(screen.queryByRole('combobox', { name: 'Başlangıç rolü' })).not.toBeInTheDocument()
    await browser.type(screen.getByRole('textbox', { name: 'Ad' }), 'Deniz')
    await browser.type(screen.getByRole('textbox', { name: 'Soyad' }), 'Kaya')
    await browser.type(screen.getByRole('textbox', { name: 'Kurumsal e-posta' }), 'deniz.kaya@kurum.gov.tr')
    await browser.click(screen.getByRole('button', { name: 'Hesap Aç' }))
    await browser.click(await screen.findByRole('button', { name: 'Tamam' }))

    expect(within(screen.getByRole('row', { name: /Deniz Kaya/ })).getByText('Çalışan')).toBeInTheDocument()
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
