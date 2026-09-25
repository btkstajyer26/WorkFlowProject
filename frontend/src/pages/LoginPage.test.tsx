import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, useLocation } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { LoginPage } from './LoginPage'

function LocationProbe() {
  const location = useLocation()
  return <output aria-label="Geçerli adres">{location.pathname}</output>
}

describe('LoginPage', () => {
  it('giriş butonunu MSW auth endpointine bağlar ve kullanıcıyı yönlendirir', async () => {
    const user = userEvent.setup()
    const onLogin = vi.fn()

    render(
      <MemoryRouter initialEntries={['/giris']}>
        <LoginPage user={null} onLogin={onLogin} />
        <LocationProbe />
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText('E-posta adresi'), 'john.doe@kurum.gov.tr')
    await user.type(screen.getByLabelText('Şifre'), 'demo123')
    await user.click(screen.getByRole('button', { name: 'Giriş Yap' }))

    expect(onLogin).toHaveBeenCalledWith(expect.objectContaining({
      email: 'john.doe@kurum.gov.tr',
      systemKey: 'CALISAN',
    }))
    await waitFor(() => {
      expect(screen.getByLabelText('Geçerli adres')).toHaveTextContent('/dashboard')
    })
  })

  it('MSW auth reddettiğinde API hata mesajını gösterir', async () => {
    const user = userEvent.setup()
    const onLogin = vi.fn()

    render(
      <MemoryRouter initialEntries={['/giris']}>
        <LoginPage user={null} onLogin={onLogin} />
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText('E-posta adresi'), 'john.doe@kurum.gov.tr')
    await user.type(screen.getByLabelText('Şifre'), 'yanlis123')
    await user.click(screen.getByRole('button', { name: 'Giriş Yap' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('E-posta adresi veya şifre hatalı.')
    expect(onLogin).not.toHaveBeenCalled()
  })

  it('ilk girişte şifre değişikliği zorunlu olan kullanıcıyı ilgili sayfaya yönlendirir', async () => {
    const user = userEvent.setup()
    const onLogin = vi.fn()

    render(
      <MemoryRouter initialEntries={['/giris']}>
        <LoginPage user={null} onLogin={onLogin} />
        <LocationProbe />
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText('E-posta adresi'), 'ilk.giris@kurum.gov.tr')
    await user.type(screen.getByLabelText('Şifre'), 'Gecici123')
    await user.click(screen.getByRole('button', { name: 'Giriş Yap' }))

    expect(onLogin).toHaveBeenCalledWith(expect.objectContaining({
      email: 'ilk.giris@kurum.gov.tr',
      mustChangePassword: true,
    }))
    await waitFor(() => {
      expect(screen.getByLabelText('Geçerli adres')).toHaveTextContent('/sifre-degistir')
    })
  })

  it('öz-kayıt seçeneği yerine hesabın Admin tarafından açıldığını belirtir', () => {
    render(
      <MemoryRouter initialEntries={['/giris']}>
        <LoginPage user={null} onLogin={vi.fn()} />
      </MemoryRouter>,
    )

    expect(screen.queryByRole('button', { name: 'Kayıt Ol' })).not.toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Kayıt olun' })).not.toBeInTheDocument()
    expect(screen.getByText(/Hesabınız kurumunuzun sistem yöneticisi tarafından oluşturulur/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Şifremi unuttum' })).toHaveAttribute('href', '/sifre-sifirla')
  })

  it('giriş bilgilerini gerçek değer yerine placeholder olarak gösterir', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter initialEntries={['/giris']}>
        <LoginPage user={null} onLogin={vi.fn()} />
      </MemoryRouter>,
    )

    const emailInput = screen.getByLabelText('E-posta adresi')
    const passwordInput = screen.getByLabelText('Şifre')
    expect(emailInput).toHaveValue('')
    expect(emailInput).toHaveAttribute('placeholder', 'ad.soyad@kurum.gov.tr')
    expect(passwordInput).toHaveValue('')
    expect(passwordInput).toHaveAttribute('placeholder', 'Şifrenizi girin')

    await user.type(emailInput, 'a')
    expect(emailInput).toHaveValue('a')
  })
})
