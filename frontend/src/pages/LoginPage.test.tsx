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

    await user.click(screen.getByRole('button', { name: 'Giriş Yap' }))

    expect(onLogin).toHaveBeenCalledWith(expect.objectContaining({
      email: 'john.doe@kurum.gov.tr',
      role: 'CALISAN',
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

    await user.clear(screen.getByLabelText('Şifre'))
    await user.type(screen.getByLabelText('Şifre'), 'yanlis123')
    await user.click(screen.getByRole('button', { name: 'Giriş Yap' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('E-posta adresi veya şifre hatalı.')
    expect(onLogin).not.toHaveBeenCalled()
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
  })
})
