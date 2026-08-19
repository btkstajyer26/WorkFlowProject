import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { describe, expect, it } from 'vitest'
import { apiBaseUrl } from '../api/config'
import { apiMockServer } from '../mocks/api/server'
import { ForgotPasswordPage } from './ForgotPasswordPage'

function LocationProbe() {
  const location = useLocation()
  return <span aria-label="Geçerli adres">{`${location.pathname}${location.search}`}</span>
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/sifre-sifirla']}>
      <Routes>
        <Route path="/sifre-sifirla" element={<ForgotPasswordPage user={null} />} />
        <Route path="/sifre-degistir" element={<LocationProbe />} />
      </Routes>
    </MemoryRouter>,
  )
}

async function submitEmail(user: ReturnType<typeof userEvent.setup>, email = 'john.doe@kurum.gov.tr') {
  await user.type(screen.getByLabelText('E-posta adresi'), email)
  await user.click(screen.getByRole('button', { name: 'Doğrulama kodu gönder' }))
  await screen.findByRole('heading', { name: 'E-postanızı kontrol edin' })
}

describe('ForgotPasswordPage', () => {
  it('e-posta adresini sıfırlama talebi endpointine gönderir', async () => {
    const user = userEvent.setup()
    let requestedEmail: string | undefined
    apiMockServer.use(
      http.post(`${apiBaseUrl}/api/auth/forgot-password`, async ({ request }) => {
        requestedEmail = (await request.json() as { email: string }).email
        return new HttpResponse(null, { status: 202 })
      }),
    )
    renderPage()

    await submitEmail(user, '  JOHN.DOE@kurum.gov.tr ')

    expect(requestedEmail).toBe('john.doe@kurum.gov.tr')
    expect(screen.getByLabelText('Doğrulama kodu')).toBeInTheDocument()
  })

  it('sunucu hatasında kullanıcıya tekrar deneyebileceğini söyler', async () => {
    const user = userEvent.setup()
    apiMockServer.use(
      http.post(`${apiBaseUrl}/api/auth/forgot-password`, () => HttpResponse.json({}, { status: 500 })),
    )
    renderPage()

    await user.type(screen.getByLabelText('E-posta adresi'), 'john.doe@kurum.gov.tr')
    await user.click(screen.getByRole('button', { name: 'Doğrulama kodu gönder' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Doğrulama kodu gönderilemedi')
  })

  it('6 haneli olmayan kodu sunucuya göndermeden reddeder', async () => {
    const user = userEvent.setup()
    let verifyCalled = false
    apiMockServer.use(
      http.post(`${apiBaseUrl}/api/auth/verify-reset-code`, () => {
        verifyCalled = true
        return HttpResponse.json({ resetToken: 'anahtar', expiresInSeconds: 900 })
      }),
    )
    renderPage()
    await submitEmail(user)

    await user.type(screen.getByLabelText('Doğrulama kodu'), '123')
    await user.click(screen.getByRole('button', { name: 'Kodu doğrula' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Doğrulama kodu 6 rakamdan oluşur.')
    expect(verifyCalled).toBe(false)
  })

  it('doğrulanan kodun anahtarıyla şifre değiştirme ekranına yönlendirir', async () => {
    const user = userEvent.setup()
    let verifiedBody: { email: string; code: string } | undefined
    apiMockServer.use(
      http.post(`${apiBaseUrl}/api/auth/verify-reset-code`, async ({ request }) => {
        verifiedBody = await request.json() as { email: string; code: string }
        return HttpResponse.json({ resetToken: 'tek-kullanimlik anahtar', expiresInSeconds: 900 })
      }),
    )
    renderPage()
    await submitEmail(user)

    await user.type(screen.getByLabelText('Doğrulama kodu'), '135790')
    await user.click(screen.getByRole('button', { name: 'Kodu doğrula' }))

    expect(await screen.findByLabelText('Geçerli adres')).toHaveTextContent(
      '/sifre-degistir?token=tek-kullanimlik%20anahtar',
    )
    expect(verifiedBody).toEqual({ email: 'john.doe@kurum.gov.tr', code: '135790' })
  })

  it('yanlış kod için yeni kod istenebileceğini söyler', async () => {
    const user = userEvent.setup()
    apiMockServer.use(
      http.post(`${apiBaseUrl}/api/auth/verify-reset-code`, () => HttpResponse.json({
        code: 'INVALID_OR_EXPIRED_RESET_CODE',
        message: 'Doğrulama kodu geçersiz veya süresi dolmuş',
        status: 400,
        timestamp: '2026-08-19T09:00:00Z',
      }, { status: 400 })),
    )
    renderPage()
    await submitEmail(user)

    await user.type(screen.getByLabelText('Doğrulama kodu'), '000000')
    await user.click(screen.getByRole('button', { name: 'Kodu doğrula' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Kod geçersiz veya süresi dolmuş')
  })

  it('kodu tekrar göndermek aynı adres için yeni talep açar', async () => {
    const user = userEvent.setup()
    let requestCount = 0
    apiMockServer.use(
      http.post(`${apiBaseUrl}/api/auth/forgot-password`, () => {
        requestCount += 1
        return new HttpResponse(null, { status: 202 })
      }),
    )
    renderPage()
    await submitEmail(user)

    await user.click(screen.getByRole('button', { name: 'Kodu tekrar gönder' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Yeni kod gönderildi')
    expect(requestCount).toBe(2)
  })
})
