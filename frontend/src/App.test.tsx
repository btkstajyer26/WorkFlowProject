import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from './App'
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
    })
    renderApp('/kayitlar/rec-006/duzenle')
    expect(await screen.findByRole('heading', { name: 'Bu sayfayı görüntüleme yetkiniz yok' })).toBeInTheDocument()
  })
})
