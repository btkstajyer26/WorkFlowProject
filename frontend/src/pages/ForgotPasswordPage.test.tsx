import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import { apiBaseUrl } from '../api/config'
import { apiMockServer } from '../mocks/api/server'
import { ForgotPasswordPage } from './ForgotPasswordPage'

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/sifre-sifirla']}>
      <ForgotPasswordPage user={null} />
    </MemoryRouter>,
  )
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

    await user.type(screen.getByLabelText('E-posta adresi'), '  JOHN.DOE@kurum.gov.tr ')
    await user.click(screen.getByRole('button', { name: 'Sıfırlama bağlantısı gönder' }))

    expect(await screen.findByRole('heading', { name: 'E-postanızı kontrol edin' })).toBeInTheDocument()
    expect(requestedEmail).toBe('john.doe@kurum.gov.tr')
  })

  it('sunucu hatasında kullanıcıya tekrar deneyebileceğini söyler', async () => {
    const user = userEvent.setup()
    apiMockServer.use(
      http.post(`${apiBaseUrl}/api/auth/forgot-password`, () => HttpResponse.json({}, { status: 500 })),
    )
    renderPage()

    await user.type(screen.getByLabelText('E-posta adresi'), 'john.doe@kurum.gov.tr')
    await user.click(screen.getByRole('button', { name: 'Sıfırlama bağlantısı gönder' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Bağlantı gönderilemedi')
  })
})
