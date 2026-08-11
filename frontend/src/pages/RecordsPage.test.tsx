import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { MemoryRouter, useLocation } from 'react-router'
import { describe, expect, it } from 'vitest'
import { CategoryProvider } from '../context/CategoryContext'
import { WorkflowProvider } from '../context/WorkflowContext'
import { getDemoUserByRole } from '../mocks/users'
import { seedAuthenticatedUser } from '../test/auth'
import { apiBaseUrl } from '../api/config'
import { apiMockServer } from '../mocks/api/server'
import { RecordsPage } from './RecordsPage'

function LocationProbe() {
  const location = useLocation()
  return <output data-testid="location-search">{location.search}</output>
}

async function renderEmployeeRecords(initialEntry: string) {
  await seedAuthenticatedUser('CALISAN')
  const employee = getDemoUserByRole('CALISAN')
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <CategoryProvider>
        <WorkflowProvider user={employee}>
          <RecordsPage role={employee.role} />
          <LocationProbe />
        </WorkflowProvider>
      </CategoryProvider>
    </MemoryRouter>,
  )
}

describe('RecordsPage filters', () => {
  it('tek durumlu görünümde çelişen durum parametresini URL’den temizler', async () => {
    await renderEmployeeRecords('/kayitlar?gorunum=taslaklar&durum=REDDEDILDI')

    expect(screen.getByRole('combobox', { name: 'Durum' })).toBeDisabled()
    expect(screen.getByRole('combobox', { name: 'Durum' })).toHaveValue('TASLAK')
    await waitFor(() => expect(screen.getByTestId('location-search')).toHaveTextContent('?gorunum=taslaklar'))
    expect(screen.getByTestId('location-search')).not.toHaveTextContent('durum=')
  })

  it('kullanıcı filtresini URL sorgusuna yazar', async () => {
    const user = userEvent.setup()
    await renderEmployeeRecords('/kayitlar')

    const categorySelect = screen.getByRole('combobox', { name: 'Kategori' })
    await waitFor(() => expect(categorySelect).toBeEnabled())
    await user.selectOptions(categorySelect, 'Bilgi İşlem')
    expect(screen.getByTestId('location-search')).toHaveTextContent('kategori=Bilgi')
  })

  it('aramayı debounce sonrasında URL sorgusuna yazar', async () => {
    const user = userEvent.setup()
    await renderEmployeeRecords('/kayitlar')

    await user.type(screen.getByRole('searchbox'), 'sunucu')
    expect(screen.getByTestId('location-search')).not.toHaveTextContent('q=')
    await waitFor(() => expect(screen.getByTestId('location-search')).toHaveTextContent('q=sunucu'), { timeout: 1000 })
  })

  it('geçersiz filtreleri temizler ve taşan sayfayı son geçerli sayfaya çeker', async () => {
    await renderEmployeeRecords('/kayitlar?gorunum=bilinmeyen&kategori=Yok&durum=BOZUK&baslangic=2026-99-99&bitis=x&sayfa=999&boyut=7')

    await waitFor(() => expect(screen.getByTestId('location-search')).toHaveTextContent('?sayfa=2'))
    expect(screen.getByTestId('location-search')).not.toHaveTextContent('gorunum=')
    expect(screen.getByTestId('location-search')).not.toHaveTextContent('kategori=')
    expect(screen.getByTestId('location-search')).not.toHaveTextContent('durum=')
  })

  it('başlangıç tarihi bitiş tarihinden sonraysa açık doğrulama hatası gösterir', async () => {
    await renderEmployeeRecords('/kayitlar?baslangic=2026-08-05&bitis=2026-08-01')

    expect(screen.getByRole('alert')).toHaveTextContent('Başlangıç tarihi bitiş tarihinden sonra olamaz')
  })

  it('kategori isteği başarısız olursa tekrar deneyerek listeyi yükler', async () => {
    const user = userEvent.setup()
    let requestCount = 0
    apiMockServer.use(
      http.get(`${apiBaseUrl}/api/v1/categories`, () => {
        requestCount += 1
        return HttpResponse.json({ message: 'Geçici hata' }, { status: 500 })
      }),
    )

    await renderEmployeeRecords('/kayitlar')
    await user.click(screen.getByRole('button', { name: 'Filtreler' }))
    await waitFor(() => expect(requestCount).toBeGreaterThan(0))

    expect(await screen.findByRole('alert')).toHaveTextContent('Kategoriler yüklenemedi')
    apiMockServer.resetHandlers()
    await user.click(screen.getByRole('button', { name: 'Tekrar dene' }))

    const categorySelect = screen.getByRole('combobox', { name: 'Kategori' })
    await waitFor(() => expect(categorySelect).toBeEnabled())
    expect(screen.getByRole('option', { name: 'Mali' })).toBeInTheDocument()
  })
})
