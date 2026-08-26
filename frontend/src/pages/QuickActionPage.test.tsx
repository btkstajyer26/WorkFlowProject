import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiBaseUrl } from '../api/config'
import { apiMockServer } from '../mocks/api/server'
import { QuickActionPage } from './QuickActionPage'

const RECORD_ID = '11111111-1111-1111-1111-111111111111'

function preview(overrides: Record<string, unknown> = {}) {
  return {
    recordId: RECORD_ID,
    recordTitle: 'Bütçe teklifi',
    recordStatus: 'BASKAN_INCELEMESINDE',
    action: 'ONAYLA',
    recipientName: 'Ayşe Kaya',
    expiresAt: '2026-08-30T10:00:00',
    ...overrides,
  }
}

function setHash(hash: string) {
  window.history.replaceState(null, '', `/hizli-islem${hash}`)
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/hizli-islem']}>
      <QuickActionPage />
    </MemoryRouter>,
  )
}

describe('QuickActionPage', () => {
  beforeEach(() => {
    setHash('#token=ham-anahtar')
  })

  afterEach(() => {
    window.history.replaceState(null, '', '/')
  })

  it('açılışta hiçbir işlem yapmaz, yalnız önizleme çağırır', async () => {
    const consumeCagrildi = vi.fn()
    apiMockServer.use(
      http.post(`${apiBaseUrl}/api/public/mail-actions/preview`, () => HttpResponse.json(preview())),
      http.post(`${apiBaseUrl}/api/public/mail-actions/consume`, () => {
        consumeCagrildi()
        return HttpResponse.json({ recordId: RECORD_ID })
      }),
    )

    renderPage()

    // Onay ekranı çizilene kadar bekle; bu noktaya gelindiğinde açılış akışı bitmiştir.
    expect(await screen.findByRole('heading', { name: 'İşlemi onaylayın' })).toBeInTheDocument()
    expect(consumeCagrildi).not.toHaveBeenCalled()
  })

  it('anahtarı adres çubuğundan siler', async () => {
    apiMockServer.use(
      http.post(`${apiBaseUrl}/api/public/mail-actions/preview`, () => HttpResponse.json(preview())),
    )

    renderPage()

    await screen.findByRole('heading', { name: 'İşlemi onaylayın' })
    expect(window.location.hash).toBe('')
    expect(window.location.href).not.toContain('ham-anahtar')
  })

  it('onaylandığında anahtarı gövdede gönderir ve sonucu gösterir', async () => {
    const user = userEvent.setup()
    let gonderilenGovde: unknown = null

    apiMockServer.use(
      http.post(`${apiBaseUrl}/api/public/mail-actions/preview`, () => HttpResponse.json(preview())),
      http.post(`${apiBaseUrl}/api/public/mail-actions/consume`, async ({ request }) => {
        gonderilenGovde = await request.json()
        return HttpResponse.json({ recordId: RECORD_ID })
      }),
    )

    renderPage()
    await user.click(await screen.findByRole('button', { name: 'Onayla' }))

    expect(await screen.findByRole('heading', { name: 'İşlem tamamlandı' })).toBeInTheDocument()
    expect(gonderilenGovde).toEqual({ token: 'ham-anahtar' })
  })

  it('kullanılmış bağlantıda backend mesajını gösterir', async () => {
    const user = userEvent.setup()
    apiMockServer.use(
      http.post(`${apiBaseUrl}/api/public/mail-actions/preview`, () => HttpResponse.json(preview())),
      http.post(`${apiBaseUrl}/api/public/mail-actions/consume`, () =>
        HttpResponse.json(
          {
            code: 'INVALID_OR_EXPIRED_MAIL_ACTION_TOKEN',
            message: 'Bağlantı geçersiz, süresi dolmuş veya daha önce kullanılmış',
            status: 400,
            timestamp: '2026-08-24T10:00:00',
          },
          { status: 400 },
        ),
      ),
    )

    renderPage()
    await user.click(await screen.findByRole('button', { name: 'Onayla' }))

    expect(await screen.findByRole('heading', { name: 'İşlem yapılamadı' })).toBeInTheDocument()
    expect(
      screen.getByText('Bağlantı geçersiz, süresi dolmuş veya daha önce kullanılmış'),
    ).toBeInTheDocument()
  })

  it('evrak arada ilerlediyse durum makinesinin mesajını gösterir', async () => {
    const user = userEvent.setup()
    apiMockServer.use(
      http.post(`${apiBaseUrl}/api/public/mail-actions/preview`, () => HttpResponse.json(preview())),
      http.post(`${apiBaseUrl}/api/public/mail-actions/consume`, () =>
        HttpResponse.json(
          {
            code: 'WORKFLOW_INVALID_TRANSITION',
            message: 'Bu evrak için geçerli bir işlem değil',
            status: 400,
            timestamp: '2026-08-24T10:00:00',
          },
          { status: 400 },
        ),
      ),
    )

    renderPage()
    await user.click(await screen.findByRole('button', { name: 'Onayla' }))

    expect(await screen.findByRole('heading', { name: 'İşlem yapılamadı' })).toBeInTheDocument()
    expect(screen.getByText('Bu evrak için geçerli bir işlem değil')).toBeInTheDocument()
  })

  it('anahtarsız açıldığında backend’e hiç istek gitmez', async () => {
    setHash('')
    const previewCagrildi = vi.fn()
    apiMockServer.use(
      http.post(`${apiBaseUrl}/api/public/mail-actions/preview`, () => {
        previewCagrildi()
        return HttpResponse.json(preview())
      }),
    )

    renderPage()

    expect(await screen.findByRole('heading', { name: 'İşlem yapılamadı' })).toBeInTheDocument()
    await waitFor(() => expect(previewCagrildi).not.toHaveBeenCalled())
  })
})
