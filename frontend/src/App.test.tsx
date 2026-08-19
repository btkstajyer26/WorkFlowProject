import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from './App'
import { requestPasswordReset } from './api/auth'
import { getMockUserByRole, mockPasswordResetTokenFor } from './mocks/api/auth'
import { api } from './api/client'
import { apiBaseUrl } from './api/config'
import { apiMockServer } from './mocks/api/server'
import { seedAuthenticatedUser } from './test/auth'

function renderApp(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}

describe('App authorization boundaries', () => {
  it('MSW ile açılan oturumu çıkış butonundan kapatır', async () => {
    const user = userEvent.setup()
    renderApp('/giris')

    await user.type(await screen.findByLabelText('E-posta adresi'), 'john.doe@kurum.gov.tr')
    await user.type(screen.getByLabelText('Şifre'), 'demo123')
    await user.click(await screen.findByRole('button', { name: 'Giriş Yap' }))
    expect(await screen.findByRole('heading', { name: /Hoş geldiniz/ })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Çıkış' }))
    await user.click(screen.getByRole('button', { name: 'Çıkış Yap' }))

    expect(await screen.findByRole('heading', { name: 'Hesabınıza giriş yapın' })).toBeInTheDocument()
  })

  it('oturumsuz kullanıcıyı giriş ekranına yönlendirir', async () => {
    renderApp('/dashboard')
    expect(await screen.findByRole('heading', { name: 'Hesabınıza giriş yapın' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Demo hesapları' })).not.toBeInTheDocument()
  })

  it('Başkan Yardımcısına yeni kayıt oluşturma kontrolünü göstermez', async () => {
    await seedAuthenticatedUser('BASKAN_YARDIMCISI')
    renderApp('/dashboard')
    expect(await screen.findByRole('heading', { name: /Hoş geldiniz/ })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Yeni Kayıt' })).not.toBeInTheDocument()
  })

  it('Başkanın kendi kapsamı dışındaki kaydını 403 ile sınırlar', async () => {
    await seedAuthenticatedUser('BASKAN')
    renderApp('/kayitlar/rec-001')
    expect(await screen.findByRole('heading', { name: 'Bu sayfayı görüntüleme yetkiniz yok' })).toBeInTheDocument()
  })

  it('olmayan kayıt için 404 ekranını gösterir', async () => {
    await seedAuthenticatedUser('CALISAN')
    renderApp('/kayitlar/olmayan-kayit')
    expect(await screen.findByRole('heading', { name: 'Aradığınız sayfa bulunamadı' })).toBeInTheDocument()
  })

  it('refresh token geçersizse oturumu temizleyip süresi doldu mesajıyla girişe yönlendirir', async () => {
    await seedAuthenticatedUser('CALISAN')
    apiMockServer.use(
      http.post(`${apiBaseUrl}/api/auth/refresh`, () => HttpResponse.json({
        timestamp: new Date().toISOString(),
        status: 401,
        code: 'UNAUTHORIZED',
        message: 'Refresh token geçersiz',
      }, { status: 401 })),
    )

    renderApp('/dashboard')

    expect(await screen.findByRole('heading', { name: 'Hesabınıza giriş yapın' })).toBeInTheDocument()
    expect(screen.getByText('Oturumunuzun süresi doldu. Lütfen tekrar giriş yapın.')).toBeInTheDocument()
  })

  it('e-posta deep link adresini kayıt detayına yönlendirir', async () => {
    await seedAuthenticatedUser('CALISAN')
    renderApp('/records/rec-001')

    expect(await screen.findByRole('heading', { name: 'Sunucu Donanım Alım Talebi' })).toBeInTheDocument()
  })

  it('zorunlu şifre değişikliği tamamlanmadan korumalı sayfaları açmaz', async () => {
    getMockUserByRole('CALISAN').mustChangePassword = true
    await seedAuthenticatedUser('CALISAN')
    renderApp('/kayitlar')

    expect(await screen.findByRole('heading', { name: 'Şifrenizi değiştirin' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Tüm Kayıtlarım' })).not.toBeInTheDocument()
  })

  it('zorunlu şifre değişikliğinden sonra oturumu kapatıp yeniden giriş ister', async () => {
    const user = userEvent.setup()
    getMockUserByRole('CALISAN').mustChangePassword = true
    await seedAuthenticatedUser('CALISAN')
    renderApp('/dashboard')

    await screen.findByRole('heading', { name: 'Şifrenizi değiştirin' })
    await user.type(screen.getByLabelText('Mevcut şifre'), 'demo123')
    await user.type(screen.getByLabelText('Yeni şifre'), 'YeniParola123')
    await user.type(screen.getByLabelText('Yeni şifre tekrar'), 'YeniParola123')
    await user.click(screen.getByRole('button', { name: 'Şifreyi Güncelle' }))

    expect(await screen.findByRole('heading', { name: 'Hesabınıza giriş yapın' })).toBeInTheDocument()
    expect(await screen.findByText('Şifreniz değiştirildi. Yeni şifrenizle tekrar giriş yapın.')).toBeInTheDocument()
  })

  it('e-posta bağlantısındaki tek kullanımlık token ile oturumsuz şifre sıfırlar', async () => {
    const user = userEvent.setup()
    await requestPasswordReset({ email: 'john.doe@kurum.gov.tr' })
    const token = mockPasswordResetTokenFor('user-demo-001')
    renderApp(`/sifre-degistir?token=${encodeURIComponent(token)}`)

    expect(await screen.findByRole('heading', { name: 'Yeni şifrenizi belirleyin' })).toBeInTheDocument()
    expect(screen.queryByLabelText('Mevcut şifre')).not.toBeInTheDocument()
    await user.type(screen.getByLabelText('Yeni şifre'), 'YeniParola123')
    await user.type(screen.getByLabelText('Yeni şifre tekrar'), 'YeniParola123')
    await user.click(screen.getByRole('button', { name: 'Şifreyi sıfırla' }))

    expect(await screen.findByRole('heading', { name: 'Hesabınıza giriş yapın' })).toBeInTheDocument()
    expect(screen.getByText('Şifreniz sıfırlandı. Yeni şifrenizle giriş yapabilirsiniz.')).toBeInTheDocument()

    await user.type(screen.getByLabelText('E-posta adresi'), 'john.doe@kurum.gov.tr')
    await user.type(screen.getByLabelText('Şifre'), 'YeniParola123')
    await user.click(screen.getByRole('button', { name: 'Giriş Yap' }))
    expect(await screen.findByRole('heading', { name: /Hoş geldiniz/ })).toBeInTheDocument()
  })

  it('geçersiz şifre sıfırlama tokenı için yeni bağlantı istemeyi önerir', async () => {
    const user = userEvent.setup()
    renderApp('/sifre-degistir?token=gecersiz-token')

    await screen.findByRole('heading', { name: 'Yeni şifrenizi belirleyin' })
    await user.type(screen.getByLabelText('Yeni şifre'), 'YeniParola123')
    await user.type(screen.getByLabelText('Yeni şifre tekrar'), 'YeniParola123')
    await user.click(screen.getByRole('button', { name: 'Şifreyi sıfırla' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('geçersiz, kullanılmış veya süresi dolmuş')
    expect(screen.getByRole('link', { name: 'Yeni bağlantı iste' })).toHaveAttribute('href', '/sifre-sifirla')
  })

  it('kayıt detayında API tarafından adları sağlanmayan kişi alanlarını göstermez', async () => {
    await seedAuthenticatedUser('CALISAN')
    renderApp('/kayitlar/rec-001')

    expect(await screen.findByRole('heading', { name: 'Sunucu Donanım Alım Talebi' })).toBeInTheDocument()
    expect(screen.queryByText('Oluşturan')).not.toBeInTheDocument()
    expect(screen.queryByText('Atanan')).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /Ek Dosyalar/ })).toBeInTheDocument()
  })

  it('Admin kullanıcısını yönetim ekranına alır', async () => {
    await seedAuthenticatedUser('ADMIN')
    renderApp('/admin')
    expect(await screen.findByRole('heading', { name: 'Yönetim Özeti' })).toBeInTheDocument()
    expect(screen.getByText('Yetkili hesap')).toBeInTheDocument()
    expect(screen.queryByText('Pasif hesap')).not.toBeInTheDocument()
  })

  it('rol önizlemesinden Admin seçildiğinde API isteklerine Admin yetkisini taşır', async () => {
    const user = userEvent.setup()
    await seedAuthenticatedUser('CALISAN')
    renderApp('/dashboard')

    await user.click(await screen.findByRole('button', { name: 'Admin' }))
    expect(await screen.findByRole('heading', { name: 'Yönetim Özeti' })).toBeInTheDocument()

    await expect(api.admin.createUser({
      firstName: 'John',
      lastName: 'Doe',
      email: 'john.doe@kurum.gov.tr',
      password: 'guvenli123',
    })).rejects.toMatchObject({ status: 409 })
  })

  it('Admin olmayan kullanıcının yönetim ekranını açmasını engeller', async () => {
    await seedAuthenticatedUser('CALISAN')
    renderApp('/admin/kullanicilar')
    expect(await screen.findByRole('heading', { name: 'Bu sayfayı görüntüleme yetkiniz yok' })).toBeInTheDocument()
  })

  it('başka çalışanın taslağına düzenleme URL’siyle girildiğinde erkenden 403 gösterir', async () => {
    await seedAuthenticatedUser('CALISAN', {
      id: 'user-other-employee',
      firstName: 'Elif',
      lastName: 'Akın',
      email: 'elif.akin@kurum.gov.tr',
      role: 'CALISAN',
      mustChangePassword: false,
    })
    renderApp('/kayitlar/rec-006/duzenle')
    expect(await screen.findByRole('heading', { name: 'Bu sayfayı görüntüleme yetkiniz yok' })).toBeInTheDocument()
  })
})
