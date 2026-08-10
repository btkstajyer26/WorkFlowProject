import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from './App'
import { getDemoUserByRole } from './mocks/users'

const mockSessionKey = 'ebys:mock-session:v1'

function renderApp(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}

describe('App authorization boundaries', () => {
  it('oturumsuz kullanıcıyı giriş ekranına yönlendirir', async () => {
    renderApp('/dashboard')
    expect(await screen.findByRole('heading', { name: 'Hesabınıza giriş yapın' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Demo hesapları' })).not.toBeInTheDocument()
  })

  it('Başkan Yardımcısına yeni kayıt oluşturma kontrolünü göstermez', async () => {
    window.sessionStorage.setItem(mockSessionKey, JSON.stringify(getDemoUserByRole('BASKAN_YARDIMCISI')))
    renderApp('/dashboard')
    expect(await screen.findByRole('heading', { name: /Hoş geldiniz/ })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Yeni Kayıt' })).not.toBeInTheDocument()
  })

  it('Başkanın kendi kapsamı dışındaki kaydını 403 ile sınırlar', async () => {
    window.sessionStorage.setItem(mockSessionKey, JSON.stringify(getDemoUserByRole('BASKAN')))
    renderApp('/kayitlar/rec-001')
    expect(await screen.findByRole('heading', { name: 'Bu sayfayı görüntüleme yetkiniz yok' })).toBeInTheDocument()
  })

  it('olmayan kayıt için 404 ekranını gösterir', async () => {
    window.sessionStorage.setItem(mockSessionKey, JSON.stringify(getDemoUserByRole('CALISAN')))
    renderApp('/kayitlar/olmayan-kayit')
    expect(await screen.findByRole('heading', { name: 'Aradığınız sayfa bulunamadı' })).toBeInTheDocument()
  })

  it('Admin kullanıcısını yönetim ekranına alır', async () => {
    window.sessionStorage.setItem(mockSessionKey, JSON.stringify(getDemoUserByRole('ADMIN')))
    renderApp('/admin')
    expect(await screen.findByRole('heading', { name: 'Yönetim Özeti' })).toBeInTheDocument()
    expect(screen.getByText('Onay bekleyen')).toBeInTheDocument()
    expect(screen.queryByText('Pasif hesap')).not.toBeInTheDocument()
  })

  it('Admin olmayan kullanıcının yönetim ekranını açmasını engeller', async () => {
    window.sessionStorage.setItem(mockSessionKey, JSON.stringify(getDemoUserByRole('CALISAN')))
    renderApp('/admin/kullanicilar')
    expect(await screen.findByRole('heading', { name: 'Bu sayfayı görüntüleme yetkiniz yok' })).toBeInTheDocument()
  })

  it('başka çalışanın taslağına düzenleme URL’siyle girildiğinde erkenden 403 gösterir', async () => {
    window.sessionStorage.setItem(mockSessionKey, JSON.stringify({
      id: 'user-other-employee',
      firstName: 'Elif',
      lastName: 'Akın',
      email: 'elif.akin@kurum.gov.tr',
      role: 'CALISAN',
    }))
    renderApp('/kayitlar/rec-006/duzenle')
    expect(await screen.findByRole('heading', { name: 'Bu sayfayı görüntüleme yetkiniz yok' })).toBeInTheDocument()
  })
})
