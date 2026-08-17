import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { startAuthSession } from '../auth/authSession'
import type { AuthUser } from '../types/auth'
import { PasswordChangePage } from './PasswordChangePage'

const firstLoginUser: AuthUser = {
  id: 'user-demo-first-login',
  firstName: 'İlk',
  lastName: 'Giriş',
  email: 'ilk.giris@kurum.gov.tr',
  role: 'CALISAN',
  mustChangePassword: true,
}

function LocationProbe() {
  const location = useLocation()
  return <output aria-label="Geçerli adres">{`${location.pathname}${location.search}`}</output>
}

function renderPage(
  user: AuthUser | null = firstLoginUser,
  onPasswordChanged = vi.fn(),
  onUseAnotherAccount = vi.fn(),
) {
  render(
    <MemoryRouter initialEntries={['/sifre-degistir']}>
      <Routes>
        <Route path="*" element={(
          <>
            <PasswordChangePage
              user={user}
              onPasswordChanged={onPasswordChanged}
              onUseAnotherAccount={onUseAnotherAccount}
            />
            <LocationProbe />
          </>
        )} />
      </Routes>
    </MemoryRouter>,
  )
  return { onPasswordChanged, onUseAnotherAccount }
}

describe('PasswordChangePage', () => {
  it('zorunlu olmayan kullanıcıya isteğe bağlı değişiklik formunu gösterir', () => {
    renderPage({ ...firstLoginUser, mustChangePassword: false })

    expect(screen.getByText('Hesap güvenliği')).toBeInTheDocument()
    expect(screen.getByLabelText('Mevcut şifre')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Profile dön' })).toHaveAttribute('href', '/profil')
  })

  it('eşleşmeyen yeni şifreleri endpoint çağrısından önce reddeder', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.type(screen.getByLabelText('Mevcut şifre'), 'Gecici123')
    await user.type(screen.getByLabelText('Yeni şifre'), 'YeniParola123')
    await user.type(screen.getByLabelText('Yeni şifre tekrar'), 'FarkliParola123')
    await user.click(screen.getByRole('button', { name: 'Şifreyi Güncelle' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Yeni şifreler birbiriyle eşleşmiyor.')
  })

  it('yanlış mevcut şifreyi alan bazlı gösterir', async () => {
    const user = userEvent.setup()
    await startAuthSession('ilk.giris@kurum.gov.tr', 'Gecici123')
    renderPage()

    await user.type(screen.getByLabelText('Mevcut şifre'), 'Yanlis123')
    await user.type(screen.getByLabelText('Yeni şifre'), 'YeniParola123')
    await user.type(screen.getByLabelText('Yeni şifre tekrar'), 'YeniParola123')
    await user.click(screen.getByRole('button', { name: 'Şifreyi Güncelle' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Mevcut şifreniz doğru değil.')
  })

  it('başarılı değişiklikten sonra oturumu sonlandırıp giriş ekranına yönlendirir', async () => {
    const user = userEvent.setup()
    const onPasswordChanged = vi.fn()
    await startAuthSession('ilk.giris@kurum.gov.tr', 'Gecici123')
    renderPage(firstLoginUser, onPasswordChanged)

    await user.type(screen.getByLabelText('Mevcut şifre'), 'Gecici123')
    await user.type(screen.getByLabelText('Yeni şifre'), 'YeniParola123')
    await user.type(screen.getByLabelText('Yeni şifre tekrar'), 'YeniParola123')
    await user.click(screen.getByRole('button', { name: 'Şifreyi Güncelle' }))

    await waitFor(() => expect(onPasswordChanged).toHaveBeenCalledOnce())
  })
})
