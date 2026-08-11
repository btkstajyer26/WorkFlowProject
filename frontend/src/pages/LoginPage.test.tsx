import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, useLocation } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { LoginPage } from './LoginPage'
import {
  createMockRegistrationRequest,
  updateMockRegistrationRequestStatus,
} from '../mocks/registrationRequests'
import type { RegistrationRequest } from '../types/registration'

function LocationProbe() {
  const location = useLocation()
  return <output aria-label="Geçerli adres">{location.pathname}</output>
}

describe('LoginPage registration flow', () => {
  it('giriş butonunu MSW auth endpointine bağlar ve kullanıcıyı yönlendirir', async () => {
    const user = userEvent.setup()
    const onLogin = vi.fn()

    render(
      <MemoryRouter initialEntries={['/giris']}>
        <LoginPage user={null} onLogin={onLogin} onRegister={vi.fn()} />
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
        <LoginPage user={null} onLogin={onLogin} onRegister={vi.fn()} />
      </MemoryRouter>,
    )

    await user.clear(screen.getByLabelText('Şifre'))
    await user.type(screen.getByLabelText('Şifre'), 'yanlis123')
    await user.click(screen.getByRole('button', { name: 'Giriş Yap' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('E-posta adresi veya şifre hatalı.')
    expect(onLogin).not.toHaveBeenCalled()
  })

  it('aynı sayfada kayıt formuna geçer ve rol seçimi göstermez', async () => {
    const user = userEvent.setup()
    const onRegister = vi.fn()

    render(
      <MemoryRouter initialEntries={['/giris']}>
        <LoginPage user={null} onLogin={vi.fn()} onRegister={onRegister} />
        <LocationProbe />
      </MemoryRouter>,
    )

    await user.click(screen.getByRole('button', { name: 'Kayıt Ol' }))

    expect(screen.getByRole('heading', { name: 'Kayıt olun' })).toBeInTheDocument()
    expect(screen.getByLabelText('Geçerli adres')).toHaveTextContent('/giris')
    expect(screen.getByLabelText('Ad')).toBeInTheDocument()
    expect(screen.getByLabelText('Soyad')).toBeInTheDocument()
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument()
    expect(onRegister).not.toHaveBeenCalled()
  })

  it('kayıt talebini Çalışan rolü ve admin onayı mesajıyla tamamlar', async () => {
    const user = userEvent.setup()
    const request: RegistrationRequest = {
      id: 'registration-test-001',
      firstName: 'Deniz',
      lastName: 'Yılmaz',
      email: 'deniz.yilmaz@kurum.gov.tr',
      requestedRole: 'CALISAN',
      status: 'PENDING',
      createdAt: '2026-08-10T12:00:00.000Z',
    }
    const onRegister = vi.fn().mockResolvedValue(request)

    render(
      <MemoryRouter initialEntries={['/giris']}>
        <LoginPage user={null} onLogin={vi.fn()} onRegister={onRegister} />
      </MemoryRouter>,
    )

    await user.click(screen.getByRole('button', { name: 'Kayıt Ol' }))
    await user.type(screen.getByLabelText('Ad'), 'Deniz')
    await user.type(screen.getByLabelText('Soyad'), 'Yılmaz')
    await user.type(screen.getByLabelText('Kurumsal e-posta adresi'), 'deniz.yilmaz@kurum.gov.tr')
    await user.type(screen.getByLabelText('Şifre'), 'guvenli123')
    await user.type(screen.getByLabelText('Şifre tekrarı'), 'guvenli123')
    await user.click(screen.getByRole('button', { name: 'Kayıt Talebi Gönder' }))

    expect(await screen.findByRole('heading', { name: 'Kayıt talebiniz alındı' })).toBeInTheDocument()
    expect(screen.getByText('Admin onayı bekleniyor')).toBeInTheDocument()
    expect(onRegister).toHaveBeenCalledWith({
      firstName: 'Deniz',
      lastName: 'Yılmaz',
      email: 'deniz.yilmaz@kurum.gov.tr',
      password: 'guvenli123',
    })
  })

  it('bekleyen talep sahibinin admin onayından önce giriş yapmasını engeller', async () => {
    const user = userEvent.setup()
    const onLogin = vi.fn()
    createMockRegistrationRequest({
      firstName: 'Deniz',
      lastName: 'Yılmaz',
      email: 'deniz.yilmaz@kurum.gov.tr',
      password: 'guvenli123',
    })

    render(
      <MemoryRouter initialEntries={['/giris']}>
        <LoginPage user={null} onLogin={onLogin} onRegister={vi.fn()} />
      </MemoryRouter>,
    )

    const emailInput = screen.getByLabelText('E-posta adresi')
    const passwordInput = screen.getByLabelText('Şifre')
    await user.clear(emailInput)
    await user.type(emailInput, 'deniz.yilmaz@kurum.gov.tr')
    await user.clear(passwordInput)
    await user.type(passwordInput, 'guvenli123')
    await user.click(screen.getByRole('button', { name: 'Giriş Yap' }))

    expect(await screen.findByText('Kayıt talebiniz sistem yöneticisinin onayını bekliyor.')).toBeInTheDocument()
    expect(onLogin).not.toHaveBeenCalled()
  })

  it('reddedilen talep sahibine karar durumunu bildirir ve girişi engeller', async () => {
    const user = userEvent.setup()
    const onLogin = vi.fn()
    const request = createMockRegistrationRequest({
      firstName: 'Selin',
      lastName: 'Demir',
      email: 'selin.demir@kurum.gov.tr',
      password: 'guvenli123',
    })
    updateMockRegistrationRequestStatus(request.id, 'REJECTED')

    render(
      <MemoryRouter initialEntries={['/giris']}>
        <LoginPage user={null} onLogin={onLogin} onRegister={vi.fn()} />
      </MemoryRouter>,
    )

    await user.clear(screen.getByLabelText('E-posta adresi'))
    await user.type(screen.getByLabelText('E-posta adresi'), 'selin.demir@kurum.gov.tr')
    await user.clear(screen.getByLabelText('Şifre'))
    await user.type(screen.getByLabelText('Şifre'), 'guvenli123')
    await user.click(screen.getByRole('button', { name: 'Giriş Yap' }))

    expect(await screen.findByText('Kayıt talebiniz sistem yöneticisi tarafından reddedildi.')).toBeInTheDocument()
    expect(onLogin).not.toHaveBeenCalled()
  })
})
